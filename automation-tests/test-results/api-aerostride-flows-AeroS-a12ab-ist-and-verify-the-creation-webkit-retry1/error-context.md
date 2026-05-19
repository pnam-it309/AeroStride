# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: api\aerostride-flows.spec.js >> AeroStride Flow-Based API Automation Tests >> Flow 2: Staff & Work Schedule Flow >> Step 3: Retrieve schedule list and verify the creation
- Location: tests\api\aerostride-flows.spec.js:165:5

# Error details

```
Error: expect(received).toBe(expected) // Object.is equality

Expected: "CHO_XAC_NHAN"
Received: "DA_XAC_NHAN"
```

# Test source

```ts
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
  172 |       });
  173 | 
  174 |       expect(response.status()).toBe(200);
  175 |       const body = await response.json();
  176 |       expect(body).toHaveProperty('success', true);
  177 |       expect(body.data.length).toBeGreaterThan(0);
  178 | 
  179 |       // Tìm kiếm lịch làm việc vừa tạo của nhân viên đó trong danh sách
  180 |       const foundSchedule = body.data.find(s => 
  181 |         s.nhanVien === selectedEmployeeName && 
  182 |         s.ca === activeShiftName
  183 |       );
  184 |       
  185 |       expect(foundSchedule).toBeDefined();
> 186 |       expect(foundSchedule.trangThai).toBe('CHO_XAC_NHAN');
      |                                       ^ Error: expect(received).toBe(expected) // Object.is equality
  187 | 
  188 |       if (foundSchedule && foundSchedule.id) {
  189 |         createdScheduleIds.push(foundSchedule.id);
  190 |         console.log(`📌 Tagged schedule ${foundSchedule.id} for teardown cleanup.`);
  191 |       }
  192 |       console.log('✅ Step 3 Checked: Created schedule verified in list.');
  193 |     });
  194 |   });
  195 | 
  196 |   // ==============================================================================
  197 |   // 💾 LUỒNG 3: Nhập Lịch Làm Việc Từ Excel & Xem Trước (Excel Preview & Import Flow)
  198 |   // ==============================================================================
  199 |   test.describe('Flow 3: Excel Preview & Import Flow', () => {
  200 | 
  201 |     test('Step 1: Import confirm should save valid Excel rows directly', async ({ request }) => {
  202 |       expect(adminToken).not.toBe('');
  203 |       expect(selectedEmployeeMa).not.toBe('');
  204 | 
  205 |       const targetDate = '2026-05-20'; // Ngày mai
  206 | 
  207 |       // Gửi danh sách dữ liệu dòng đã được validate (VALID)
  208 |       const response = await request.post('/api/v1/admin/lich-lam-viec/confirm-import', {
  209 |         headers: {
  210 |           'Authorization': `Bearer ${adminToken}`
  211 |         },
  212 |         data: [
  213 |           {
  214 |             maNhanVien: selectedEmployeeMa,
  215 |             nhanVien: selectedEmployeeName,
  216 |             ca: activeShiftName,
  217 |             ngay: targetDate,
  218 |             status: 'VALID'
  219 |           }
  220 |         ]
  221 |       });
  222 | 
  223 |       expect(response.status()).toBe(200);
  224 |       const body = await response.json();
  225 |       expect(body).toHaveProperty('success', true);
  226 |       expect(body.data).toContain('Đã lưu thành công 1 lịch làm việc');
  227 |       console.log(`✅ Step 1 Checked: Excel import confirmed and successfully persisted to CSDL for ${selectedEmployeeMa} on ${targetDate}.`);
  228 |     });
  229 | 
  230 |     test('Step 2: Retrieve schedule list and verify Excel-imported schedule', async ({ request }) => {
  231 |       expect(adminToken).not.toBe('');
  232 | 
  233 |       const response = await request.get('/api/v1/admin/lich-lam-viec/schedules', {
  234 |         headers: {
  235 |           'Authorization': `Bearer ${adminToken}`
  236 |         }
  237 |       });
  238 | 
  239 |       expect(response.status()).toBe(200);
  240 |       const body = await response.json();
  241 |       expect(body).toHaveProperty('success', true);
  242 | 
  243 |       // Tìm kiếm lịch làm việc ngày mai (2026-05-20) vừa được import
  244 |       const foundSchedule = body.data.find(s => 
  245 |         s.nhanVien === selectedEmployeeName && 
  246 |         s.ca === activeShiftName &&
  247 |         s.ngay === '2026-05-20'
  248 |       );
  249 |       
  250 |       expect(foundSchedule).toBeDefined();
  251 |       expect(foundSchedule.trangThai).toBe('DA_XAC_NHAN'); // Lịch import từ Excel mặc định là DA_XAC_NHAN
  252 | 
  253 |       if (foundSchedule && foundSchedule.id) {
  254 |         createdScheduleIds.push(foundSchedule.id);
  255 |         console.log(`📌 Tagged imported schedule ${foundSchedule.id} for teardown cleanup.`);
  256 |       }
  257 |       console.log('✅ Step 2 Checked: Excel-imported schedule successfully verified in CSDL.');
  258 |     });
  259 |   });
  260 | 
  261 |   // ==============================================================================
  262 |   // 📝 LUỒNG 4: Kiểm Tra Nhật Ký Hoạt Động Của Lịch (Activity Logging Flow)
  263 |   // ==============================================================================
  264 |   test.describe('Flow 4: Activity Logging Flow', () => {
  265 | 
  266 |     test('Step 1: Fetch activity history and verify logged actions', async ({ request }) => {
  267 |       expect(adminToken).not.toBe('');
  268 | 
  269 |       const response = await request.get('/api/v1/admin/lich-lam-viec/activities', {
  270 |         headers: {
  271 |           'Authorization': `Bearer ${adminToken}`
  272 |         },
  273 |         params: {
  274 |           page: 0,
  275 |           size: 10
  276 |         }
  277 |       });
  278 | 
  279 |       expect(response.status()).toBe(200);
  280 |       const body = await response.json();
  281 |       expect(body).toHaveProperty('success', true);
  282 |       expect(body.data).toHaveProperty('content');
  283 |       
  284 |       const activities = body.data.content;
  285 |       expect(activities.length).toBeGreaterThan(0);
  286 | 
```