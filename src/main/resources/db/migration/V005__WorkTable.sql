
CREATE TABLE work_queue (
    id SERIAL PRIMARY KEY,
    case_id VARCHAR(255),
    json_data JSON,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_datetime TIMESTAMP NULL
);