-- Tach kenh ban khoi phuong thuc giao/nhan hang.
-- Cac lenh dong duoc dung de migration van chay duoc tren DB ma Hibernate
-- da tung tu tao cot order_type.

SET @add_order_type = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'hoa_don'
       AND column_name = 'order_type') = 0,
    'ALTER TABLE hoa_don ADD COLUMN order_type VARCHAR(20) NULL AFTER loai_don',
    'SELECT 1'
);
PREPARE add_order_type_stmt FROM @add_order_type;
EXECUTE add_order_type_stmt;
DEALLOCATE PREPARE add_order_type_stmt;

SET @add_delivery_method = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'hoa_don'
       AND column_name = 'delivery_method') = 0,
    'ALTER TABLE hoa_don ADD COLUMN delivery_method VARCHAR(20) NULL AFTER order_type',
    'SELECT 1'
);
PREPARE add_delivery_method_stmt FROM @add_delivery_method;
EXECUTE add_delivery_method_stmt;
DEALLOCATE PREPARE add_delivery_method_stmt;

-- Don co nhan vien la don tao tu POS, ke ca khi loai_don cu la ONLINE/GIAO_HANG.
UPDATE hoa_don
SET order_type = CASE
    WHEN id_nhan_vien IS NOT NULL THEN 'IN_STORE'
    WHEN UPPER(COALESCE(loai_don, '')) = 'ONLINE' THEN 'ONLINE'
    ELSE 'IN_STORE'
END
WHERE order_type IS NULL OR order_type = '';

UPDATE hoa_don
SET delivery_method = CASE
    WHEN UPPER(COALESCE(loai_don, '')) IN ('ONLINE', 'GIAO_HANG') THEN 'SHIPPING'
    ELSE 'TAKEAWAY'
END
WHERE delivery_method IS NULL OR delivery_method = '';

