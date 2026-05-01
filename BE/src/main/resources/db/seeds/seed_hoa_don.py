import random
from datetime import datetime, timedelta

# Settings
num_invoices = 200
start_id = 100
current_date = datetime(2026, 4, 14, 11, 0)
days_back = 30
invoices_per_day_min = 5
invoices_per_day_max = 7

# Data pools
customers = [f'kh{i}' for i in range(1, 26)]
employees = ['nv1', 'nv2']
statuses = [0, 1, 2, 3, 4] # 0: Pending, 1: Processed, 2: Shipping, 3: Completed, 4: Cancelled
types = ['ONLINE', 'OFFLINE']
addresses = [
    'Số 123 Nguyễn Huệ, Quận 1, TP.HCM',
    '88 Cầu Giấy, Hà Nội',
    '32 Lê Duẩn, Đà Nẵng',
    '15 Võ Văn Kiệt, Cần Thơ',
    '99 Lạch Tray, Hải Phòng',
    '64 Hùng Vương, Huế',
    'Mua tại quầy'
]

sql_template = "('{id}', NULL, NULL, '{kh}', '{nv}', '{ma}', '{type}', {phi}, {tong}, {sau_giam}, {tra}, '{addr}', '{sdt}', {du_kien}, {note}, {status}, {ngay_tao})"

results = []
counter = start_id

# Spread over 30+ days until 200 reached
i = 35 # Start from 35 days ago to be safe
while len(results) < num_invoices:
    day = current_date - timedelta(days=i)
    count = random.randint(invoices_per_day_min, invoices_per_day_max)
    
    for _ in range(count):
        if len(results) >= num_invoices:
            break
            
        # Random time within the day
        hour = random.randint(8, 21)
        minute = random.randint(0, 59)
        invoice_time = day.replace(hour=hour, minute=minute)
        ts = int(invoice_time.timestamp() * 1000)
        
        id_val = f'hd{counter}'
        ma_val = f'HD{counter:03}'
        kh = random.choice(customers)
        nv = random.choice(employees)
        inv_type = random.choice(types)
        status = random.choice(statuses)
        phi = 30000.0 if inv_type == 'ONLINE' else 0.0
        tong = random.randint(5, 50) * 100000.0
        sau_giam = tong + phi
        tra = sau_giam if status == 3 else random.choice([0, sau_giam])
        addr = random.choice(addresses) if inv_type == 'ONLINE' else 'Mua tại quầy'
        sdt = f'09{random.randint(10000000, 99999999)}'
        du_kien = ts + (3 * 24 * 3600 * 1000) if inv_type == 'ONLINE' else 'NULL'
        note = 'NULL'
        
        line = sql_template.format(
            id=id_val, kh=kh, nv=nv, ma=ma_val, type=inv_type, 
            phi=phi, tong=tong, sau_giam=sau_giam, tra=tra,
            addr=addr, sdt=sdt, du_kien=du_kien, note=note, 
            status=status, ngay_tao=ts
        )
        results.append(line)
        counter += 1
    i -= 1
    if i < 0: i = 35

# Generate Details (1-3 products per invoice)
detail_results = []
detail_counter = 100
variant_pool = [f'ct_sp{random.randint(1,25)}_{random.randint(1,10)}' for _ in range(100)] # Realistic variant IDs

for res in results:
    import re
    # Extract ID and date from result line
    match = re.search(r"\('hd(\d+)',.*?, (\d+)\)", res)
    if not match: continue
    
    inv_id = f'hd{match.group(1)}'
    ts = match.group(2)
    
    num_items = random.randint(1, 3)
    for _ in range(num_items):
        v_id = random.choice(variant_pool)
        qty = random.randint(1, 2)
        price = random.randint(10, 50) * 100000.0
        
        detail_line = "('hdct{dc}', '{inv}', '{var}', 'HDCT{dc}', {qty}, {price}, 0, {ts})".format(
            dc=detail_counter, inv=inv_id, var=v_id, qty=qty, price=price, ts=ts
        )
        detail_results.append(detail_line)
        detail_counter += 1

output_sql = "INSERT IGNORE INTO hoa_don (id, id_phieu_giam_gia, id_phieu_giam_gia_ca_nhan, id_khach_hang, id_nhan_vien, ma_hoa_don, loai_don, phi_van_chuyen, tong_tien, tong_tien_sau_giam, tien_nguoi_mua, dia_chi_nguoi_nhan, so_dien_thoai_nguoi_nhan, ngay_du_kien_nhan, ghi_chu, trang_thai, ngay_tao) VALUES\n"
output_sql += ",\n".join(results) + ";\n\n"

output_sql += "INSERT IGNORE INTO hoa_don_chi_tiet (id, id_hoa_don, id_chi_tiet_san_pham, ma_hoa_don_chi_tiet, so_luong, don_gia, trang_thai, ngay_tao) VALUES\n"
output_sql += ",\n".join(detail_results) + ";"

with open('seed_hoa_don.sql', 'w', encoding='utf-8') as f:
    f.write(output_sql)

print(f"Generated {len(results)} invoices and {len(detail_results)} details.")
