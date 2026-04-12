<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, UsersIcon, EyeIcon } from 'vue-tabler-icons';

const loading = ref(false);
const isRefreshing = ref(false);
const employees = ref([]);
const router = useRouter();

const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ keyword: '', trangThai: null, gioiTinh: null });

// Confirmation Logic
const confirmDialog = ref({
  show: false, title: '', message: '', color: 'primary', action: null, loading: false
});

const tableHeaders = [
  { text: 'Mã NV', align: 'center', width: '120px' },
  { text: 'Thông tin nhân viên', align: 'center' , width: '220px' },
  { text: 'Chức vụ', align: 'center', width: '120px' },
  { text: 'Tài khoản', align: 'center', width: '140px' },
  { text: 'Liên hệ', align: 'center', width: '220px' },
  { text: 'Ngày sinh', align: 'center', width: '130px' },
  { text: 'Giới tính', align: 'center', width: '100px' },
  { text: 'Trạng thái', align: 'center', width: '150px' },
  { text: 'Thao tác', align: 'center', width: '100px' }
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

const handleExport = async () => {
  try {
    const blob = await dichVuNhanVien.xuatExcelNhanVien();
    downloadFile(blob, 'danh_sach_nhan_vien.xlsx');
  } catch (error) {
    console.error('Lỗi xuất Excel:', error);
  }
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

onMounted(() => loadEmployees());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Quản lý nhân viên</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Hồ sơ nhân sự và phân quyền hệ thống</p>
      </div>
    </div>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="3">
        <v-text-field v-model="filters.keyword" label="Tìm kiếm nhân viên" placeholder="Tên, SĐT, Email..." persistent-placeholder variant="outlined" density="comfortable" hide-details prepend-inner-icon="mdi-magnify" class="font-weight-medium" @keyup.enter="loadEmployees"></v-text-field>
      </v-col>
      <v-col cols="12" md="3">
        <v-select v-model="filters.trangThai" label="Trạng thái" :items="[{title:'Tất cả trạng thái',value:null}, {title:'Đang làm việc',value:'DANG_HOAT_DONG'}, {title:'Đã nghỉ việc',value:'KHONG_HOAT_DONG'}]" variant="outlined" density="comfortable" hide-details class="font-weight-medium" @update:model-value="loadEmployees"></v-select>
      </v-col>
      <v-col cols="12" md="3">
        <v-select v-model="filters.gioiTinh" label="Giới tính" :items="[{title:'Tất cả giới tính',value:null}, {title:'Nam',value:true}, {title:'Nữ',value:false}]" variant="outlined" density="comfortable" hide-details class="font-weight-medium" @update:model-value="loadEmployees"></v-select>
      </v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable
      title="Danh sách nhân viên"
      addButtonText="Thêm nhân viên"
      show-export-button
      :headers="tableHeaders"
      :items="employees"
      :total-count="pagination.totalElements"
      :loading="loading"
      @add="router.push('/nhan-vien/form')"
      @export="handleExport"
    >
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell" style="text-align: center;">
            <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold">{{ item.ma }}</v-chip>
          </td>
          <td class="data-cell">
            <div class="d-flex align-center gap-6">
              <v-avatar color="primary-lighten-4" size="44" class="mr-3 font-weight-medium border rounded-lg font-weight-bold">{{ item.ten.charAt(0).toUpperCase() }}</v-avatar>
              <div class="text-left">
                <div class="text-subtitle-1 font-weight-bold text-dark">{{ item.ten }}</div>
              </div>
            </div>
          </td>
          <td class="data-cell">
            <v-chip size="small" color="primary" variant="tonal" class="font-weight-bold">{{ item.tenPhanQuyen || 'Nhân viên' }}</v-chip>
          </td>
          <td class="data-cell">
            <div class="text-subtitle-2 font-weight-black text-primary">{{ item.tenTaiKhoan }}</div>
          </td>
          <td class="data-cell">
            <div class="text-subtitle-2 font-weight-bold text-dark mb-1">{{ item.email }}</div>
            <div class="text-caption font-weight-bold text-secondary d-flex align-center">
              <v-icon size="14" class="mr-1">mdi-phone</v-icon> {{ item.sdt }}
            </div>
          </td>
          <td class="data-cell">
            <div class="text-subtitle-2 font-weight-medium">{{ item.ngaySinh || '-' }}</div>
          </td>
          <td class="data-cell" style="text-align: center;">
            <v-chip variant="flat" :color="item.gioiTinh ? 'blue-lighten-4' : 'pink-lighten-4'" class="font-weight-bold px-4" size="small">{{ item.gioiTinh ? 'Nam' : 'Nữ' }}</v-chip>
          </td>
          <td class="data-cell" style="text-align: center;">
            <v-chip :color="item.trangThai === 'DANG_HOAT_DONG' ? 'success' : 'error'" variant="flat" class="font-weight-bold px-4" size="small">{{ item.trangThai === 'DANG_HOAT_DONG' ? 'Đang làm' : 'Nghỉ việc' }}</v-chip>
          </td>
          <td class="data-cell" style="text-align: center;">
            <div class="d-flex align-center justify-center gap-2">
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-0" @click.stop="router.push(`/nhan-vien/form/${item.id}`)">
                <EditIcon size="18" />
                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
              </v-btn>
              <v-switch :model-value="item.trangThai === 'DANG_HOAT_DONG'" color="success" inset hide-details density="compact" :readonly="item.trangThai !== 'DANG_HOAT_DONG'" class="tight-switch ml-2" @click.stop="item.trangThai === 'DANG_HOAT_DONG' ? confirmChangeStatus(item) : null">
                <v-tooltip activator="parent" location="top">Đổi trạng thái</v-tooltip>
              </v-switch>
            </div>
          </td>
        </tr>
      </template>
      <template #pagination>
        <AdminPagination v-model="pagination.page" v-model:page-size="pagination.size" :total-pages="pagination.totalPages" :total-elements="pagination.totalElements" :current-size="employees.length" @change="loadEmployees" />
      </template>
    </AdminTable>

    <!-- SHARED CONFIRM -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message" :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #fdfdfd; } .text-dark { color: #1e293b !important; } .tight-switch { transform: scale(0.75); width: 44px; } :deep(.v-btn) { border-radius: 3px !important; }
</style>
