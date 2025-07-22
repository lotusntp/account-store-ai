# Database Setup Instructions

## Prerequisites

- PostgreSQL 12 or higher installed
- PostgreSQL command-line tools (psql) available

## Initial Database Setup

1. Connect to PostgreSQL as a superuser:

```bash
psql -U postgres
```

2. Run the initialization script:

```bash
\i /path/to/init-db.sql
```

Alternatively, you can run the script directly from the command line:

```bash
psql -U postgres -f /path/to/init-db.sql
```

## Database Configuration

The application is configured to connect to the PostgreSQL database with the following default settings:

- Host: localhost
- Port: 5432
- Database: accountselling
- Username: postgres
- Password: postgres

You can override these settings by:

1. Modifying the `application.yml` file
2. Setting environment variables:
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USERNAME`
   - `DB_PASSWORD`

## Database Migration

The application uses Flyway for database migrations. Migrations are automatically applied when the application starts.

Migration scripts are located in the `src/main/resources/db/migration` directory and follow the naming convention:

```
V{version}__{description}.sql
```

For example: `V1__init_schema.sql`

## Development vs. Production

- Development environment uses `update` mode for Hibernate DDL, which automatically updates the schema based on entity classes.
- Production environment uses `validate` mode, which validates that the database schema matches the entity classes but doesn't modify the schema.

For production deployment, ensure that all migrations have been properly tested in a staging environment before applying them to production.