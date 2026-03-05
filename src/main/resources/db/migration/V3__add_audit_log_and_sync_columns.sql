-- Sync-tracking columns for bidirectional synchronisation
ALTER TABLE users ADD COLUMN updated_at TEXT DEFAULT '';
ALTER TABLE files ADD COLUMN updated_at TEXT DEFAULT '';

-- Initialise timestamps for pre-existing rows
UPDATE users SET updated_at = datetime('now') WHERE updated_at = '' OR updated_at IS NULL;
UPDATE files SET updated_at = datetime('now') WHERE updated_at = '' OR updated_at IS NULL;

-- Audit log – records every user action and system event
CREATE TABLE IF NOT EXISTS audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TEXT NOT NULL,
    username  TEXT,
    action    TEXT NOT NULL,
    target    TEXT,
    details   TEXT,
    status    TEXT NOT NULL DEFAULT 'SUCCESS'
);

CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_log_action    ON audit_log(action);

-- Sync metadata – tracks last sync time per table
CREATE TABLE IF NOT EXISTS sync_metadata (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    table_name  TEXT NOT NULL UNIQUE,
    last_sync_at TEXT NOT NULL DEFAULT '1970-01-01 00:00:00'
);

INSERT OR IGNORE INTO sync_metadata (table_name, last_sync_at) VALUES ('users', '1970-01-01 00:00:00');
INSERT OR IGNORE INTO sync_metadata (table_name, last_sync_at) VALUES ('files', '1970-01-01 00:00:00');
