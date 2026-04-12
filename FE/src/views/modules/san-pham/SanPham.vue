<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, BoxIcon, EyeIcon } from 'vue-tabler-icons';

const { addNotification } = useNotifications();
const router = useRouter();

const loading = ref(false);
const importing = ref(false);
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
  thuongHieu: null,
  gioiTinh: null,
  xuatXu: null,
  mucDichChay: null,
  chatLieu: null
});

const filterOptions = ref({
  danhMucs: [],
  thuongHieus: [],
  xuatXus: [],
  mucDichChays: [],
  chatLieus: [],
  coGiays: [],
  deGiays: [],
  gioiTinhKhachHangs: ['NAM', 'NU', 'TRE_EM', 'UNISEX']
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
  { text: 'Thông tin sản phẩm', align: 'center' },
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
      thuongHieuId: filters.value.thuongHieu || null,
      gioiTinhKhachHang: filters.value.gioiTinh || null,
      xuatXuId: filters.value.xuatXu || null,
      mucDichChayId: filters.value.mucDichChay || null,
      chatLieuId: filters.value.chatLieu || null
    };
    const response = await dichVuSanPham.layDanhSachSanPham(params);
    // Robust extraction
    const data = response?.data?.content || response?.content || response?.data || response;
    products.value = Array.isArray(data) ? data : [];
    
    pagination.value.totalElements = response?.data?.totalElements || response?.totalElements || products.value.length;
    pagination.value.totalPages = response?.data?.totalPages || response?.totalPages || 1;
  } catch (error) {
    console.error('Error loading products:', error);
  } finally {
    loading.value = false;
  }
};

const handleRefresh = async () => {
  isRefreshing.value = true;
  filters.value = { keyword: '', trangThai: null, danhMuc: null, thuongHieu: null, gioiTinh: null, xuatXu: null, mucDichChay: null, chatLieu: null };
  pagination.value.page = 1;
  await loadProducts();
  setTimeout(() => { isRefreshing.value = false; }, 800);
};

const loadFilterOptions = async () => {
  try {
    const options = await dichVuSanPham.layOptionsForm();
    if (options) {
      filterOptions.value.danhMucs = options.danhMucs || [];
      filterOptions.value.thuongHieus = options.thuongHieus || [];
      filterOptions.value.xuatXus = options.xuatXus || [];
      filterOptions.value.mucDichChays = options.mucDichChays || [];
      filterOptions.value.chatLieus = options.chatLieus || [];
      filterOptions.value.coGiays = options.coGiays || [];
      filterOptions.value.deGiays = options.deGiays || [];
    }
  } catch (error) {
    console.error('Lỗi tải options:', error);
  }
};

const handleExport = async () => {
  try {
    const blob = await dichVuSanPham.xuatExcelSanPham();
    downloadFile(blob, 'danh_sach_san_pham.xlsx');
  } catch (error) {
    console.error('Lỗi xuất Excel:', error);
  }
};

const handleDownloadTemplate = async () => {
  try {
    const blob = await dichVuSanPham.taiTemplateExcel();
    downloadFile(blob, 'template_nhap_san_pham.xlsx');
  } catch (error) {
    console.error('Lỗi tải template:', error);
    addNotification({ title: 'Lỗi', subtitle: 'Không thể tải template', color: 'error' });
  }
};

const handleImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  importing.value = true;
  try {
    await dichVuSanPham.nhapExcelSanPham(file);
    addNotification({ title: 'Thành công', subtitle: 'Đã nhập dữ liệu từ Excel', color: 'success' });
    loadProducts(); // Refresh list
  } catch (error) {
    console.error('Lỗi nhập Excel:', error);
    addNotification({ title: 'Lỗi', subtitle: 'Lỗi khi nhập dữ liệu Excel', color: 'error' });
  } finally {
    importing.value = false;
    event.target.value = ''; // Reset input
  }
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

const getCategoryColor = (name) => {
  if (!name) return 'grey';
  const n = name.toLowerCase();
  if (n.includes('chạy bộ')) return 'blue';
  if (n.includes('thể thao')) return 'orange';
  if (n.includes('sneaker')) return 'indigo';
  if (n.includes('thời trang')) return 'purple';
  if (n.includes('đá bóng')) return 'green';
  return 'blue-grey';
};

