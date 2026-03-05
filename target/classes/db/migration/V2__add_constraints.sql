-- FK enforcement is enabled per-connection in DB.getConnection()

-- Recreate users table with CHECK constraint on role
CREATE TABLE IF NOT EXISTS users_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin'))
);

INSERT INTO users_new (id, username, password, role)
    SELECT id, username, password, role FROM users;

DROP TABLE users;
ALTER TABLE users_new RENAME TO users;

-- Recreate files table with FK on uploaded_by
CREATE TABLE IF NOT EXISTS files_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL UNIQUE,
    uploaded_by TEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    upload_date TEXT NOT NULL
);

INSERT INTO files_new (id, file_name, uploaded_by, upload_date)
    SELECT id, file_name, uploaded_by, upload_date FROM files;

DROP TABLE files;
ALTER TABLE files_new RENAME TO files;
