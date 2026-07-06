# demo-be

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
