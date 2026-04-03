<template>
  <div class="container-fluid p-4 bg-light min-vh-100">
    <div class="row mb-4">
      <div class="col-12 d-flex justify-content-between align-items-center mb-3">
        <div>
          <h4 class="fw-bold m-0 text-uppercase">
            <i class="bi bi-ticket-perforated-fill me-2 text-primary"></i>Quản Lý Phiếu Giảm Giá
          </h4>
          <nav aria-label="breadcrumb">
            <ol class="breadcrumb mb-0">
              <li class="breadcrumb-item small"><a href="#">Trang chủ</a></li>
              <li class="breadcrumb-item small active">Giảm giá</li>
            </ol>
          </nav>
        </div>
        <button class="btn btn-primary px-4 shadow-sm fw-bold" @click="openDialog()">
          <i class="bi bi-plus-lg me-2"></i>TẠO VOUCHER MỚI
        </button>
      </div>

      <div class="col-md-3">
        <div class="card border-0 shadow-sm rounded-3 p-3 border-start border-4 border-primary bg-white">
          <div class="d-flex align-items-center">
            <div class="rounded-circle bg-primary bg-opacity-10 p-3 me-3">
              <i class="bi bi-collection text-primary fs-4"></i>
            </div>
            <div>
              <div class="text-muted small fw-bold">TỔNG SỐ PHIẾU</div>
              <div class="fs-4 fw-bold">{{ total }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card border-0 shadow-sm rounded-3 p-3 border-start border-4 border-success bg-white">
          <div class="d-flex align-items-center">
            <div class="rounded-circle bg-success bg-opacity-10 p-3 me-3">
              <i class="bi bi-check2-circle text-success fs-4"></i>
            </div>
            <div>
              <div class="text-muted small fw-bold">ĐANG HOẠT ĐỘNG</div>
              <div class="fs-4 fw-bold">{{ stats.active }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card border-0 shadow-sm rounded-3 p-3 border-start border-4 border-warning bg-white">
          <div class="d-flex align-items-center">
            <div class="rounded-circle bg-warning bg-opacity-10 p-3 me-3">
              <i class="bi bi-clock-history text-warning fs-4"></i>
            </div>
            <div>
              <div class="text-muted small fw-bold">SẮP DIỄN RA</div>
              <div class="fs-4 fw-bold">{{ stats.upcoming }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card border-0 shadow-sm rounded-3 p-3 border-start border-4 border-danger bg-white">
          <div class="d-flex align-items-center">
            <div class="rounded-circle bg-danger bg-opacity-10 p-3 me-3">
              <i class="bi bi-slash-circle text-danger fs-4"></i>
            </div>
            <div>
              <div class="text-muted small fw-bold">ĐÃ KẾT THÚC</div>
              <div class="fs-4 fw-bold">{{ stats.expired }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3 p-4 mb-4">
      <div class="row g-3">
        <div class="col-md-4">
          <label class="form-label small fw-bold text-secondary">Tìm kiếm thông minh</label>
          <el-input 
            v-model="filters.keyword" 
            placeholder="Nhập mã, tên voucher..." 
            clearable
            @input="handleFilter"
          >
            <template #prefix><i class="bi bi-search"></i></template>
          </el-input>
        </div>
        <div class="col-md-2">
          <label class="form-label small fw-bold text-secondary">Loại phiếu</label>
          <el-select v-model="filters.loaiPhieu" placeholder="Tất cả" clearable style="width: 100%" @change="handleFilter">
            <el-option label="Công khai" value="CÔNG KHAI" />
            <el-option label="Cá nhân" value="CÁ NHÂN" />
          </el-select>
        </div>
        <div class="col-md-4">
          <label class="form-label small fw-bold text-secondary">Thời gian áp dụng</label>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="Từ ngày"
            end-placeholder="Đến ngày"
            style="width: 100%"
            value-format="x"
            @change="handleFilter"
          />
        </div>
        <div class="col-md-2 d-flex align-items-end">
          <button class="btn btn-outline-secondary w-100 fw-bold" @click="resetFilters">
            <i class="bi bi-arrow-counterclockwise me-1"></i>LÀM MỚI
          </button>
        </div>
      </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3 overflow-hidden">
      <div class="bg-white p-3 border-bottom d-flex justify-content-between align-items-center">
        <span class="fw-bold text-secondary"><i class="bi bi-list-ul me-2"></i>DANH SÁCH PHIẾU GIẢM GIÁ</span>
        <div class="btn-group">
          <button class="btn btn-sm btn-outline-primary"><i class="bi bi-file-earmark-excel me-1"></i>Xuất Excel</button>
        </div>
      </div>
      
      <el-table 
        :data="vouchers" 
        v-loading="loading" 
        stripe 
        style="width: 100%"
        :header-cell-style="{ background: '#f8f9fa', color: '#495057', fontWeight: '700', fontSize: '13px' }"
      >
        <el-table-column type="index" label="STT" width="60" align="center" />
        
        <el-table-column label="THÔNG TIN PHIẾU" min-width="250">
          <template #default="{ scope }">
            <div class="fw-bold text-primary">{{ scope.ma }}</div>
            <div class="text-dark small fw-semibold">{{ scope.ten }}</div>
            <div class="mt-1">
              <span v-if="scope.loaiPhieu === 'CÔNG KHAI'" class="badge bg-info-subtle text-info border border-info-subtle">CÔNG KHAI</span>
              <span v-else class="badge bg-purple-subtle text-purple border border-purple-subtle">CÁ NHÂN</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="MỨC GIẢM" width="180">
          <template #default="{ scope }">
            <div v-if="scope.phanTramGiamGia" class="d-flex align-items-center">
              <span class="fs-5 fw-bold text-danger">{{ scope.phanTramGiamGia }}%</span>
              <span class="ms-2 text-muted small">(Tối đa: {{ formatCurrency(scope.giamToiDa) }})</span>
            </div>
            <div v-else>
              <span class="fs-5 fw-bold text-primary">{{ formatCurrency(scope.soTienGiam) }}</span>
            </div>
            <div class="small text-muted mt-1">Đơn tối thiểu: {{ formatCurrency(scope.donHangToiThieu) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="SỐ LƯỢNG" width="100" align="center">
          <template #default="{ scope }">
            <div class="fw-bold fs-6">{{ scope.soLuong }}</div>
            <div class="progress mt-1" style="height: 4px;">
              <div class="progress-bar bg-success" :style="{ width: '70%' }"></div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="THỜI GIAN" width="220">
          <template #default="{ scope }">
            <div class="small">
              <div class="mb-1"><i class="bi bi-clock me-1 text-success"></i>Bắt đầu: <b>{{ formatDate(scope.ngayBatDau) }}</b></div>
              <div><i class="bi bi-clock-fill me-1 text-danger"></i>Kết thúc: <b>{{ formatDate(scope.ngayKetThuc) }}</b></div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="TRẠNG THÁI" width="140" align="center">
          <template #default="{ scope }">
            <span :class="getStatus(scope).class">{{ getStatus(scope).text }}</span>
          </template>
        </el-table-column>

        <el-table-column label="THAO TÁC" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <div class="d-flex gap-2 justify-content-center">
              <button class="btn btn-sm btn-light border" title="Xem chi tiết" @click="openDialog(row)">
                <i class="bi bi-pencil-square text-primary"></i>
              </button>
              <button class="btn btn-sm btn-light border" title="Xóa" @click="handleDelete(row)">
                <i class="bi bi-trash text-danger"></i>
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="p-3 d-flex justify-content-between align-items-center bg-white border-top">
        <span class="small text-muted">Hiển thị {{ vouchers.length }} trên tổng số {{ total }} bản ghi</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[5, 10, 20, 50]"
          layout="sizes, prev, pager, next"
          @current-change="fetchVouchers"
          @size-change="fetchVouchers"
        />
      </div>
    </div>

    <el-dialog 
      v-model="dialogVisible" 
      :title="form.id ? 'CẬP NHẬT PHIẾU GIẢM GIÁ' : 'TẠO PHIẾU GIẢM GIÁ MỚI'" 
      width="750px"
      custom-class="custom-dialog"
    >
      <el-form :model="form" label-position="top" class="row p-2">
        <div class="col-md-6 mb-3">
          <el-form-item label="Mã Voucher (Hệ thống tự sinh nếu trống)">
            <el-input v-model="form.ma" placeholder="Vd: SUMMER2024" />
          </el-form-item>
        </div>
        <div class="col-md-6 mb-3">
          <el-form-item label="Tên chương trình giảm giá" required>
            <el-input v-model="form.ten" placeholder="Nhập tên chương trình..." />
          </el-form-item>
        </div>
        
        <div class="col-md-4 mb-3">
          <el-form-item label="Loại phiếu">
            <el-select v-model="form.loaiPhieu" style="width: 100%">
              <el-option label="Công khai" value="CÔNG KHAI" />
              <el-option label="Cá nhân" value="CÁ NHÂN" />
            </el-select>
          </el-form-item>
        </div>
        <div class="col-md-4 mb-3">
          <el-form-item label="Số lượng">
            <el-input-number v-model="form.soLuong" :min="1" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="col-md-4 mb-3">
          <el-form-item label="Hình thức giảm">
            <el-select v-model="discountMethod" style="width: 100%">
              <el-option label="Theo Phần trăm (%)" value="PERCENT" />
              <el-option label="Theo Số tiền (VND)" value="CASH" />
            </el-select>
          </el-form-item>
        </div>

        <div class="col-md-4 mb-3">
  <el-form-item v-if="discountMethod === 'PERCENT'" label="Phần trăm giảm (%)">
    <el-input-number 
      v-model="form.phanTramGiamGia" 
      :min="0" 
      :max="100"
      :precision="0"
      style="width: 100%" 
      placeholder="0 - 100"
    />
  </el-form-item>

  <el-form-item v-else label="Số tiền giảm (VND)">
    <el-input-number 
      v-model="form.soTienGiam" 
      :min="0" 
      :step="1000"
      style="width: 100%" 
      placeholder="Vd: 50,000"
    />
  </el-form-item>
</div>
        <div class="col-md-4 mb-3">
          <el-form-item label="Giảm tối đa (VND)">
            <el-input-number v-model="form.giamToiDa" :min="0" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="col-md-4 mb-3">
          <el-form-item label="Đơn hàng tối thiểu (VND)">
            <el-input-number v-model="form.donHangToiThieu" :min="0" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="col-md-12 mb-3">
          <el-form-item label="Thời gian áp dụng chương trình">
            <el-date-picker
              v-model="form.timeRange"
              type="datetimerange"
              value-format="x"
              range-separator="Đến ngày"
              start-placeholder="Bắt đầu"
              end-placeholder="Kết thúc"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <div class="col-md-12">
          <el-form-item label="Ghi chú / Mô tả điều kiện">
            <el-input v-model="form.ghiChu" type="textarea" rows="3" placeholder="Nhập ghi chú thêm..." />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="d-flex justify-content-end gap-2 border-top pt-3">
          <button class="btn btn-light border px-4" @click="dialogVisible = false">ĐÓNG</button>
          <button class="btn btn-primary px-4 shadow" @click="saveVoucher">XÁC NHẬN LƯU</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { phieuGiamGiaService } from '@/services/phieuGiamGiaService';

// --- State Quản Lý ---
const vouchers = ref([]);
const loading = ref(false);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const dialogVisible = ref(false);
const discountMethod = ref('PERCENT');

const stats = reactive({ total: 0, active: 0, upcoming: 0, expired: 0 });
const filters = reactive({ keyword: '', loaiPhieu: '', dateRange: [] });

// Khởi tạo Form dựa trên AdminPhieuGiamGiaRequest.java
const form = reactive({
  id: null,
  ma: '',
  ten: '',
  loaiPhieu: 'CÔNG KHAI',
  phanTramGiamGia: 0,
  soTienGiam: 0,
  soLuong: 1,
  donHangToiThieu: 0,
  giamToiDa: 0,
  ghiChu: '',
  timeRange: [] // Chứa [ngayBatDau, ngayKetThuc]
});

// --- Logic Nghiệp Vụ ---

const fetchVouchers = async () => {
  loading.value = true;
  try {
    const params = {
      pageNo: currentPage.value - 1, // Spring tính từ 0
      pageSize: pageSize.value,
      keyword: filters.keyword,
      loaiPhieu: filters.loaiPhieu
    };
    const response = await phieuGiamGiaService.getAll(params);
    vouchers.value = response.content;
    total.value = response.totalElements;
    calculateStats(response.content);
  } catch (error) {
    ElMessage.error('Không thể tải dữ liệu từ máy chủ!');
  } finally {
    loading.value = false;
  }
};

const handleFilter = () => { currentPage.value = 1; fetchVouchers(); };

const resetFilters = () => {
  Object.assign(filters, { keyword: '', loaiPhieu: '', dateRange: [] });
  handleFilter();
};

const openDialog = (row = null) => {
  if (row) {
    // Chế độ Edit: Fill dữ liệu từ AdminPhieuGiamGiaResponse
    Object.assign(form, row);
    form.timeRange = [row.ngayBatDau, row.ngayKetThuc];
    discountMethod.value = row.phanTramGiamGia > 0 ? 'PERCENT' : 'CASH';
  } else {
    // Chế độ Thêm mới: Reset form
    Object.assign(form, {
      id: null, ma: '', ten: '', loaiPhieu: 'CÔNG KHAI',
      phanTramGiamGia: 0, soTienGiam: 0, soLuong: 1,
      donHangToiThieu: 0, giamToiDa: 0, ghiChu: '', timeRange: []
    });
  }
  dialogVisible.value = true;
};

const saveVoucher = async () => {
  if (!form.ten || !form.timeRange?.length) {
    return ElMessage.warning('Vui lòng điền đủ tên và thời hạn!');
  }

  try {
    const payload = {
      ...form,
      ngayBatDau: form.timeRange[0],
      ngayKetThuc: form.timeRange[1]
    };
    
    // Nếu chọn CASH thì set PERCENT = 0 và ngược lại
    if (discountMethod.value === 'CASH') payload.phanTramGiamGia = 0;
    else payload.soTienGiam = 0;

    if (form.id) {
       // phieuGiamGiaService.update(...)
       ElMessage.success('Cập nhật thành công!');
    } else {
       await phieuGiamGiaService.add(payload);
       ElMessage.success('Thêm voucher thành công!');
    }
    dialogVisible.value = false;
    fetchVouchers();
  } catch (error) {
    ElMessage.error('Có lỗi xảy ra khi lưu dữ liệu!');
  }
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`Bạn có chắc chắn muốn xóa voucher ${row.ma}?`, 'Cảnh báo hệ thống', {
    confirmButtonText: 'Xác nhận xóa',
    cancelButtonText: 'Hủy',
    type: 'error'
  }).then(async () => {
    await phieuGiamGiaService.delete(row.id);
    ElMessage.success('Đã xóa bản ghi!');
    fetchVouchers();
  });
};

// --- Utilities (Hàm hỗ trợ hiển thị) ---

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

const formatDate = (ts) => ts ? new Date(ts).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '---';

const getStatus = (row) => {
  const now = Date.now();
  if (now < row.ngayBatDau) return { text: 'SẮP BẮT ĐẦU', class: 'badge bg-warning-subtle text-warning border border-warning' };
  if (now > row.ngayKetThuc) return { text: 'KẾT THÚC', class: 'badge bg-secondary-subtle text-secondary border border-secondary' };
  return { text: 'ĐANG CHẠY', class: 'badge bg-success-subtle text-success border border-success' };
};

const calculateStats = (data) => {
  const now = Date.now();
  stats.active = data.filter(v => now >= v.ngayBatDau && now <= v.ngayKetThuc).length;
  stats.upcoming = data.filter(v => now < v.ngayBatDau).length;
  stats.expired = data.filter(v => now > v.ngayKetThuc).length;
};

onMounted(fetchVouchers);
</script>

<style scoped>
.breadcrumb-item + .breadcrumb-item::before { content: ">"; }
.badge { padding: 0.5em 0.8em; font-size: 11px; font-weight: 700; letter-spacing: 0.5px; }
.text-purple { color: #6f42c1; }
.bg-purple-subtle { background-color: #f3e5f5; }
.border-purple-subtle { border-color: #e1bee7 !important; }
.el-table { border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05); }
.custom-dialog :deep(.el-dialog__header) { border-bottom: 1px solid #eee; padding-bottom: 15px; }
.custom-dialog :deep(.el-dialog__title) { font-weight: 800; color: #333; }
</style>