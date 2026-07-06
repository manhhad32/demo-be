# demo-be

## Setup and Run Instructions

### Prerequisites
- Java 21 or higher
- Maven 3.8+
- PostgreSQL 15+ (or any compatible version)

### Database Setup
The application uses Flyway/Liquibase or Hibernate for database initialization. Ensure PostgreSQL is running and update `src/main/resources/application.properties` (or `application.yml`) with your local database credentials if they differ from the default.

### Running the Application
1. **Build the project:**
   ```bash
   mvn clean install
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```
   Or run the generated JAR:
   ```bash
   java -jar target/demo-0.0.1-SNAPSHOT.jar
   ```

### Running Tests
To execute the automated test suite:
```bash
mvn test
```

---

## Concurrency Strategy: Job Processing

### The Problem
When scaling the application to multiple instances (or when handling concurrent API calls to `/api/jobs/process`), we need a robust way to ensure that multiple nodes do not pick up and process the same `PENDING` job simultaneously. If they do, it would lead to duplicate processing (e.g. sending the same email twice).

### The Solution: `FOR UPDATE SKIP LOCKED`
To achieve safe concurrency, we utilized PostgreSQL's `SELECT ... FOR UPDATE SKIP LOCKED` mechanism within a native query inside our `JobRepository`:

```sql
SELECT * FROM job 
WHERE status = 'PENDING' 
  AND (next_run_at IS NULL OR next_run_at <= NOW()) 
FOR UPDATE SKIP LOCKED 
LIMIT :limit
```

**How it works:**
1. **Pessimistic Locking (`FOR UPDATE`)**: When a thread fetches a batch of `PENDING` jobs, it immediately acquires row-level locks on those specific records in the database.
2. **Non-blocking Concurrency (`SKIP LOCKED`)**: If Thread A has locked rows 1-10, and Thread B arrives concurrently, Thread B will not wait for Thread A to release its locks. Instead, Thread B will instantly skip rows 1-10 and lock the next available batch (e.g., rows 11-20). 

### Trade-offs & Advantages

**Advantages:**
- **Absolute Concurrency Safety:** Guarantees that no two threads/nodes can ever fetch and process the exact same job at the same time.
- **High Throughput (No Contention):** Because of `SKIP LOCKED`, threads do not block each other waiting for locks to release. Multiple application nodes can process different chunks of the queue fully in parallel.
- **No External Dependencies:** We achieved a robust distributed queue without needing to introduce external infrastructure like Redis, RabbitMQ, or Kafka.

**Trade-offs:**
- **Database Load:** Offloads the queue-management burden entirely onto PostgreSQL. For extremely high-throughput systems (e.g., thousands of messages per second), a dedicated message broker (Kafka/RabbitMQ) would scale better than relational DB locking.
- **Transaction Scope Management:** Requires careful management of database connections. We specifically decoupled the "Fetch & Lock" transaction (using `TransactionTemplate`, which updates status to `PROCESSING` and commits immediately) from the actual "Execution" logic. This ensures we don't hold long-running database transactions while calling slow external APIs (e.g., sending emails).

### Retry Mechanism
The system employs a resilient two-tier retry mechanism:
1. **Short-term (In-Memory)**: Utilizes Spring Retry (`@Retryable`) for transient failures, retrying up to 3 times rapidly.
2. **Long-term (Database-backed)**: If the short-term retry is exhausted, the job's failure is logged to `job_execution_log`, and the job is rescheduled (`next_run_at`) with an incremental backoff.
- To prevent infinite loops, if a job accumulates 3 failure logs, it is permanently marked as `FAILED`.

---

## Written Questions to Answer

### Question A - System Design
**Suppose this service needs to support 1 million jobs per day and multiple application instances running in parallel. How would you improve the current design for production use?**

1. **Proposed Architecture & Data Flow**: 
   - Shift from a "Database as a Queue" pattern to a dedicated **Message Broker** architecture (e.g., Apache Kafka, RabbitMQ, or AWS SQS).
   - Data Flow: The `POST /api/jobs` endpoint persists the job to PostgreSQL (as the source of truth) and immediately publishes an event to Kafka. Background worker instances consume events from Kafka rather than continuously polling the database.
2. **Duplicate Processing Prevention**:
   - Message brokers typically offer *at-least-once* delivery. To achieve *exactly-once* semantics, we must implement **Idempotency**. 
   - We would introduce a distributed cache (e.g., Redis). Before processing, the worker attempts to acquire a Redis lock using the `jobId` or an `idempotency_key`. If the lock is acquired, it processes the job. If the lock exists, the message is a duplicate and can be safely acked and discarded.
3. **Failure Handling**:
   - Utilize a **Dead Letter Queue (DLQ)**. If a job fails multiple retries, the message is routed to the DLQ instead of lingering in the active queue or constantly updating the database. A separate operational process handles DLQ triage.
4. **Scaling Approach**:
   - Separate the API Web Servers from the Background Workers. Scale the worker pods dynamically based on the queue depth (e.g., using KEDA - Kubernetes Event-driven Autoscaling).
5. **Operational Considerations**:
   - Implement Distributed Tracing (e.g., OpenTelemetry, Jaeger) to track a job's lifecycle across API and worker instances. Implement robust alerting (Prometheus/Grafana) for queue lag (when jobs are being published faster than processed).

### Question B - Database Performance
**The jobs table has 50 million records. GET /api/jobs?status=PENDING&page=0&size=20 becomes slow. How would you investigate the issue and improve the performance?**

1. **Investigation Steps**:
   - Run `EXPLAIN ANALYZE SELECT * FROM job WHERE status = 'PENDING' OFFSET 0 LIMIT 20;` directly in the database.
   - Look for `Seq Scan` (Sequential Scan) in the execution plan, which indicates the DB is scanning the entire table.
   - Check if an index exists on the `status` column.
2. **Likely Bottlenecks**:
   - **Missing Index**: Scanning 50M rows without an index will cripple performance.
   - **Deep Pagination (OFFSET)**: Even with an index, if a user navigates to page 100,000 (`OFFSET 2000000`), the database still has to fetch and discard 2 million rows before returning the 20 records.
3. **Possible Database/Query Changes**:
   - **Add a Partial Index**: Since we usually only query active states like `PENDING` or `PROCESSING` (while 99% of historical jobs are `COMPLETED`), a regular B-Tree index might not be selective enough. A Partial Index is highly efficient: 
     `CREATE INDEX idx_job_pending ON job (status) WHERE status = 'PENDING';`
   - **Adopt Keyset Pagination (Cursor-based)**: Replace `OFFSET` with a cursor. The query becomes:
     `SELECT * FROM job WHERE status = 'PENDING' AND id > :lastId ORDER BY id ASC LIMIT 20;`
4. **Trade-offs**:
   - *Indexes*: Adding indexes speeds up `SELECT` but slightly slows down `INSERT`/`UPDATE` operations and consumes disk space. A Partial Index is the perfect trade-off here, as it remains extremely small and fast.
   - *Keyset Pagination*: It provides stable, lightning-fast queries regardless of depth, but the trade-off is the loss of "Jump to Page X" functionality in the UI (users can only click "Next" or "Previous").
