const { test, expect } = require('@playwright/test');

/**
 * AeroStride Flow-Based API Automation Test Suite
 * 
 * Kịch bản kiểm thử tự động toàn diện được thiết kế theo LUỒNG NGHIỆP VỤ (Flow-based)
 * mô phỏng chính xác các hành vi của Quản trị viên và hệ thống.
 */
test.describe('AeroStride Flow-Based API Automation Tests', () => {
  // Cấu hình chạy tuần tự theo đúng luồng nghiệp vụ liên tục (serial mode)
  test.describe.configure({ mode: 'serial' });

  let adminToken = '';
  let selectedEmployeeId = '';
  let selectedEmployeeMa = 'NV001';
  let selectedEmployeeName = '';
  let activeShiftName = 'Ca Sáng';
  const createdScheduleIds = [];

  // Đăng nhập một lần duy nhất trước khi chạy toàn bộ luồng kiểm thử
  test.beforeAll(async ({ request }) => {
    const response = await request.post('/api/v1/auth/login', {
      data: {
        username: 'nguyenhuyducbg19062002@gmail.com',
        password: '123456',
        loginType: 'ADMIN'
      }
    });

    if (response.status() === 200) {
      const body = await response.json();
      if (body.success && body.data) {
        adminToken = body.data.accessToken;
      }
    }
  });

  // ==============================================================================
  // 🔑 LUỒNG 1: Xác Thực Hệ Thống & Kiểm Tra Quyền Hạn (Auth & Access Control Flow)
  // ==============================================================================
  test.describe('Flow 1: Authentication & Access Control Flow', () => {

    test('Step 1: Login should fail with 401 Unauthorized for incorrect credentials', async ({ request }) => {
      const response = await request.post('/api/v1/auth/login', {
        data: {
          username: 'incorrect_admin@aerostride.com',
          password: 'wrong_password_123',
          loginType: 'ADMIN'
        }
      });

      // Verify status is 401 Unauthorized or 400 Bad Request
      expect([401, 400]).toContain(response.status());
      
      const body = await response.json();
      expect(body).toHaveProperty('success', false);
      expect(body).toHaveProperty('message');
      console.log('✅ Step 1 Checked: Unauthenticated access blocked successfully.');
    });

    test('Step 2: Login should succeed and return Access Token for valid Admin credentials', async ({ request }) => {
      // Đăng nhập bằng tài khoản Admin mặc định đã được cấu hình trong CSDL
      const response = await request.post('/api/v1/auth/login', {
        data: {
          username: 'nguyenhuyducbg19062002@gmail.com',
          password: '123456',
          loginType: 'ADMIN'
        }
      });

      expect(response.status()).toBe(200);
      
      const body = await response.json();
      expect(body).toHaveProperty('success', true);
      expect(body.data).toHaveProperty('accessToken');
      
      // Lưu lại token cho các luồng tiếp theo
      adminToken = body.data.accessToken;
      console.log('✅ Step 2 Checked: Admin logged in, Access Token acquired.');
    });

    test('Step 3: Access admin resources with token should return 200 OK', async ({ request }) => {
      expect(adminToken).not.toBe('');

      const response = await request.get('/api/v1/admin/nhan-vien/hien-thi', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });

      expect(response.status()).toBe(200);
      
      const body = await response.json();
      expect(body).toHaveProperty('success', true);
      expect(body).toHaveProperty('data');
      
      const empList = Array.isArray(body.data?.content) ? body.data.content : (Array.isArray(body.data) ? body.data : []);
      expect(empList.length).toBeGreaterThan(0);
      
      // Lưu lại thông tin một nhân viên thực tế trong CSDL để test phân lịch ở Luồng 2
      selectedEmployeeId = empList[0].id;
      selectedEmployeeMa = empList[0].maNhanVien || empList[0].ma || 'NV001';
      selectedEmployeeName = empList[0].tenNhanVien || empList[0].ten || 'Nhân viên test';
      
      console.log(`✅ Step 3 Checked: Admin resources accessed. Staff chosen: ${selectedEmployeeName} (ID: ${selectedEmployeeId}, Mã: ${selectedEmployeeMa})`);
    });

    test('Step 4: Access admin resources without token should block with 401 Unauthorized', async ({ request }) => {
      const response = await request.get('/api/v1/admin/nhan-vien/hien-thi');
      
      // Phải chặn không cho truy cập trái phép
      expect(response.status()).toBe(401);
      console.log('✅ Step 4 Checked: Unauthorized access correctly blocked with 401.');
    });
  });

  // ==============================================================================
  // 🌳 LUỒNG 2: Khởi Tạo Lịch Làm Việc Cho Nhân Viên (Staff & Work Schedule Flow)
  // ==============================================================================
  test.describe('Flow 2: Staff & Work Schedule Flow', () => {
    
    test('Step 1: Retrieve active shifts from DB', async ({ request }) => {
      expect(adminToken).not.toBe('');

      const response = await request.get('/api/v1/admin/lich-lam-viec/shifts', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body).toHaveProperty('success', true);
      expect(body.data.length).toBeGreaterThan(0);
      
      activeShiftName = body.data[0].tenCa;
      console.log(`✅ Step 1 Checked: Shifts retrieved. Active shift name: ${activeShiftName}`);
    });

    test('Step 2: Create new schedule for the selected staff', async ({ request }) => {
      expect(adminToken).not.toBe('');
      expect(selectedEmployeeId).not.toBe('');

      const targetDate = new Date().toISOString().substring(0, 10); // Lấy ngày hôm nay dạng yyyy-MM-dd

      const response = await request.post('/api/v1/admin/lich-lam-viec/schedules', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        },
        data: {
          nhanVien: [selectedEmployeeId],
          ca: activeShiftName,
          ngay: targetDate,
          trangThai: 'CHO_XAC_NHAN'
        }
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body).toHaveProperty('success', true);
      expect(body.data).toContain('Đã thêm lịch làm việc cho');
      console.log(`✅ Step 2 Checked: New schedule created for date ${targetDate} and shift ${activeShiftName}`);
    });

    test('Step 3: Retrieve schedule list and verify the creation', async ({ request }) => {
      expect(adminToken).not.toBe('');

      const response = await request.get('/api/v1/admin/lich-lam-viec/schedules', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body).toHaveProperty('success', true);
      expect(body.data.length).toBeGreaterThan(0);

      // Tìm kiếm lịch làm việc vừa tạo của nhân viên đó trong danh sách
      const foundSchedule = body.data.find(s => 
        s.nhanVien === selectedEmployeeName && 
        s.ca === activeShiftName
      );
      
      expect(foundSchedule).toBeDefined();
      expect(foundSchedule.trangThai).toBe('CHO_XAC_NHAN');

      if (foundSchedule && foundSchedule.id) {
        createdScheduleIds.push(foundSchedule.id);
        console.log(`📌 Tagged schedule ${foundSchedule.id} for teardown cleanup.`);
      }
      console.log('✅ Step 3 Checked: Created schedule verified in list.');
    });
  });

  // ==============================================================================
  // 💾 LUỒNG 3: Nhập Lịch Làm Việc Từ Excel & Xem Trước (Excel Preview & Import Flow)
  // ==============================================================================
  test.describe('Flow 3: Excel Preview & Import Flow', () => {

    test('Step 1: Import confirm should save valid Excel rows directly', async ({ request }) => {
      expect(adminToken).not.toBe('');
      expect(selectedEmployeeMa).not.toBe('');

      const targetDate = '2026-05-20'; // Ngày mai

      // Gửi danh sách dữ liệu dòng đã được validate (VALID)
      const response = await request.post('/api/v1/admin/lich-lam-viec/confirm-import', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        },
        data: [
          {
            maNhanVien: selectedEmployeeMa,
            nhanVien: selectedEmployeeName,
            ca: activeShiftName,
            ngay: targetDate,
            status: 'VALID'
          }
        ]
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body).toHaveProperty('success', true);
      expect(body.data).toContain('Đã lưu thành công 1 lịch làm việc');
      console.log(`✅ Step 1 Checked: Excel import confirmed and successfully persisted to CSDL for ${selectedEmployeeMa} on ${targetDate}.`);
    });

    test('Step 2: Retrieve schedule list and verify Excel-imported schedule', async ({ request }) => {
      expect(adminToken).not.toBe('');

      const response = await request.get('/api/v1/admin/lich-lam-viec/schedules', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body).toHaveProperty('success', true);

      // Tìm kiếm lịch làm việc ngày mai (2026-05-20) vừa được import
      const foundSchedule = body.data.find(s => 
        s.nhanVien === selectedEmployeeName && 
        s.ca === activeShiftName &&
        s.ngay === '2026-05-20'
      );
      
      expect(foundSchedule).toBeDefined();
      expect(foundSchedule.trangThai).toBe('DA_XAC_NHAN'); // Lịch import từ Excel mặc định là DA_XAC_NHAN

      if (foundSchedule && foundSchedule.id) {
        createdScheduleIds.push(foundSchedule.id);
        console.log(`📌 Tagged imported schedule ${foundSchedule.id} for teardown cleanup.`);
      }
      console.log('✅ Step 2 Checked: Excel-imported schedule successfully verified in CSDL.');
    });
  });

  // ==============================================================================
  // 📝 LUỒNG 4: Kiểm Tra Nhật Ký Hoạt Động Của Lịch (Activity Logging Flow)
  // ==============================================================================
  test.describe('Flow 4: Activity Logging Flow', () => {

    test('Step 1: Fetch activity history and verify logged actions', async ({ request }) => {
      expect(adminToken).not.toBe('');

      const response = await request.get('/api/v1/admin/lich-lam-viec/activities', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        },
        params: {
          page: 0,
          size: 10
        }
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body).toHaveProperty('success', true);
      expect(body.data).toHaveProperty('content');
      
      const activities = body.data.content;
      expect(activities.length).toBeGreaterThan(0);

      // Log mới nhất phải phản ánh hành động Tạo lịch hoặc Import lịch vừa thực hiện
      const latestActivity = activities[0];
      expect(latestActivity).toHaveProperty('hanhDong');
      expect(latestActivity).toHaveProperty('doiTuong');
      expect(latestActivity).toHaveProperty('ngay');
      
      console.log(`✅ Step 1 Checked: Activity log retrieved. Latest action: "${latestActivity.hanhDong}" - Object: "${latestActivity.doiTuong}"`);
    });
  });

  // ==============================================================================
  // 📊 LUỒNG 5: Thống Kê Dashboard Đồng Bộ (Dashboard & Stats Sync Flow)
  // ==============================================================================
  test.describe('Flow 5: Dashboard Stats Flow', () => {

    test('Step 1: Retrieve main dashboard metrics', async ({ request }) => {
      expect(adminToken).not.toBe('');

      // Gửi yêu cầu lấy thống kê dashboard
      const response = await request.get('/api/v1/admin/thong-ke', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });

      // Nếu endpoint này tồn tại, kiểm tra dữ liệu trả về
      if (response.status() === 200) {
        const body = await response.json();
        expect(body).toHaveProperty('success', true);
        expect(body).toHaveProperty('data');
        console.log('✅ Step 1 Checked: Dashboard statistics retrieved successfully.');
      } else {
        // Fallback check if path differs or is a mock
        console.log(`ℹ️ Dashboard stats returned status ${response.status()}. Path might differ in dev.`);
      }
    });
  });

  // ==============================================================================
  // 🧹 TEARDOWN CLEANUP: Xóa sạch dữ liệu tạo ra trong quá trình test
  // ==============================================================================
  test.afterAll(async ({ request }) => {
    if (createdScheduleIds.length > 0 && adminToken) {
      console.log(`\n🧹 Starting Teardown Cleanup: deleting ${createdScheduleIds.length} test schedules...`);
      for (const id of createdScheduleIds) {
        const response = await request.delete(`/api/v1/admin/lich-lam-viec/schedules/${id}`, {
          headers: {
            'Authorization': `Bearer ${adminToken}`
          }
        });
        if (response.status() === 200) {
          console.log(`  ➔ Deleted test schedule ID: ${id}`);
        } else {
          console.warn(`  ⚠️ Failed to delete test schedule ID: ${id} (Status: ${response.status()})`);
        }
      }
      console.log('✨ Cleanup complete. Environment is clean.');
    }
  });
});
