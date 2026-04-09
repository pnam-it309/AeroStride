<script setup>
import { ref, onMounted } from 'vue';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { EditIcon, TicketIcon } from 'vue-tabler-icons';

const loading = ref(false);
const isRefreshing = ref(false);
const vouchers = ref([]);
const showVoucherDialog = ref(false);
const selectedVoucher = ref(null);
const isEditMode = ref(false);

const voucherForm = ref({ code: '', name: '', description: '', discountType: 'percentage', discountValue: '', usageLimit: '', usedCount: 0, startDate: '', endDate: '', status: 'active' });
const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ keyword: '', loaiPhieu: null });

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const loadVouchers = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      size: pagination.value.size,
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

const confirmToggleStatus = (item) => {
  confirmDialog.value = {
    show: true, title: 'Thay đổi trạng thái',
    message: `Bạn có muốn đổi trạng thái của Voucher [${item.ma}]?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        const newS = (item.trangThai === 'DANG_HOAT_DONG' || item.status === 'active') ? 'inactive' : 'active';
        await dichVuPhieuGiamGia.thayDoiTrangThaiPhieuGiamGia(item.id, newS);
        item.trangThai = newS === 'active' ? 'DANG_HOAT_DONG' : 'KHONG_HOAT_DONG';
        item.status = newS; confirmDialog.value.show = false;
      } catch (e) {} finally { confirmDialog.value.loading = false; }
    }
  };
};

const confirmSaveVoucher = () => {
  confirmDialog.value = {
    show: true, title: isEditMode.value ? 'Cập nhật khách hàng' : 'Thêm khách hàng',
    message: `Bạn có chắc muốn lưu phiếu giảm giá [${voucherForm.value.code}]?`,
    color: 'success',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        if (isEditMode.value) await updateVoucher(); else await createVoucher();
        confirmDialog.value.show = false;
      } catch (e) {} finally { confirmDialog.value.loading = false; }
    }
  };
};

const updateVoucher = async () => {
  const updated = await dichVuPhieuGiamGia.capNhatPhieuGiamGia(selectedVoucher.value.id, voucherForm.value);
  const idx = vouchers.value.findIndex(v => v.id === selectedVoucher.value.id);
  if (idx !== -1) vouchers.value[idx] = updated; showVoucherDialog.value = false;
};

const createVoucher = async () => {
  const created = await dichVuPhieuGiamGia.taoPhieuGiamGia(voucherForm.value);
  vouchers.value.unshift(created); showVoucherDialog.value = false; loadVouchers();
};

const editVoucher = (v) => { selectedVoucher.value = v; voucherForm.value = { ...v }; isEditMode.value = true; showVoucherDialog.value = true; };
const openCreateDialog = () => { voucherForm.value = { code: '', name: '', description: '', discountType: 'percentage', discountValue: '', usageLimit: '', usedCount: 0, startDate: '', endDate: '', status: 'active' }; isEditMode.value = false; showVoucherDialog.value = true; };
const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
const getDiscountDisplay = (v) => v.loaiPhieu === 'PHAN_TRAM' ? { val: `${v.phanTramGiamGia || 0}%`, color: 'indigo' } : { val: formatCurrency(v.soTienGiam || 0), color: 'emerald' };
const getVoucherStatusLabel = (v) => { const now = new Date().getTime(); if (now < v.ngayBatDau) return { text: 'Chưa diễn ra', color: 'amber' }; if (now > v.ngayKetThuc) return { text: 'Hết hạn', color: 'grey' }; return { text: 'Đang hoạt động', color: 'success' }; };

onMounted(() => loadVouchers());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <v-row class="mb-4">
      <v-col cols="12">
        <h2 class="text-h4 font-weight-bold text-dark mb-1">Quản lý phiếu giảm giá</h2>
        <div class="text-subtitle-1 text-medium-emphasis">Hệ thống khuyến mãi và mã ưu đãi AeroStride</div>
      </v-col>
    </v-row>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="4"><v-text-field v-model="filters.keyword" placeholder="Mã hoặc tên phiếu..." variant="outlined" density="comfortable" hide-details prepend-inner-icon="mdi-magnify" class="font-weight-medium" @keyup.enter="loadVouchers"></v-text-field></v-col>
      <v-col cols="12" md="3"><v-select v-model="filters.loaiPhieu" :items="[{title:'Tất cả loại',value:null}, {title:'Giảm theo %',value:'PHAN_TRAM'}, {title:'Giảm theo VNĐ',value:'TIEN_MAT'}]" variant="outlined" density="comfortable" hide-details class="font-weight-medium" @update:model-value="loadVouchers"></v-select></v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable title="Danh sách voucher" addButtonText="Thêm phiếu" :headers="[{text:'Mã & Tên phiếu', align:'center'}, {text:'Giá trị giảm', align:'center'}, {text:'Sử dụng / Giới hạn', align:'center'}, {text:'Trạng thái', align:'center'}, {text:'Thao tác', align:'center'}]" :items="vouchers" :total-count="pagination.totalElements" :loading="loading" @add="openCreateDialog">
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell">
            <div class="d-flex align-center justify-center">
              <div class="voucher-stub mr-3"><TicketIcon size="24" class="text-primary" /></div>
              <div class="text-left">
                <div class="text-subtitle-1 font-weight-bold text-primary">{{ item.ma }}</div>
                <div class="text-caption font-weight-medium text-dark">{{ item.ten }}</div>
              </div>
            </div>
          </td>
          <td class="data-cell" style="text-align: center;"><v-chip :color="getDiscountDisplay(item).color" variant="flat" size="small" class="font-weight-bold px-4">{{ getDiscountDisplay(item).val }}</v-chip></td>
          <td class="data-cell" style="text-align: center;"><div class="text-subtitle-2 font-weight-bold text-dark">{{ item.usedCount || 0 }} / <span class="text-grey">{{ item.soLuong || '∞' }}</span></div><v-progress-linear :model-value="item.soLuong ? (item.usedCount / item.soLuong)*100 : 0" color="primary" height="4" class="mt-1" rounded="0"></v-progress-linear></td>
          <td class="data-cell" style="text-align: center;"><v-chip :color="getVoucherStatusLabel(item).color" size="x-small" variant="flat" class="font-weight-bold px-4">{{ getVoucherStatusLabel(item).text }}</v-chip></td>
          <td class="data-cell" style="text-align: center;">
            <div class="d-flex align-center justify-center">
              <v-switch :model-value="item.trangThai === 'DANG_HOAT_DONG' || item.status === 'active'" color="success" inset hide-details density="compact" class="tight-switch" @click.stop="confirmToggleStatus(item)"></v-switch>
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-0" @click.stop="editVoucher(item)"><EditIcon size="18" /></v-btn>
            </div>
          </td>
        </tr>
      </template>
      <template #pagination>
        <AdminPagination v-model="pagination.page" v-model:page-size="pagination.size" :total-pages="pagination.totalPages" :total-elements="pagination.totalElements" :current-size="vouchers.length" @change="loadVouchers" />
      </template>
    </AdminTable>

    <!-- Dialog (SQUARE) -->
    <v-dialog v-model="showVoucherDialog" max-width="700">
      <v-card class="rounded-0 border shadow-2xl">
        <v-card-title class="pa-4 font-weight-bold border-b bg-grey-lighten-4 text-primary">{{ isEditMode ? 'Cập nhật phiếu' : 'Tạo voucher mới' }}</v-card-title>
        <v-card-text class="pa-6">
          <v-form>
            <v-row>
              <v-col cols="12" md="6"><v-text-field v-model="voucherForm.code" label="Mã voucher" variant="outlined" class="font-weight-medium" rounded="0"></v-text-field></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="voucherForm.name" label="Tên phiếu" variant="outlined" class="font-weight-medium" rounded="0"></v-text-field></v-col>
              <v-col cols="12" md="6"><v-select v-model="voucherForm.discountType" label="Loại ưu đãi" :items="[{title:'Phần trăm (%)',value:'percentage'},{title:'Số tiền mặt (VNĐ)',value:'fixed'}]" variant="outlined" rounded="0"></v-select></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="voucherForm.discountValue" label="Giá trị giảm" variant="outlined" type="number" class="font-weight-black" rounded="0"></v-text-field></v-col>
            </v-row>
          </v-form>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions class="pa-4 bg-grey-lighten-4">
          <v-spacer></v-spacer>
          <v-btn variant="text" class="text-none font-weight-black" @click="showVoucherDialog = false">Hủy bỏ</v-btn>
          <v-btn color="primary" variant="flat" rounded="0" class="px-8 text-none font-weight-black" @click="confirmSaveVoucher">Xác nhận</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- SHARED CONFIRM -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message" :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #fdfdfd; } .text-dark { color: #1e293b !important; } .tight-switch { transform: scale(0.75); width: 44px; } .font-body { font-family: 'Public Sans', sans-serif; } .voucher-stub { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; background: #e0e7ff; border-radius: 0; border: 1px dashed #4f46e5; } :deep(.v-btn) { border-radius: 0 !important; }
</style>