onMounted(() => {
  loadProducts();
  loadFilterOptions();
});
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Quản lý sản phẩm</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Quản lý danh mục hàng hóa và biến thể sản phẩm</p>
      </div>
    </div>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <!-- Search -->
      <v-col cols="12" md="3">
        <v-text-field
          v-model="filters.keyword"
          label="Tìm kiếm sản phẩm"
          placeholder="Tên, SKU, ID..."
          persistent-placeholder
          variant="outlined" density="comfortable" hide-details prepend-inner-icon="mdi-magnify"
          class="font-weight-medium" rounded="md"
          @keyup.enter="loadProducts"
        ></v-text-field>
      </v-col>

      <!-- Danh Mục -->
      <v-col cols="12" md="2">
        <v-select
          v-model="filters.danhMuc"
          label="Danh mục"
          :items="[{title:'Tất cả danh mục', value:null}, ...filterOptions.danhMucs.map(d => ({title: d.ten, value: d.id}))]"
          variant="outlined" density="comfortable" hide-details
          class="font-weight-medium" rounded="md"
          @update:model-value="loadProducts"
        ></v-select>
      </v-col>

      <!-- Thương Hiệu -->
      <v-col cols="12" md="2">
        <v-select
          v-model="filters.thuongHieu"
          label="Thương hiệu"
          :items="[{title:'Tất cả thương hiệu', value:null}, ...filterOptions.thuongHieus.map(t => ({title: t.ten, value: t.id}))]"
          variant="outlined" density="comfortable" hide-details
          class="font-weight-medium" rounded="md"
          @update:model-value="loadProducts"
        ></v-select>
      </v-col>

      <!-- Giới Tính -->
      <v-col cols="12" md="2">
        <v-select
          v-model="filters.gioiTinh"
          label="Giới tính"
          :items="[{title:'Tất cả', value:null}, {title:'Nam', value:'NAM'}, {title:'Nữ', value:'NU'}, {title:'Trẻ em', value:'TRE_EM'}, {title:'Unisex', value:'UNISEX'}]"
          variant="outlined" density="comfortable" hide-details
          class="font-weight-medium" rounded="md"
          @update:model-value="loadProducts"
        ></v-select>
      </v-col>

      <!-- Trạng Thái -->
      <v-col cols="12" md="2">
        <v-select
          v-model="filters.trangThai"
          label="Trạng thái"
          :items="[{title:'Tất cả trạng thái', value:null}, {title:'Đang bán', value:'DANG_HOAT_DONG'}, {title:'Ngừng bán', value:'KHONG_HOAT_DONG'}]"
          variant="outlined" density="comfortable" hide-details
          class="font-weight-medium" rounded="md"
          @update:model-value="loadProducts"
        ></v-select>
      </v-col>

      <!-- Xuất Xứ -->
      <v-col cols="12" md="2">
        <v-select
          v-model="filters.xuatXu"
          label="Xuất xứ"
          :items="[{title:'Tất cả xuất xứ', value:null}, ...filterOptions.xuatXus.map(x => ({title: x.ten, value: x.id}))]"
          variant="outlined" density="comfortable" hide-details
          class="font-weight-medium" rounded="md"
          @update:model-value="loadProducts"
        ></v-select>
      </v-col>

      <!-- Mục Đích Chạy -->
      <v-col cols="12" md="2">
        <v-select
          v-model="filters.mucDichChay"
          label="Mục đích chạy"
          :items="[{title:'Tất cả mục đích', value:null}, ...filterOptions.mucDichChays.map(m => ({title: m.ten, value: m.id}))]"
          variant="outlined" density="comfortable" hide-details
          class="font-weight-medium" rounded="md"
          @update:model-value="loadProducts"
        ></v-select>
      </v-col>

      <!-- Chất Liệu -->
      <v-col cols="12" md="2">
        <v-select
          v-model="filters.chatLieu"
          label="Chất liệu"
          :items="[{title:'Tất cả chất liệu', value:null}, ...filterOptions.chatLieus.map(c => ({title: c.ten, value: c.id}))]"
          variant="outlined" density="comfortable" hide-details
          class="font-weight-medium" rounded="md"
          @update:model-value="loadProducts"
        ></v-select>
      </v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable
      title="Danh sách sản phẩm"
      addButtonText="Thêm sản phẩm"
      show-export-button
      show-import-button
      show-template-button
      :headers="[
        { text: 'Thông tin sản phẩm', align: 'center' ,width: '250px'},
        { text: 'Thương hiệu', align: 'center', width: '150px' },
        { text: 'Phân loại', align: 'center', width: '150px' },
        { text: 'Giá niêm yết', align: 'center', width: '150px' },
        { text: 'Số lượng', align: 'center', width: '130px' },
        { text: 'Trạng thái', align: 'center', width: '130px' },
        { text: 'Thao tác', align: 'center', width: '100px' }
      ]"
      :items="products"
      :loading="loading"
      @add="router.push({ name: 'SanPhamForm' })"
      @export="handleExport"
      @import="$refs.fileInput.click()"
      @download-template="handleDownloadTemplate"
    >
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell">
            <div class="d-flex align-center">
              <v-img :src="item.hinhAnh" width="64" height="64" class="mr-3 border shadow-sm flex-grow-0 rounded-lg" cover></v-img>
              <div>
                <div class="text-subtitle-1 font-weight-bold text-dark mb-1">{{ item.tenSanPham }}</div>
                <div class="text-caption font-weight-bold text-primary">{{ item.maSanPham }}</div>
              </div>
            </div>
          </td>

          <td class="data-cell">
            <div class="text-subtitle-2 font-weight-bold text-secondary mb-1">{{ item.tenThuongHieu }}</div>
            <div class="d-flex flex-wrap gap-1">
              <v-chip size="x-small" variant="outlined" color="secondary" class="font-weight-bold">{{ item.tenXuatXu }}</v-chip>
              <v-chip size="x-small" variant="outlined" color="info" class="font-weight-bold">{{ item.tenChatLieu }}</v-chip>
              <v-chip size="x-small" variant="outlined" color="primary" class="font-weight-bold">{{ item.gioiTinhKhachHang }}</v-chip>
            </div>
          </td>

          <td class="data-cell">
            <v-chip variant="tonal" :color="getCategoryColor(item.tenDanhMuc)" class="font-weight-bold" size="small">
              {{ item.tenDanhMuc }}
            </v-chip>
          </td>

          <td class="data-cell">
            <div class="text-subtitle-1 font-weight-black text-error">{{ formatCurrency(item.giaBanThapNhat || 0) }}</div>
            <div class="text-caption text-secondary" v-if="item.giaBanCaoNhat > item.giaBanThapNhat">
              Đến {{ formatCurrency(item.giaBanCaoNhat) }}
            </div>
          </td>

          <td class="data-cell">
            <div class="text-subtitle-2 font-weight-bold" :class="item.tongSoLuongTon > 10 ? 'text-dark' : 'text-error'">
              {{ item.tongSoLuongTon || 0 }} SP
            </div>
            <div class="text-caption text-secondary">{{ item.tongBienThe || 0 }} mẫu</div>
          </td>

          <td class="data-cell" style="text-align: center;">
            <v-chip 
              :color="item.trangThai === 'DANG_HOAT_DONG' ? 'success' : 'error'"
              variant="flat"
              class="font-weight-bold px-4"
              size="small"
            >
              {{ item.trangThai === 'DANG_HOAT_DONG' ? 'Đang bán' : 'Ngừng bán' }}
            </v-chip>
          </td>

          <td class="data-cell" style="text-align: center;">
            <div class="d-flex align-center justify-center gap-2">
              <v-btn icon variant="tonal" size="32" color="info" class="rounded-lg" @click="router.push({ name: 'SanPhamDetail', params: { id: item.id } })">
                <EyeIcon size="18" />
                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
              </v-btn>
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-lg" @click="router.push({ name: 'SanPhamForm', params: { id: item.id } })">
                <EditIcon size="18" />
                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
              </v-btn>
              <v-switch
                :model-value="item.trangThai === 'DANG_HOAT_DONG'"
                color="success" inset hide-details density="compact"
                :readonly="item.trangThai !== 'DANG_HOAT_DONG'"
                class="tight-switch"
                @click.stop="item.trangThai === 'DANG_HOAT_DONG' ? confirmToggleStatus(item) : null"
              >
                <v-tooltip activator="parent" location="top">Đổi trạng thái</v-tooltip>
              </v-switch>
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

    <!-- Hidden Input for Excel Import -->
    <input type="file" ref="fileInput" class="d-none" accept=".xlsx, .xls" @change="handleImport">

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
:deep(.v-btn) { border-radius: 3px !important; }
</style>
