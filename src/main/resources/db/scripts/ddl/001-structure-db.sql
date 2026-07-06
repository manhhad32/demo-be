-- Tạo bảng Type_Job
CREATE TABLE type_job (
    id SERIAL PRIMARY KEY,
    type VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tạo bảng JOB
CREATE TABLE job (
    id SERIAL PRIMARY KEY,
    type_job_id INT NOT NULL,
    payload JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Thêm foreign key constraint
ALTER TABLE job ADD CONSTRAINT fk_job_type_job FOREIGN KEY (type_job_id) REFERENCES type_job(id) ON DELETE RESTRICT;

-- Đánh index trên trường status để tối ưu truy vấn lấy các Job đang PENDING
CREATE INDEX IF NOT EXISTS idx_job_status ON job(status);

-- Đánh index trên type_job_id để tối ưu truy vấn join giữa 2 bảng
CREATE INDEX IF NOT EXISTS idx_job_type_job_id ON job(type_job_id);
