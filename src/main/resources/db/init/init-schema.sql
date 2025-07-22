-- สร้าง schema
CREATE SCHEMA IF NOT EXISTS account_selling;

-- ให้สิทธิ์กับ user
GRANT ALL PRIVILEGES ON SCHEMA account_selling TO accountselling_user;

-- ตั้ง default privileges สำหรับ table/sequence ที่จะสร้างในอนาคต
ALTER DEFAULT PRIVILEGES IN SCHEMA account_selling
GRANT ALL PRIVILEGES ON TABLES TO accountselling_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA account_selling
GRANT ALL PRIVILEGES ON SEQUENCES TO accountselling_user;
