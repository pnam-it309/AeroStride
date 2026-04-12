<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, TicketIcon, EyeIcon } from 'vue-tabler-icons';

const router = useRouter();
const loading = ref(false);
const isRefreshing = ref(false);
const vouchers = ref([]);

const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ keyword: '', loaiPhieu: null });

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const loadVouchers = async () => {
  loading.value = true;
  try {
    const params = {
      pageNo: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      pageSize: pagination.value.size,
      keyword: filters.value.keyword || null,
      loaiPhieu: filters.value.loaiPhieu || null
    };
    const response = await dichVuPhieuGiamGia.layPhieuGiamGiaPhanTrang(params);
    vouchers.value = response.content || response;
    pagination.value.totalElements = response.totalElements || vouchers.value.length;
    pagination.value.totalPages = response.totalPages || 1;
  } catch (error) { console.error(error); } finally { loading.value = false; }
};

const handleRefresh = async () => {
  isRefreshing.value = true; filters.value = { keyword: '', loaiPhieu: null };
  pagination.value.page = 1; await loadVouchers();
  setTimeout(() => isRefreshing.value = false, 800);
};

const handleExport = async () => {
  try {
    const blob = await dichVuPhieuGiamGia.xuatExcelPhieuGiamGia();
    downloadFile(blob, 'danh_sach_phieu_giam_gia.xlsx');
  } catch (error) {
    console.error('Lỗi xuất Excel:', error);
  }
};

