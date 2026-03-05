CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user'
);

CREATE TABLE IF NOT EXISTS files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL UNIQUE,
    uploaded_by TEXT NOT NULL,
    upload_date TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS file_chunks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL,
    chunk_name TEXT,
    parent_file TEXT,
    chunk_index INTEGER,
    chunk_data BLOB
);

CREATE INDEX IF NOT EXISTS idx_file_chunks_parent_file ON file_chunks(parent_file);
CREATE INDEX IF NOT EXISTS idx_file_chunks_file_name ON file_chunks(file_name);
