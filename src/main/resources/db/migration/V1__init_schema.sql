-- V1: Initial database schema

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS account_selling;

-- Set search path
SET search_path TO account_selling, public;

-- Create sequences
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START 1;

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Insert default roles
INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER') ON CONFLICT DO NOTHING;
INSERT INTO roles (id, name) VALUES (2, 'ROLE_ADMIN') ON CONFLICT DO NOTHING;

-- Note: Other tables will be created by Hibernate based on entity classes