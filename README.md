# ReFind

ReFind is a JDBC-based lost-and-found backend skeleton wired to the `lostfound` MySQL schema.

## Database setup

1. Install MySQL.
2. Run `database/schema.sql` in MySQL. It creates the `lostfound` database and all required tables.
3. Copy `src/main/resources/database.properties.example` to `src/main/resources/database.properties`.
4. Set your MySQL username and password.

The connection defaults to `jdbc:mysql://localhost:3306/lostfound`, and the application also supports the environment variables `REFIND_DB_URL`, `REFIND_DB_USERNAME`, and `REFIND_DB_PASSWORD`.

## Build

Requires Java 21 and Maven.

```bash
mvn clean package
```

## Run the database connectivity smoke test

```bash
mvn exec:java -Dexec.mainClass=com.refind.app.Application
```

## Architecture

The database has a single `items` table. `items.type` distinguishes `LOST` and `FOUND`, while `items.status` tracks `LOST`, `FOUND`, `CLAIMED`, `RETURNED`, or `CLOSED`.

JDBC implementations are under `com.refind.dao.impl` and services are under `com.refind.service.impl`.
