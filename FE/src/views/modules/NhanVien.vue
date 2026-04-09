<script setup>
import { ref, onMounted } from 'vue';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { EditIcon, UsersIcon } from 'vue-tabler-icons';

const loading = ref(false);
const isRefreshing = ref(false);
const employees = ref([]);
const showEmployeeDialog = ref(false);
const selectedEmployee = ref(null);
const isEditMode = ref(false);

const employeeForm = ref({
  ten: '', email: '', sdt: '', tenTaiKhoan: '', matKhau: '', ngaySinh: '', idPhanQuyen: '', gioiTinh: true, trangThai: 'DANG_HOAT_DONG'
});

const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ keyword: '', trangThai: null, gioiTinh: null });

// Confirmation Logic
const confirmDialog = ref({
  show: false, title: '', message: '', color: 'primary', action: null, loading: false
});

const tableHeaders = [
  { text: 'Thông tin nhân viên', align: 'center' },
  { text: 'Liên hệ', align: 'center', width: '250px' },
  { text: 'Giới tính', align: 'center', width: '120px' },
  { text: 'Trạng thái', align: 'center', width: '180px' },
  { text: 'Thao tác', align: 'center', width: '120px' }
];

const loadEmployees = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      size: pagination.value.size,
      keyword: filters.value.keyword || null,
      trangThai: filters.value.trangThai || null,
      gioiTinh: filters.value.gioiTinh !== null ? filters.value.gioiTinh : null
    };
    const response = await dichVuNhanVien.layNhanVienPhanTrang(params);
    employees.value = response.content || response;
    pagination.value.totalElements = response.totalElements || employees.value.length;
    pagination.value.totalPages = response.totalPages || 1;
  } catch (error) { console.error(error); } finally { loading.value = false; }
};

const handleRefresh = async () => {
  isRefreshing.value = true; filters.value = { keyword: '', trangThai: null, gioiTinh: null };
  pagination.value.page = 1; await loadEmployees();
  setTimeout(() => isRefreshing.value = false, 800);
};

