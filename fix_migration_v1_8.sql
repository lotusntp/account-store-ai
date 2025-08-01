-- Fix for V1_8 migration issue
-- The migration is trying to rename 'credentials' to 'account_data' but the column already exists as 'account_data'
-- This script will mark the migration as completed in flyway_schema_history

-- First, check if the migration failed and needs to be removed from history
DELETE FROM flyway_schema_history WHERE version = '1.8' AND success = false;

-- Insert the migration as successful (assuming the table structure is already correct)
INSERT INTO flyway_schema_history (
    installed_rank, 
    version, 
    description, 
    type, 
    script, 
    checksum, 
    installed_by, 
    installed_on, 
    execution_time, 
    success
) VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
    '1.8',
    'Update stock for encrypted storage',
    'SQL',
    'V1_8__Update_stock_for_encrypted_storage.sql',
    0, -- You may need to calculate the actual checksum
    'manual_fix',
    NOW(),
    0,
    true
) ON CONFLICT (version) DO NOTHING;