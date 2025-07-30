-- Migration to update Stock entity for encrypted storage
-- Updates stock table to support encrypted account data and additional security fields

-- Rename credentials column to account_data and increase size for encrypted data
ALTER TABLE stock RENAME COLUMN credentials TO account_data;
ALTER TABLE stock ALTER COLUMN account_data TYPE VARCHAR(2000);

-- Add new columns for enhanced stock management
ALTER TABLE stock ADD COLUMN account_type VARCHAR(100);
ALTER TABLE stock ADD COLUMN price DECIMAL(10,2);
ALTER TABLE stock ADD COLUMN available BOOLEAN NOT NULL DEFAULT true;

-- Update existing data to mark as available
UPDATE stock SET available = true WHERE available IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN stock.account_data IS 'Encrypted account credentials and information';
COMMENT ON COLUMN stock.account_type IS 'Type/description of the account (e.g., Premium Gaming Account)';
COMMENT ON COLUMN stock.price IS 'Individual price for this stock item';
COMMENT ON COLUMN stock.available IS 'Whether this stock item is available for sale (can be disabled without selling)';

-- Create additional indexes for performance
CREATE INDEX IF NOT EXISTS idx_stock_available ON stock(available);
CREATE INDEX IF NOT EXISTS idx_stock_account_type ON stock(account_type);
CREATE INDEX IF NOT EXISTS idx_stock_price ON stock(price);

-- Update existing indexes if needed
DROP INDEX IF EXISTS idx_stock_product_available;
CREATE INDEX idx_stock_product_available ON stock(product_id, available, sold);