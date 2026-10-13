# ReFind

ReFind is a Java Swing lost-and-found application that stores data in MySQL and keeps item images in a local filesystem directory.

## Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 or compatible database server
- An existing schema prepared by `database/schema.sql` or an equivalent database you manage yourself

## Database configuration

1. Copy `src/main/resources/database.properties.example` to `src/main/resources/database.properties`.
2. Set the `db.url`, `db.username`, and `db.password` values for your database.
3. Keep credentials out of version control; do not commit real secrets.

The application supports these configuration sources:

- `src/main/resources/database.properties`
- `REFIND_DB_URL`, `REFIND_DB_USERNAME`, `REFIND_DB_PASSWORD`
- `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, and related standard fallbacks

The default runtime configuration is not a testing database. During normal startup, the app validates the required schema and refuses to auto-create a schema unless you explicitly opt in.

## Explicit development-only opt-ins

These features are intentionally disabled unless set explicitly:

```bash
REFIND_AUTO_INITIALIZE_SCHEMA=true
REFIND_SEED_DEMO_DATA=true
```

You can also pass the equivalent JVM properties:

```bash
mvn exec:java -Dexec.mainClass=com.refind.app.Application -Drefind.autoInitializeSchema=true -Drefind.seedDemoData=true
```

This is for local development only. The application does not automatically create schema objects or seed sample users/items during a normal startup.

## Launch the application

```bash
mvn exec:java -Dexec.mainClass=com.refind.app.Application
```

## Run unit tests

```bash
mvn clean test
```

## Compile without tests

```bash
mvn -DskipTests compile
```

## Integration tests against a dedicated test database

Integration tests are not part of the default `mvn test` path and require a dedicated database with a test-name pattern such as `test`, `test_app`, or `app_test`.

Set the dedicated test database variables before running the integration suite:

```bash
export REFIND_TEST_DB_URL="jdbc:mysql://localhost:3306/test_refind?sslMode=REQUIRED"
export REFIND_TEST_DB_USERNAME="refind_test_user"
export REFIND_TEST_DB_PASSWORD="YOUR_TEST_PASSWORD"
```

Then enable the dedicated test check:

```bash
mvn test -Drefind.integration=true
```

The integration database guard refuses to fall back to the application database configuration and requires the `REFIND_TEST_DB_*` settings for a dedicated test database.

## Image storage

Images are stored locally on disk rather than in the main MySQL schema. The default storage location is:

```text
$HOME/.refind/uploads/items
```

You can override it with:

```bash
REFIND_IMAGE_DIR=/path/to/shared/images
```

or

```bash
-Drefind.image.dir=/path/to/shared/images
```

Images are not automatically shared across machines; they are persisted on the local filesystem associated with the current installation.

## Notes and limitations

- The app does not automatically create or reset the production schema.
- Demo data seeding is opt-in and does not run during a normal startup.
- Database integration tests must be run only against a dedicated safe test database.
- The desktop app requires an interactive GUI session and is not a headless server application.
