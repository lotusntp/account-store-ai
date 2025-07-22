-- Database initialization script
-- This script should be run manually before starting the application for the first time

-- Create database if it doesn't exist
-- PostgreSQL syntax for creating database if it doesn't exist
DO
$$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'accountselling') THEN
    CREATE DATABASE accountselling;
  END IF;
END
$$;

-- Create user if it doesn't exist
DO
$$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'accountselling_user') THEN
    CREATE USER accountselling_user WITH ENCRYPTED PASSWORD 'accountselling_password';
  END IF;
END
$$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE accountselling TO accountselling_user;

-- Connect to the database
\c accountselling

-- Create schema
CREATE SCHEMA IF NOT EXISTS account_selling;

-- Grant privileges on schema
GRANT ALL PRIVILEGES ON SCHEMA account_selling TO accountselling_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA account_selling
GRANT ALL PRIVILEGES ON TABLES TO accountselling_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA account_selling
GRANT ALL PRIVILEGES ON SEQUENCES TO accountselling_user;