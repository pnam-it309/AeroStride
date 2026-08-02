-- Migration to convert all existing pending seed invoices (trang_thai = 0) to completed status (trang_thai = 4)
-- This ensures no lingering sample/seed invoices remain in pending status at POS
UPDATE hoa_don
SET trang_thai = 4
WHERE trang_thai = 0;
