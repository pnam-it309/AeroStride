<script setup>
import { ref, onMounted } from 'vue';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { EditIcon, BoxIcon } from 'vue-tabler-icons';

const { addNotification } = useNotifications();

const loading = ref(false);
const isRefreshing = ref(false);
const products = ref([]);

// Pagination logic
const pagination = ref({
  page: 1,
  size: 10,
  totalElements: 0,
  totalPages: 1
});

const filters = ref({
  keyword: '',
  trangThai: null,
  danhMuc: null,
  thuongHieu: null
});

// Confirmation Logic
const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  color: 'primary',
  action: null,
  loading: false
});

const tableHeaders = [
  { text: 'Thông tin sản phẩm', align: 'start' },
  { text: 'Phân loại', align: 'center', width: '180px' },
  { text: 'Giá niêm yết', align: 'center', width: '180px' },
  { text: 'Trạng thái', align: 'center', width: '150px' },
  { text: 'Thao tác', align: 'center', width: '120px' }
];

const loadProducts = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      size: pagination.value.size,
      keyword: filters.value.keyword || null,
      trangThai: filters.value.trangThai || null,
      danhMucId: filters.value.danhMuc || null,
      thuongHieuId: filters.value.thuongHieu || null
    };
    const response = await dichVuSanPham.layDanhSachSanPham(params);
    products.value = response.content || response;
    pagination.value.totalElements = response.totalElements || products.value.length;
    pagination.value.totalPages = response.totalPages || 1;
  } catch (error) {
    console.error('Error loading products:', error);
  } finally {
    loading.value = false;
  }
};

const handleRefresh = async () => {
  isRefreshing.value = true;
  filters.value = { keyword: '', trangThai: null, danhMuc: null, thuongHieu: null };
  pagination.value.page = 1;
  await loadProducts();
  setTimeout(() => { isRefreshing.value = false; }, 800);
};

const confirmToggleStatus = (product) => {
  confirmDialog.value = {
    show: true,
    title: 'Xác nhận trạng thái',
    message: `Bạn có chắc chắn muốn thay đổi trạng thái sản phẩm [${product.tenSanPham}]?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        const newS = product.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
        await dichVuSanPham.thayDoiTrangThaiSanPham(product.id, newS);
        product.trangThai = newS;
        addNotification({
          title: 'Cập nhật trạng thái',
          subtitle: `Sản phẩm [${product.tenSanPham}] đã chuyển sang ${newS === 'DANG_HOAT_DONG' ? 'Đang hoạt động' : 'Ngừng hoạt động'}`,
          icon: 'InfoCircleIcon',
          color: 'primary'
        });
        confirmDialog.value.show = false;
      } catch (e) { console.error(e); } finally { confirmDialog.value.loading = false; }
    }
  };
};

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

onMounted(() => loadProducts());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <v-row class="mb-4">
      <v-col cols="12">
        <h2 class="text-h4 font-weight-bold text-dark mb-1">Quản lý sản phẩm</h2>
        <div class="text-subtitle-1 text-medium-emphasis">Hệ thống quản lý chuỗi cung ứng AeroStride</div>
      </v-col>
    </v-row>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="4">
        <v-text-field
          v-model="filters.keyword"
          placeholder="Tên, SKU, ID..."
          variant="outlined" density="comfortable" hide-details prepend-inner-icon="mdi-magnify"
          class="font-weight-black" rounded="md"
          @keyup.enter="loadProducts"
        ></v-text-field>
      </v-col>
      <v-col cols="12" md="3">
        <v-select
          v-model="filters.trangThai"
          :items="[{title:'Tất cả trạng thái', value:null}, {title:'Đang bán', value:'DANG_HOAT_DONG'}, {title:'Ngừng bán', value:'KHONG_HOAT_DONG'}]"
          variant="outlined" density="comfortable" hide-details
          class="font-weight-black" rounded="md"
          @update:model-value="loadProducts"
        ></v-select>
      </v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable
      title="Danh sách sản phẩm"
      :headers="[
        { text: 'Thông tin sản phẩm', align: 'center' ,width: '200px'},
        { text: 'Phân loại', align: 'center', width: '180px' },
        { text: 'Giá niêm yết', align: 'center', width: '180px' },
        { text: 'Trạng thái', align: 'center', width: '150px' },
        { text: 'Thao tác', align: 'center', width: '120px' }
      ]"
      :items="products"
      :loading="loading"
      @add="() => {}"
    >
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell">
            <div class="d-flex align-center">
              <v-img :src="item.hinhAnh" width="60" height="60" class="mr-3 border shadow-sm flex-grow-0" cover></v-img>
              <div>
                <div class="text-subtitle-1 font-weight-black text-dark">{{ item.tenSanPham }}</div>
                <div class="text-caption font-weight-bold text-primary">#{{ item.id.toString().padStart(6, '0') }} - {{ item.tenThuongHieu }}</div>
              </div>
            </div>
          </td>

          <td class="data-cell" style="text-align: center;">
            <v-chip variant="flat" color="blue-grey-lighten-4" class="text-dark font-weight-black px-4" size="x-small">
              {{ item.tenDanhMuc }}
            </v-chip>
          </td>

          <td class="data-cell" style="text-align: center;">
            <div class="text-h6 font-weight-black text-primary">{{ formatCurrency(item.giaBanThapNhat || 0) }}</div>
          </td>

          <td class="data-cell" style="text-align: center;">
            <v-chip 
              :color="item.trangThai === 'DANG_HOAT_DONG' ? 'success' : 'error'"
              size="x-small"
              variant="flat"
              class="font-weight-black px-4"
            >
              {{ item.trangThai === 'DANG_HOAT_DONG' ? 'Đang bán' : 'Ngừng bán' }}
            </v-chip>
          </td>

          <td class="data-cell" style="text-align: center;">
            <div class="d-flex align-center justify-center">
              <v-switch
                :model-value="item.trangThai === 'DANG_HOAT_DONG'"
                color="success" inset hide-details density="compact"
                class="tight-switch"
                @click.stop="confirmToggleStatus(item)"
              ></v-switch>
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-0" @click.stop="">
                <EditIcon size="18" />
              </v-btn>
            </div>
          </td>
        </tr>
      </template>

      <template #pagination>
        <AdminPagination
          v-model="pagination.page"
          :page-size="pagination.size"
          @update:pageSize="pagination.size = $event"
          @update:page-size="pagination.size = $event"
          :total-pages="pagination.totalPages"
          :total-elements="pagination.totalElements"
          :current-size="products.length"
          @change="loadProducts"
        />
      </template>
    </AdminTable>

    <!-- SHARED CONFIRM DIALOG -->
    <AdminConfirm
      v-model:show="confirmDialog.show"
      :title="confirmDialog.title"
      :message="confirmDialog.message"
      :color="confirmDialog.color"
      :loading="confirmDialog.loading"
      @confirm="confirmDialog.action"
    />
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #fcfcfc; } /* Brighter for high density */
.text-dark {
  color: #000000 !important;
}
.font-body { font-family: 'Public Sans', sans-serif; }
.tight-switch { transform: scale(0.75); width: 44px; }

/* Global Style Override for square look */
:deep(.v-btn) { border-radius: 0 !important; }
</style>
