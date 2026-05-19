# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: api\aerostride-flows.spec.js >> AeroStride Flow-Based API Automation Tests >> Flow 1: Authentication & Access Control Flow >> Step 2: Login should succeed and return Access Token for valid Admin credentials
- Location: tests\api\aerostride-flows.spec.js:61:5

# Error details

```
Error: expect(received).toBe(expected) // Object.is equality

Expected: 200
Received: 500
```

# Test source

```ts
  1   | const { test, expect } = require('@playwright/test');
  2   | 
  3   | /**
  4   |  * AeroStride Flow-Based API Automation Test Suite
  5   |  * 
  6   |  * Kịch bản kiểm thử tự động toàn diện được thiết kế theo LUỒNG NGHIỆP VỤ (Flow-based)
  7   |  * mô phỏng chính xác các hành vi của Quản trị viên và hệ thống.
  8   |  */
  9   | test.describe('AeroStride Flow-Based API Automation Tests', () => {
  10  |   // Cấu hình chạy tuần tự theo đúng luồng nghiệp vụ liên tục (serial mode)
  11  |   test.describe.configure({ mode: 'serial' });
  12  | 
  13  |   let adminToken = '';
  14  |   let selectedEmployeeId = '';
  15  |   let selectedEmployeeMa = 'NV001';
  16  |   let selectedEmployeeName = '';
  17  |   let activeShiftName = 'Ca Sáng';
  18  |   const createdScheduleIds = [];
  19  | 
  20  |   // Đăng nhập một lần duy nhất trước khi chạy toàn bộ luồng kiểm thử
  21  |   test.beforeAll(async ({ request }) => {
  22  |     const response = await request.post('/api/v1/auth/login', {
  23  |       data: {
  24  |         username: 'nguyenhuyducbg19062002@gmail.com',
  25  |         password: '123456',
  26  |         loginType: 'ADMIN'
  27  |       }
  28  |     });
  29  | 
  30  |     if (response.status() === 200) {
  31  |       const body = await response.json();
  32  |       if (body.success && body.data) {
  33  |         adminToken = body.data.accessToken;
  34  |       }
  35  |     }
  36  |   });
  37  | 
  38  |   // ==============================================================================
  39  |   // 🔑 LUỒNG 1: Xác Thực Hệ Thống & Kiểm Tra Quyền Hạn (Auth & Access Control Flow)
  40  |   // ==============================================================================
  41  |   test.describe('Flow 1: Authentication & Access Control Flow', () => {
  42  | 
  43  |     test('Step 1: Login should fail with 401 Unauthorized for incorrect credentials', async ({ request }) => {
  44  |       const response = await request.post('/api/v1/auth/login', {
  45  |         data: {
  46  |           username: 'incorrect_admin@aerostride.com',
  47  |           password: 'wrong_password_123',
  48  |           loginType: 'ADMIN'
  49  |         }
  50  |       });
  51  | 
  52  |       // Verify status is 401 Unauthorized or 400 Bad Request
  53  |       expect([401, 400]).toContain(response.status());
  54  |       
  55  |       const body = await response.json();
  56  |       expect(body).toHaveProperty('success', false);
  57  |       expect(body).toHaveProperty('message');
  58  |       console.log('✅ Step 1 Checked: Unauthenticated access blocked successfully.');
  59  |     });
  60  | 
  61  |     test('Step 2: Login should succeed and return Access Token for valid Admin credentials', async ({ request }) => {
  62  |       // Đăng nhập bằng tài khoản Admin mặc định đã được cấu hình trong CSDL
  63  |       const response = await request.post('/api/v1/auth/login', {
  64  |         data: {
  65  |           username: 'nguyenhuyducbg19062002@gmail.com',
  66  |           password: '123456',
  67  |           loginType: 'ADMIN'
  68  |         }
  69  |       });
  70  | 
> 71  |       expect(response.status()).toBe(200);
      |                                 ^ Error: expect(received).toBe(expected) // Object.is equality
  72  |       
  73  |       const body = await response.json();
  74  |       expect(body).toHaveProperty('success', true);
  75  |       expect(body.data).toHaveProperty('accessToken');
  76  |       
  77  |       // Lưu lại token cho các luồng tiếp theo
  78  |       adminToken = body.data.accessToken;
  79  |       console.log('✅ Step 2 Checked: Admin logged in, Access Token acquired.');
  80  |     });
  81  | 
  82  |     test('Step 3: Access admin resources with token should return 200 OK', async ({ request }) => {
  83  |       expect(adminToken).not.toBe('');
  84  | 
  85  |       const response = await request.get('/api/v1/admin/nhan-vien/hien-thi', {
  86  |         headers: {
  87  |           'Authorization': `Bearer ${adminToken}`
  88  |         }
  89  |       });
  90  | 
  91  |       expect(response.status()).toBe(200);
  92  |       
  93  |       const body = await response.json();
  94  |       expect(body).toHaveProperty('success', true);
  95  |       expect(body).toHaveProperty('data');
  96  |       
  97  |       const empList = Array.isArray(body.data?.content) ? body.data.content : (Array.isArray(body.data) ? body.data : []);
  98  |       expect(empList.length).toBeGreaterThan(0);
  99  |       
  100 |       // Lưu lại thông tin một nhân viên thực tế trong CSDL để test phân lịch ở Luồng 2
  101 |       selectedEmployeeId = empList[0].id;
  102 |       selectedEmployeeMa = empList[0].maNhanVien || empList[0].ma || 'NV001';
  103 |       selectedEmployeeName = empList[0].tenNhanVien || empList[0].ten || 'Nhân viên test';
  104 |       
  105 |       console.log(`✅ Step 3 Checked: Admin resources accessed. Staff chosen: ${selectedEmployeeName} (ID: ${selectedEmployeeId}, Mã: ${selectedEmployeeMa})`);
  106 |     });
  107 | 
  108 |     test('Step 4: Access admin resources without token should block with 401 Unauthorized', async ({ request }) => {
  109 |       const response = await request.get('/api/v1/admin/nhan-vien/hien-thi');
  110 |       
  111 |       // Phải chặn không cho truy cập trái phép
  112 |       expect(response.status()).toBe(401);
  113 |       console.log('✅ Step 4 Checked: Unauthorized access correctly blocked with 401.');
  114 |     });
  115 |   });
  116 | 
  117 |   // ==============================================================================
  118 |   // 🌳 LUỒNG 2: Khởi Tạo Lịch Làm Việc Cho Nhân Viên (Staff & Work Schedule Flow)
  119 |   // ==============================================================================
  120 |   test.describe('Flow 2: Staff & Work Schedule Flow', () => {
  121 |     
  122 |     test('Step 1: Retrieve active shifts from DB', async ({ request }) => {
  123 |       expect(adminToken).not.toBe('');
  124 | 
  125 |       const response = await request.get('/api/v1/admin/lich-lam-viec/shifts', {
  126 |         headers: {
  127 |           'Authorization': `Bearer ${adminToken}`
  128 |         }
  129 |       });
  130 | 
  131 |       expect(response.status()).toBe(200);
  132 |       const body = await response.json();
  133 |       expect(body).toHaveProperty('success', true);
  134 |       expect(body.data.length).toBeGreaterThan(0);
  135 |       
  136 |       activeShiftName = body.data[0].tenCa;
  137 |       console.log(`✅ Step 1 Checked: Shifts retrieved. Active shift name: ${activeShiftName}`);
  138 |     });
  139 | 
  140 |     test('Step 2: Create new schedule for the selected staff', async ({ request }) => {
  141 |       expect(adminToken).not.toBe('');
  142 |       expect(selectedEmployeeId).not.toBe('');
  143 | 
  144 |       const targetDate = new Date().toISOString().substring(0, 10); // Lấy ngày hôm nay dạng yyyy-MM-dd
  145 | 
  146 |       const response = await request.post('/api/v1/admin/lich-lam-viec/schedules', {
  147 |         headers: {
  148 |           'Authorization': `Bearer ${adminToken}`
  149 |         },
  150 |         data: {
  151 |           nhanVien: [selectedEmployeeId],
  152 |           ca: activeShiftName,
  153 |           ngay: targetDate,
  154 |           trangThai: 'CHO_XAC_NHAN'
  155 |         }
  156 |       });
  157 | 
  158 |       expect(response.status()).toBe(200);
  159 |       const body = await response.json();
  160 |       expect(body).toHaveProperty('success', true);
  161 |       expect(body.data).toContain('Đã thêm lịch làm việc cho');
  162 |       console.log(`✅ Step 2 Checked: New schedule created for date ${targetDate} and shift ${activeShiftName}`);
  163 |     });
  164 | 
  165 |     test('Step 3: Retrieve schedule list and verify the creation', async ({ request }) => {
  166 |       expect(adminToken).not.toBe('');
  167 | 
  168 |       const response = await request.get('/api/v1/admin/lich-lam-viec/schedules', {
  169 |         headers: {
  170 |           'Authorization': `Bearer ${adminToken}`
  171 |         }
```