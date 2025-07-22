-- สร้าง database
CREATE DATABASE accountselling;

-- สร้าง user
DO
$$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'accountselling_user') THEN
    CREATE USER accountselling_user WITH ENCRYPTED PASSWORD 'accountselling_password';
  END IF;
END
$$;

-- ให้สิทธิ์กับ user
GRANT ALL PRIVILEGES ON DATABASE accountselling TO accountselling_user;
