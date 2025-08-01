-- สร้างตาราง flyway_schema_history และทำเครื่องหมายว่า migration สำเร็จแล้ว

-- สร้างตาราง flyway_schema_history
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT NOW(),
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);

-- สร้าง index สำหรับ flyway_schema_history
CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx ON flyway_schema_history (success);

-- เพิ่มข้อมูล migration ที่สำเร็จแล้ว
-- V1: Initial schema
INSERT INTO flyway_schema_history (
    installed_rank, version, description, type, script, checksum, 
    installed_by, installed_on, execution_time, success
) VALUES (
    1, '1', 'init schema', 'SQL', 'V1__init_schema.sql', 
    -1, 'manual', NOW(), 0, true
);

-- V1.8: Update stock for encrypted storage (mark as completed since structure already exists)
INSERT INTO flyway_schema_history (
    installed_rank, version, description, type, script, checksum, 
    installed_by, installed_on, execution_time, success
) VALUES (
    2, '1.8', 'Update stock for encrypted storage', 'SQL', 'V1_8__Update_stock_for_encrypted_storage.sql', 
    -1, 'manual', NOW(), 0, true
);

-- ตรวจสอบผลลัพธ์
SELECT * FROM flyway_schema_history ORDER BY installed_rank;