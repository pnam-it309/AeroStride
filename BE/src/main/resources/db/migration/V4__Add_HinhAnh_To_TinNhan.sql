-- Migration V4: Add column hinh_anh to tin_nhan table for persistent chat images
ALTER TABLE tin_nhan ADD COLUMN IF NOT EXISTS hinh_anh LONGTEXT;