const confirmChangeStatus = (item) => {
  confirmDialog.value = {
    show: true, title: 'Thay đổi trạng thái',
    message: `Bạn có chắc muốn đổi trạng thái của nhân viên [${item.ten}]?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
        await dichVuNhanVien.thayDoiTrangThaiNhanVien(item.id, newS);
        item.trangThai = newS; confirmDialog.value.show = false;
      } catch (e) {} finally { confirmDialog.value.loading = false; }
    }
  };
};

const confirmSaveEmployee = () => {
  confirmDialog.value = {
    show: true, title: isEditMode.value ? 'Cập nhật nhân viên' : 'Thêm nhân viên',
    message: `Bạn có chắc chắn muốn lưu thông tin nhân viên [${employeeForm.value.ten}]?`,
    color: 'success',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        if (isEditMode.value) await updateEmployee(); else await createEmployee();
        confirmDialog.value.show = false;
      } catch (e) {} finally { confirmDialog.value.loading = false; }
    }
  };
};

const updateEmployee = async () => {
  const updated = await dichVuNhanVien.capNhatNhanVien(selectedEmployee.value.id, employeeForm.value);
  const index = employees.value.findIndex(e => e.id === selectedEmployee.value.id);
  if (index !== -1) employees.value[index] = updated;
  showEmployeeDialog.value = false;
};

const createEmployee = async () => {
  const created = await dichVuNhanVien.taoNhanVien(employeeForm.value);
  employees.value.unshift(created); showEmployeeDialog.value = false; loadEmployees();
};

const editEmployee = (e) => { selectedEmployee.value = e; employeeForm.value = { ...e }; isEditMode.value = true; showEmployeeDialog.value = true; };
const openCreateDialog = () => { employeeForm.value = { ten: '', email: '', sdt: '', tenTaiKhoan: '', matKhau: '', ngaySinh: '', idPhanQuyen: '', gioiTinh: true, trangThai: 'DANG_HOAT_DONG' }; isEditMode.value = false; showEmployeeDialog.value = true; };

onMounted(() => loadEmployees());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <v-row class="mb-4">
      <v-col cols="12">
        <h2 class="text-h4 font-weight-bold text-dark mb-1">Quản lý nhân viên</h2>
        <div class="text-subtitle-1 text-medium-emphasis">Điều hành đội ngũ nhân sự hệ thống AeroStride</div>
      </v-col>
    </v-row>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="3"><v-text-field v-model="filters.keyword" placeholder="Tên, SĐT, Email..." variant="outlined" density="comfortable" hide-details prepend-inner-icon="mdi-magnify" class="font-weight-black" @keyup.enter="loadEmployees"></v-text-field></v-col>
      <v-col cols="12" md="3"><v-select v-model="filters.trangThai" :items="[{title:'Tất cả trạng thái',value:null}, {title:'Đang làm việc',value:'DANG_HOAT_DONG'}, {title:'Đã nghỉ việc',value:'KHONG_HOAT_DONG'}]" variant="outlined" density="comfortable" hide-details class="font-weight-black" @update:model-value="loadEmployees"></v-select></v-col>
      <v-col cols="12" md="3"><v-select v-model="filters.gioiTinh" :items="[{title:'Tất cả giới tính',value:null}, {title:'Nam',value:true}, {title:'Nữ',value:false}]" variant="outlined" density="comfortable" hide-details class="font-weight-black" @update:model-value="loadEmployees"></v-select></v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable title="Danh sách nhân viên" addButtonText="Thêm nhân viên" :headers="tableHeaders" :items="employees" :total-count="pagination.totalElements" :loading="loading" @add="openCreateDialog">
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell">
            <div class="d-flex align-center justify-center">
              <v-avatar color="primary-lighten-4" size="44" class="mr-3 font-weight-black border rounded-0">{{ item.ten.charAt(0).toUpperCase() }}</v-avatar>
              <div class="text-left">
                <div class="text-subtitle-1 font-weight-black text-dark">{{ item.ten }}</div>
                <div class="text-caption font-weight-bold text-primary">#{{ (item.ma || 'NV' + item.id).toUpperCase() }}</div>
              </div>
            </div>
          </td>
          <td class="data-cell">
            <div class="text-subtitle-2 font-weight-bold text-dark mb-1">{{ item.email || '-' }}</div>
            <div class="text-caption font-weight-bold text-medium-emphasis d-flex align-center justify-center font-weight-black"><v-icon size="x-small" class="mr-1">mdi-phone</v-icon> {{ item.sdt }}</div>
          </td>
          <td class="data-cell" style="text-align: center;"><v-chip variant="flat" :color="item.gioiTinh ? 'blue-lighten-4' : 'pink-lighten-4'" size="x-small" class="font-weight-black px-4">{{ item.gioiTinh ? 'Nam' : 'Nữ' }}</v-chip></td>
          <td class="data-cell" style="text-align: center;"><v-chip :color="item.trangThai === 'DANG_HOAT_DONG' ? 'success' : 'error'" size="x-small" variant="flat" class="font-weight-black px-4">{{ item.trangThai === 'DANG_HOAT_DONG' ? 'Đang làm việc' : 'Đã nghỉ việc' }}</v-chip></td>
          <td class="data-cell" style="text-align: center;">
            <div class="d-flex align-center justify-center">
              <v-switch :model-value="item.trangThai === 'DANG_HOAT_DONG'" color="success" inset hide-details density="compact" class="tight-switch" @click.stop="confirmChangeStatus(item)"></v-switch>
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-0" @click.stop="editEmployee(item)"><EditIcon size="18" /></v-btn>
            </div>
          </td>
        </tr>
      </template>
      <template #pagination>
        <AdminPagination v-model="pagination.page" v-model:page-size="pagination.size" :total-pages="pagination.totalPages" :total-elements="pagination.totalElements" :current-size="employees.length" @change="loadEmployees" />
      </template>
    </AdminTable>

    <!-- Dialog (SQUARE) -->
    <v-dialog v-model="showEmployeeDialog" max-width="700">
      <v-card class="rounded-0 border shadow-2xl">
        <v-card-title class="pa-4 font-weight-black border-b bg-grey-lighten-4 text-primary">{{ isEditMode ? 'Cập nhật nhân viên' : 'Thêm nhân viên mới' }}</v-card-title>
        <v-card-text class="pa-6">
          <v-form>
            <v-row>
              <v-col cols="12" md="6"><v-text-field v-model="employeeForm.tenTaiKhoan" label="Tên đăng nhập" variant="outlined" class="font-weight-black" rounded="0"></v-text-field></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="employeeForm.matKhau" label="Mật khẩu" variant="outlined" type="password" class="font-weight-black" rounded="0"></v-text-field></v-col>
              <v-col cols="12"><v-text-field v-model="employeeForm.ten" label="Họ và tên" variant="outlined" class="font-weight-black" rounded="0"></v-text-field></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="employeeForm.email" label="Email" variant="outlined" class="font-weight-black" rounded="0"></v-text-field></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="employeeForm.sdt" label="Số điện thoại" variant="outlined" class="font-weight-black" rounded="0"></v-text-field></v-col>
            </v-row>
          </v-form>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions class="pa-4 bg-grey-lighten-4">
          <v-spacer></v-spacer>
          <v-btn variant="text" class="text-none font-weight-black" @click="showEmployeeDialog = false">Hủy bỏ</v-btn>
          <v-btn color="primary" variant="flat" rounded="0" class="px-8 text-none font-weight-black" @click="confirmSaveEmployee">Xác nhận</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- SHARED CONFIRM -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message" :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #fdfdfd; } .text-dark { color: #1e293b !important; } .tight-switch { transform: scale(0.75); width: 44px; } .font-body { font-family: 'Public Sans', sans-serif; } :deep(.v-btn) { border-radius: 0 !important; }
</style>
