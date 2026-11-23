# Web database migrations

These SQL scripts are for the separate VAARL web database. They do not touch the desktop application's local serialized data.

## Apply migration 001

1. Create a dedicated PostgreSQL database for the web backend (default connection name in backend configuration is `vaarl_db`).
2. Connect to that database using a PostgreSQL administrator or database owner account.
3. Run `001_create_web_accounts.sql`.
4. Configure the backend environment variables (`VAARL_DB_URL`, `VAARL_DB_USER`, `VAARL_DB_PASSWORD`) before starting it.

Do not expose database credentials or use an administrator account as the web application's database login. Public registration must only allow `VEHICLE_OWNER` and `REPAIR_SHOP`; `ADMIN` accounts must be provisioned by a trusted administrator.