const confirmToggleStatus = (item) => {
  confirmDialog.value = {
    show: true, title: 'Thay đổi trạng thái',
    message: `Bạn có muốn đổi trạng thái của Voucher [${item.ma}]?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
        await dichVuPhieuGiamGia.thayDoiTrangThaiPhieuGiamGia(item.id, newS);
        item.trangThai = newS;
        confirmDialog.value.show = false;
      } catch (e) {} finally { confirmDialog.value.loading = false; }
    }
  };
};

const editVoucher = (v) => { router.push(`/phieu-giam-gia/form/${v.id}`); };
const openCreateDialog = () => { router.push({ name: 'PhieuGiamGiaForm' }); };
const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
const getDiscountDisplay = (v) => v.loaiPhieu === 'PHAN_TRAM' ? { val: `${v.phanTramGiamGia || 0}%`, color: 'indigo' } : { val: formatCurrency(v.soTienGiam || 0), color: 'emerald' };
const getVoucherStatusLabel = (v) => { const now = new Date().getTime(); if (now < v.ngayBatDau) return { text: 'Chưa diễn ra', color: 'amber' }; if (now > v.ngayKetThuc) return { text: 'Hết hạn', color: 'grey' }; return { text: 'Đang hoạt động', color: 'success' }; };

onMounted(() => loadVouchers());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Phiếu giảm giá</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Danh sách mã voucher và phiếu ưu đãi khách hàng</p>
      </div>
    </div>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="4"><v-text-field v-model="filters.keyword" label="Tìm kiếm phiếu giảm giá" placeholder="Mã hoặc tên phiếu..." persistent-placeholder variant="outlined" density="comfortable" hide-details prepend-inner-icon="mdi-magnify" class="font-weight-medium" @keyup.enter="loadVouchers"></v-text-field></v-col>
      <v-col cols="12" md="3"><v-select v-model="filters.loaiPhieu" label="Loại voucher" :items="[{title:'Tất cả loại',value:null}, {title:'Giảm theo %',value:'PHAN_TRAM'}, {title:'Giảm theo VNĐ',value:'TIEN_MAT'}]" variant="outlined" density="comfortable" hide-details class="font-weight-medium" @update:model-value="loadVouchers"></v-select></v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable
      title="Danh sách voucher"
      addButtonText="Thêm phiếu"
      show-export-button
      :headers="[{text:'Mã & Tên phiếu', align:'center', width:'250px'}, {text:'Giá trị giảm', align:'center', width:'150px'}, {text:'Thời gian', align:'center', width:'200px'}, {text:'Đơn tối thiểu', align:'center', width:'150px'}, {text:'Sử dụng', align:'center', width:'150px'}, {text:'Trạng thái', align:'center', width:'150px'}, {text:'Thao tác', align:'center', width:'120px'}]"
      :items="vouchers"
      :total-count="pagination.totalElements"
      :loading="loading"
      @add="openCreateDialog"
      @export="handleExport"
    >
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell">
            <div class="d-flex align-center justify-center gap-2">
              <div class="voucher-stub mr-3"><TicketIcon size="24" class="text-primary" /></div>
              <div class="text-left">
                <div class="text-subtitle-1 font-weight-bold text-primary">{{ item.ma }}</div>
                <div class="text-caption font-weight-medium text-dark">{{ item.ten }}</div>
              </div>
            </div>
          </td>
          <td class="data-cell" style="text-align: center;"><v-chip :color="getDiscountDisplay(item).color" variant="flat"  class="font-weight-bold px-4">{{ getDiscountDisplay(item).val }}</v-chip></td>
          <td class="data-cell" style="text-align: center;">
            <div class="text-caption font-weight-bold text-dark">{{ new Date(item.ngayBatDau).toLocaleDateString('vi-VN') }}</div>
            <div class="text-caption font-weight-bold text-grey">đến</div>
            <div class="text-caption font-weight-bold text-dark">{{ new Date(item.ngayKetThuc).toLocaleDateString('vi-VN') }}</div>
          </td>
          <td class="data-cell" style="text-align: center;">
            <div class="text-subtitle-2 font-weight-bold text-primary">{{ formatCurrency(item.donHangToiThieu || 0) }}</div>
          </td>
          <td class="data-cell" style="text-align: center;"><div class="text-subtitle-2 font-weight-bold text-dark">{{ item.usedCount || 0 }} / <span class="text-grey">{{ item.soLuong || '∞' }}</span></div><v-progress-linear :model-value="item.soLuong ? (item.usedCount / item.soLuong)*100 : 0" color="primary" height="4" class="mt-1" rounded="0"></v-progress-linear></td>
          <td class="data-cell" style="text-align: center;"><v-chip :color="getVoucherStatusLabel(item).color"  variant="flat" class="font-weight-bold px-4">{{ getVoucherStatusLabel(item).text }}</v-chip></td>
          <td class="data-cell" style="text-align: center;">
            <div class="d-flex align-center justify-center gap-2">
              <v-btn icon variant="tonal" size="32" color="info" class="rounded-lg" @click.stop="router.push({ name: 'PhieuGiamGiaDetail', params: { id: item.id } })">
                <EyeIcon size="18" />
                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
              </v-btn>
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-lg" @click.stop="router.push({ name: 'PhieuGiamGiaForm', params: { id: item.id } })">
                <EditIcon size="18" />
                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
              </v-btn>
              <v-switch :model-value="item.trangThai === 'DANG_HOAT_DONG'" color="success" inset hide-details density="compact" :readonly="item.trangThai !== 'DANG_HOAT_DONG'" class="tight-switch" @click.stop="item.trangThai === 'DANG_HOAT_DONG' ? confirmToggleStatus(item) : null">
                <v-tooltip activator="parent" location="top">Đổi trạng thái</v-tooltip>
              </v-switch>
            </div>
          </td>
        </tr>
      </template>
      <template #pagination>
        <AdminPagination v-model="pagination.page" v-model:page-size="pagination.size" :total-pages="pagination.totalPages" :total-elements="pagination.totalElements" :current-size="vouchers.length" @change="loadVouchers" />
      </template>
    </AdminTable>

    <!-- SHARED CONFIRM -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message" :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #fdfdfd; } .text-dark { color: #1e293b !important; } .tight-switch { transform: scale(0.75); width: 44px; } .font-body { font-family: 'Public Sans', sans-serif; } .voucher-stub { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; background: #e0e7ff; border-radius: 0; border: 1px dashed #4f46e5; } :deep(.v-btn) { border-radius: 3px !important; }
</style>

