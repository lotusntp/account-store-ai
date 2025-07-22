-- Database schema for Account Selling Platform

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS account_selling;

-- Set search path
SET search_path TO account_selling, public;

-- Create sequences
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START 1;

-- Note: The actual tables will be created by Hibernate based on entity classes
-- This file is for database initialization and extensions only