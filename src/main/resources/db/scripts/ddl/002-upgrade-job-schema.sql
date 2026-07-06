ALTER TABLE job ADD COLUMN next_run_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE job_execution_log (
    id SERIAL PRIMARY KEY,
    job_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_execution_log_job FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_job_execution_log_job_id ON job_execution_log(job_id);
