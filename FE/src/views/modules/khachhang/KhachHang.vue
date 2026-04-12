<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, UserCheckIcon, EyeIcon } from 'vue-tabler-icons';

const loading = ref(false);
const isRefreshing = ref(false);
const customers = ref([]);
const router = useRouter();

const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ keyword: '', trangThai: null, gioiTinh: null });

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const loadCustomers = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      size: pagination.value.size,
      keyword: filters.value.keyword || null,
      trangThai: filters.value.trangThai || null,
      gioiTinh: filters.value.gioiTinh !== null ? filters.value.gioiTinh : null
    };
    const response = await dichVuKhachHang.layKhachHangPhanTrang(params);
    customers.value = response.content || response;
    pagination.value.totalElements = response.totalElements || customers.value.length;
    pagination.value.totalPages = response.totalPages || 1;
  } catch (error) { console.error(error); } finally { loading.value = false; }
};

const handleRefresh = async () => {
  isRefreshing.value = true; filters.value = { keyword: '', trangThai: null, gioiTinh: null };
  pagination.value.page = 1; await loadCustomers();
  setTimeout(() => isRefreshing.value = false, 800);
};

const handleExport = async () => {
  try {
    const blob = await dichVuKhachHang.xuatExcelKhachHang();
    downloadFile(blob, 'danh_sach_khach_hang.xlsx');
  } catch (error) {
    console.error('Lỗi xuất Excel:', error);
  }
};

const confirmChangeStatus = (item) => {
  confirmDialog.value = {
    show: true, title: 'THAY ĐỔI TRẠNG THÁI',
    message: `Bạn có chắc muốn đổi trạng thái của khách hàng [${item.ten}]?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
        await dichVuKhachHang.thayDoiTrangThaiKhachHang(item.id, newS);
        item.trangThai = newS; confirmDialog.value.show = false;
      } catch (e) {} finally { confirmDialog.value.loading = false; }
    }
  };
};

onMounted(() => loadCustomers());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen">
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Quản lý khách hàng</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Thông tin khách hàng và lịch sử mua sắm</p>
      </div>
    </div>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="3">
        <v-text-field v-model="filters.keyword" label="Tìm kiếm khách hàng" placeholder="Tên, SĐT, Email..." persistent-placeholder variant="outlined" density="comfortable" hide-details prepend-inner-icon="mdi-magnify" class="font-weight-medium" @keyup.enter="loadCustomers"></v-text-field>
      </v-col>
      <v-col cols="12" md="3">
        <v-select v-model="filters.trangThai" label="Trạng thái" :items="[{title:'Tất cả trạng thái',value:null}, {title:'Hoạt động',value:'DANG_HOAT_DONG'}, {title:'Ngừng hoạt động',value:'KHONG_HOAT_DONG'}]" variant="outlined" density="comfortable" hide-details class="font-weight-medium" @update:model-value="loadCustomers"></v-select>
      </v-col>
      <v-col cols="12" md="3">
        <v-select v-model="filters.gioiTinh" label="Giới tính" :items="[{title:'Tất cả giới tính',value:null}, {title:'Nam',value:true}, {title:'Nữ',value:false}]" variant="outlined" density="comfortable" hide-details class="font-weight-medium" @update:model-value="loadCustomers"></v-select>
      </v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable
      title="Danh sách khách hàng"
      addButtonText="Thêm khách hàng"
      show-export-button
      :headers="[{text:'Mã KH', align:'center', width:'120px'}, {text:'Thông tin khách hàng', align:'center', width:'250px'}, {text:'Liên hệ', align:'center', width:'220px'}, {text:'Ngày sinh', align:'center', width:'130px'}, {text:'Địa chỉ', align:'center', width:'250px'}, {text:'Giới tính', align:'center', width:'100px'}, {text:'Trạng thái', align:'center', width:'150px'}, {text:'Thao tác', align:'center', width:'100px'}]"
      :items="customers"
      :total-count="pagination.totalElements"
      :loading="loading"
      @add="router.push({ name: 'KhachHangForm' })"
      @export="handleExport"
    >
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell" style="text-align: center;">
            <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold">{{ item.ma }}</v-chip>
          </td>
          <td class="data-cell">
            <div class="d-flex align-center justify-center gap-6">
              <v-avatar color="primary-lighten-4" size="44" class="mr-3 font-weight-medium border rounded-lg">{{ item.ten.charAt(0).toUpperCase() }}</v-avatar>
              <div class="text-left">
                <div class="text-subtitle-1 font-weight-medium text-dark">{{ item.ten }}</div>
              </div>
            </div>
          </td>
          <td class="data-cell">
            <div class="text-subtitle-2 font-weight-bold text-dark mb-1">{{ item.email || '-' }}</div>
            <div class="text-caption font-weight-bold text-medium-emphasis font-weight-medium d-flex align-center justify-center"><v-icon  class="mr-1">mdi-phone</v-icon> {{ item.sdt }}</div>
          </td>
          <td class="data-cell" style="text-align: center;">
            <div class="text-subtitle-2 font-weight-medium">{{ item.ngaySinh || '-' }}</div>
          </td>
          <td class="data-cell">
            <div class="text-caption font-weight-medium text-dark line-clamp-2" :title="item.diaChiChiTiet">{{ item.diaChiChiTiet || 'Chưa cập nhật' }}</div>
          </td>
          <td class="data-cell" style="text-align: center;"><v-chip variant="flat" :color="item.gioiTinh ? 'blue-lighten-4' : 'pink-lighten-4'" class="font-weight-bold px-4 text-button">{{ item.gioiTinh ? 'Nam' : 'Nữ' }}</v-chip></td>
          <td class="data-cell" style="text-align: center;"><v-chip :color="item.trangThai === 'DANG_HOAT_DONG' ? 'success' : 'error'" variant="flat" class="font-weight-bold px-4 text-button">{{ item.trangThai === 'DANG_HOAT_DONG' ? 'Hoạt động' : 'Ngừng hoạt động' }}</v-chip></td>
          <td class="data-cell" style="text-align: center;">
            <div class="d-flex align-center justify-center gap-2">
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-0" @click.stop="router.push({ name: 'KhachHangForm', params: { id: item.id } })">
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
        <AdminPagination v-model="pagination.page" v-model:page-size="pagination.size" :total-pages="pagination.totalPages" :total-elements="pagination.totalElements" :current-size="customers.length" @change="loadCustomers" />
      </template>
    </AdminTable>

    <!-- SHARED CONFIRM -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message" :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #fdfdfd; } .text-dark { color: #1e293b !important; } .tight-switch { transform: scale(0.75); width: 44px; } :deep(.v-btn) { border-radius: 3px !important; }
</style>

<style scoped>
.gray-bg { background-color: #fdfdfd; } .text-dark { color: #1e293b !important; } .tight-switch { transform: scale(0.75); width: 44px; } :deep(.v-btn) { border-radius: 3px !important; }
</style>
