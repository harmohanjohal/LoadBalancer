# Security and Configuration Notes

## Required environment variables

- `FILE_ENCRYPTION_PASSWORD`: Required for file encryption/decryption in `FileUtils` (minimum 12 characters).
- `APP_DB_PATH` (optional): SQLite DB path. If not set, defaults to `comp20081.db` in the project working directory.

## Startup validation

- App startup now validates required configuration before loading the login screen.
- If validation fails, the app shows a startup error dialog and exits.
- Validation currently checks:
	- required `FILE_ENCRYPTION_PASSWORD` presence and minimum length
	- `APP_DB_PATH` parent directory writability when provided

## Dependency vulnerability scanning

- Maven `quality` profile now runs OWASP Dependency Check and fails build on CVSS >= 7.
- CI passes optional `NVD_API_KEY` secret to reduce NVD API rate limiting.
- Run locally with: `mvn -Pquality verify`

## Database migrations (Flyway)

- App startup now runs Flyway migrations before loading the login screen.
- Migration scripts are in `src/main/resources/db/migration`.
- Initial schema is defined in `V1__initial_schema.sql`.
- Runtime no longer mutates schema ad-hoc from `DB.java`.

## What was changed

- Removed hard-coded DB credentials and endpoint usage.
- Consolidated DB calls to a single SQLite-backed connection strategy.
- Added schema alignment safeguards for missing legacy columns.
- Stopped exposing stored password hashes in user management table UI.
- Removed hard-coded file encryption password and switched to env var.
- Added file-name validation for encryption/chunking operations.

## Operational checklist

1. Set `FILE_ENCRYPTION_PASSWORD` before launching the app.
2. Optionally set `APP_DB_PATH` to a secure writable location.
3. Ensure `uploads/` and `temp/` directories are writable by the app runtime user.
4. Do not use production secrets in local shell profiles.

## Next hardening steps

- Move to per-user/per-file encryption keys and key rotation.
- Add explicit file ownership checks before edit/download/delete.
- Add migration tooling (Flyway/Liquibase) for deterministic schema upgrades.
- Add CI vulnerability scanning for Maven dependencies.
