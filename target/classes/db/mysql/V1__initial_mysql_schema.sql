-- Remote MySQL schema – mirrors the local SQLite structure with MySQL types.

CREATE TABLE IF NOT EXISTS users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    password   TEXT         NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'user',
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (role IN ('user', 'admin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS files (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    file_name   VARCHAR(255) NOT NULL UNIQUE,
    uploaded_by VARCHAR(255) NOT NULL,
    upload_date VARCHAR(50)  NOT NULL,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (uploaded_by) REFERENCES users(username) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS file_chunks (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    file_name   VARCHAR(255) NOT NULL,
    chunk_name  VARCHAR(255),
    parent_file VARCHAR(255),
    chunk_index INT,
    chunk_data  LONGBLOB,
    INDEX idx_fc_parent   (parent_file),
    INDEX idx_fc_filename (file_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username  VARCHAR(255),
    action    VARCHAR(100) NOT NULL,
    target    VARCHAR(255),
    details   TEXT,
    status    VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    INDEX idx_al_timestamp (timestamp),
    INDEX idx_al_action    (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sync_metadata (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    table_name   VARCHAR(100) NOT NULL UNIQUE,
    last_sync_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
