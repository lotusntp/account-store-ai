-- ตรวจสอบโครงสร้าง database ปัจจุบัน

-- ตรวจสอบว่าตาราง stock มีอยู่หรือไม่
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name = 'stock';

-- ตรวจสอบคอลัมน์ในตาราง stock (ถ้ามี)
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns 
WHERE table_schema = 'public' 
AND table_name = 'stock'
ORDER BY ordinal_position;

-- ตรวจสอบว่าตาราง flyway_schema_history มีอยู่หรือไม่
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name = 'flyway_schema_history';