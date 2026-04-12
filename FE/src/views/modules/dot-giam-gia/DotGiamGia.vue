<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, GiftIcon, EyeIcon } from 'vue-tabler-icons';

const router = useRouter();
const loading = ref(false);
const isRefreshing = ref(false);
const campaigns = ref([]);

const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ keyword: '', loaiGiamGia: null });

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const loadCampaigns = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      size: pagination.value.size,
      keyword: filters.value.keyword || null,
      loaiGiamGia: filters.value.loaiGiamGia || null
    };
    const response = await dichVuDotGiamGia.layDotGiamGiaPhanTrang(params);
    // Robust extraction: handle both direct array, Page object, and ApiResponse
    const data = response?.data?.content || response?.content || response?.data || response;
    campaigns.value = Array.isArray(data) ? data : [];
    
    pagination.value.totalElements = response?.data?.totalElements || response?.totalElements || campaigns.value.length;
    pagination.value.totalPages = response?.data?.totalPages || response?.totalPages || 1;
  } catch (error) { console.error(error); } finally { loading.value = false; }
};

const handleRefresh = async () => {
  isRefreshing.value = true; filters.value = { keyword: '', loaiGiamGia: null };
  pagination.value.page = 1; await loadCampaigns();
  setTimeout(() => isRefreshing.value = false, 800);
};

const handleExport = async () => {
  try {
    const blob = await dichVuDotGiamGia.xuatExcelDotGiamGia();
    downloadFile(blob, 'danh_sach_dot_giam_gia.xlsx');
  } catch (error) {
    console.error('Lỗi xuất Excel:', error);
  }
};

const confirmToggleStatus = (item) => {
  confirmDialog.value = {
    show: true, title: 'Thay đổi trạng thái',
    message: `Bạn có muốn đổi trạng thái của chiến dịch [${item.ten}]?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
        await dichVuDotGiamGia.thayDoiTrangThaiDotGiamGia(item.id, newS);
        item.trangThai = newS;
        confirmDialog.value.show = false;
      } catch (e) {} finally { confirmDialog.value.loading = false; }
    }
  };
};

const editCampaign = (c) => { router.push(`/dot-giam-gia/form/${c.id}`); };
const openCreateDialog = () => { router.push({ name: 'DotGiamGiaForm' }); };
const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
const formatDate = (timestamp) => timestamp ? new Date(timestamp).toLocaleDateString('vi-VN') : 'N/A';
const getDiscountDisplay = (c) => c.loaiGiamGia === 'PHAN_TRAM' ? { val: `${c.soTienGiam}%`, color: 'orange' } : { val: formatCurrency(c.soTienGiam), color: 'green' };
const getCampaignStatusLabel = (c) => { const now = new Date().getTime(); if (now < c.ngayBatDau) return { text: 'Sắp diễn ra', color: 'warning' }; if (now > c.ngayKetThuc) return { text: 'Đã kết thúc', color: 'grey' }; return { text: 'Đang diễn ra', color: 'success' }; };

onMounted(() => loadCampaigns());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Đợt giảm giá</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Chiến dịch ưu đãi và sự kiện khuyến mãi</p>
      </div>
    </div>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="4"><v-text-field v-model="filters.keyword" label="Tìm kiếm đợt giảm giá" placeholder="Nhập tên chiến dịch..." persistent-placeholder variant="outlined" density="comfortable" hide-details prepend-inner-icon="mdi-magnify" class="font-weight-medium" @keyup.enter="loadCampaigns"></v-text-field></v-col>
      <v-col cols="12" md="3"><v-select v-model="filters.loaiGiamGia" label="Loại giảm giá" :items="[{title:'Tất cả loại',value:null}, {title:'Giảm theo %',value:'PHAN_TRAM'}, {title:'Giảm theo số tiền',value:'TIEN_MAT'}]" variant="outlined" density="comfortable" hide-details class="font-weight-medium" @update:model-value="loadCampaigns"></v-select></v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable
      title="Danh sách chiến dịch"
      addButtonText="Thêm đợt giảm giá"
      show-export-button
      :headers="[{text:'Mã đợt', align:'center', width:'150px'}, {text:'Thông tin chiến dịch', align:'center'}, {text:'Giá trị giảm', align:'center'}, {text:'Thời gian', align:'center'}, {text:'Trạng thái', align:'center'}, {text:'Thao tác', align:'center'}]"
      :items="campaigns"
      :total-count="pagination.totalElements"
      :loading="loading"
      @add="openCreateDialog"
      @export="handleExport"
    >
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell" style="text-align: center;">
            <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold">{{ item.ma }}</v-chip>
          </td>
          <td class="data-cell">
            <div class="d-flex flex-column align-center justify-center">
              <div class="text-subtitle-1 font-weight-medium text-dark text-center">{{ item.ten }}</div>
              <div class="text-caption font-weight-bold text-medium-emphasis text-center line-clamp-1">{{ item.moTa || 'N/A' }}</div>
            </div>
          </td>
          <td class="data-cell"><v-chip :color="getDiscountDisplay(item).color" variant="flat"  class="font-weight-medium px-4">{{ getDiscountDisplay(item).val }}</v-chip></td>
          <td class="data-cell"><div class="text-caption font-weight-bold text-dark">{{ formatDate(item.ngayBatDau) }}</div><v-icon  color="grey">mdi-arrow-down-bold</v-icon><div class="text-caption font-weight-bold text-dark">{{ formatDate(item.ngayKetThuc) }}</div></td>
          <td class="data-cell"><v-chip :color="getCampaignStatusLabel(item).color"  variant="flat" class="font-weight-medium px-4">{{ getCampaignStatusLabel(item).text }}</v-chip></td>
          <td class="data-cell">
            <div class="d-flex align-center justify-center gap-2">
              <v-btn icon variant="tonal" size="32" color="info" class="rounded-lg" @click.stop="router.push({ name: 'DotGiamGiaDetail', params: { id: item.id } })">
                <EyeIcon size="18" />
                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
              </v-btn>
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-lg" @click.stop="router.push({ name: 'DotGiamGiaForm', params: { id: item.id } })">
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
        <AdminPagination v-model="pagination.page" v-model:page-size="pagination.size" :total-pages="pagination.totalPages" :total-elements="pagination.totalElements" :current-size="campaigns.length" @change="loadCampaigns" />
      </template>
    </AdminTable>

    <!-- SHARED CONFIRM -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message" :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #fdfdfd; } .text-dark { color: #1e293b !important; } .tight-switch { transform: scale(0.75); width: 44px; } .font-body { font-family: 'Public Sans', sans-serif; } .line-clamp-1 { display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; } :deep(.v-btn) { border-radius: 3px !important; }
</style>
