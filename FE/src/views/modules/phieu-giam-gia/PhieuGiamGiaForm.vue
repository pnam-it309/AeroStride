<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useNotifications } from '@/services/notificationService';
import {
  ChevronLeftIcon, DeviceFloppyIcon, TicketIcon, 
  SettingsIcon, InfoCircleIcon, CalendarIcon
} from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));
const isDetailView = computed(() => route.path.includes('/detail'));
const customers = ref([]);
const selectedCustomers = ref([]);

const form = ref({
  ma: '',
  ten: '',
  moTa: '',
  loaiPhieu: 'PHAN_TRAM',
  loaiHienThi: 'CONG_KHAI', // CONG_KHAI or CA_NHAN
  phanTramGiamGia: 0,
  soTienGiam: 0,
  soLuong: 0,
  giatriToiThieu: 0,
  giamToiDa: 0,
  ngayBatDau: '',
  ngayKetThuc: '',
  trangThai: 'DANG_HOAT_DONG',
  listIdKhachHang: [],
  ghiChu: ''
});

const init = async () => {
  // Load customers for selection
  try {
    const data = await dichVuKhachHang.layTatCaKhachHang();
    customers.value = data?.content || data || [];
  } catch (e) {
    console.error('Error loading customers:', e);
  }

  if (isEditMode.value) {
    loading.value = true;
    try {
      const data = await dichVuPhieuGiamGia.layChiTietPhieuGiamGia(route.params.id);
      form.value = {
        ...data,
        loaiHienThi: data.hinhThuc || 'CONG_KHAI',
        ngayBatDau: data.ngayBatDau ? new Date(data.ngayBatDau).toISOString().slice(0, 16) : '',
        ngayKetThuc: data.ngayKetThuc ? new Date(data.ngayKetThuc).toISOString().slice(0, 16) : ''
      };
    } catch (e) {
      addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin', color: 'error' });
    } finally {
      loading.value = false;
    }
  }
};

const handleSave = async () => {
  saving.value = true;
  try {
    const payload = {
      ...form.value,
      donHangToiThieu: form.value.giatriToiThieu,
      hinhThuc: form.value.loaiHienThi,
      ngayBatDau: new Date(form.value.ngayBatDau).getTime(),
      ngayKetThuc: new Date(form.value.ngayKetThuc).getTime()
    };

    if (isEditMode.value) {
      await dichVuPhieuGiamGia.capNhatPhieuGiamGia(route.params.id, payload);
      addNotification({ title: 'Thành công', subtitle: 'Cấu hình voucher hoàn tất', color: 'success' });
    } else {
      await dichVuPhieuGiamGia.taoPhieuGiamGia(payload);
      addNotification({ title: 'Thành công', subtitle: 'Đã tạo voucher mới', color: 'success' });
    }
    router.push('/phieu-giam-gia');
  } catch (e) {
    addNotification({ title: 'Lỗi', subtitle: 'Lỗi khi lưu dữ liệu', color: 'error' });
  } finally {
    saving.value = false;
  }
};

onMounted(init);
</script>

<template>
  <v-container fluid class="pa-6 bg-grey-lighten-4 min-h-screen">
    <!-- Header -->
    <div class="d-flex align-center mb-6">
      <v-btn icon variant="tonal" color="secondary" class="mr-4 rounded-lg" @click="router.push('/phieu-giam-gia')">
        <v-icon>mdi-arrow-left</v-icon>
      </v-btn>
      <div>
        <h1 class="text-h4 font-weight-bold">
            {{ isDetailView ? 'Chi tiết bộ Voucher' : (isEditMode ? 'Cập nhật Voucher' : 'Thiết lập Voucher mới') }}
        </h1>
        <p class="text-subtitle-1 text-medium-emphasis">Cấu hình mã giảm giá và các điều kiện áp dụng ưu đãi</p>
      </div>
      <v-spacer></v-spacer>
      <v-btn
        v-if="!isDetailView"
        color="primary"
        prepend-icon="mdi-content-save"
        class="text-none font-weight-bold px-8 rounded-lg"
        height="44"
        :loading="saving"
        @click="handleSave"
      >
        Lưu Voucher
      </v-btn>
      <v-btn
        v-else
        color="primary"
        variant="tonal"
        prepend-icon="mdi-pencil"
        class="text-none font-weight-bold px-8 rounded-lg"
        height="44"
        @click="router.push(`/phieu-giam-gia/form/${route.params.id}`)"
      >
        Chỉnh sửa Voucher
      </v-btn>
    </div>

    <v-row>
      <v-col cols="12" lg="8">
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <TicketIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Thông tin chung</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mã Voucher * <span class="text-caption text-primary" v-if="!isEditMode && !isDetailView">(Tự động sinh nếu trống)</span></div>
                <v-text-field v-model="form.ma" :readonly="isDetailView || isEditMode" placeholder="Hệ thống tự tạo..." variant="outlined" density="comfortable" hide-details class="mb-4 font-weight-bold"></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tên Voucher *</div>
                <v-text-field v-model="form.ten" :readonly="isDetailView" placeholder="Ví dụ: Giảm giá ngày hè" variant="outlined" density="comfortable" hide-details class="mb-4"></v-text-field>
              </v-col>
              <v-col cols="12">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mô tả</div>
                <v-textarea v-model="form.moTa" :readonly="isDetailView" variant="outlined" rows="3" placeholder="Điều kiện áp dụng..." hide-details></v-textarea>
              </v-col>
              <v-col cols="12">
                <div class="text-subtitle-2 font-weight-bold mb-3 ml-1 text-dark">Phạm vi áp dụng</div>
                <v-radio-group v-model="form.loaiHienThi" inline hide-details :readonly="isDetailView">
                  <v-radio label="Công khai (Tất cả khách hàng)" value="CONG_KHAI" color="primary" class="mr-8"></v-radio>
                  <v-radio label="Cá nhân (Chỉ định khách hàng)" value="CA_NHAN" color="primary"></v-radio>
                </v-radio-group>
              </v-col>
              <v-col cols="12" v-if="form.loaiHienThi === 'CA_NHAN'">
                <div class="text-subtitle-2 font-weight-bold mb-2 ml-1 text-dark">Chọn khách hàng nhận phiếu *</div>
                <v-autocomplete
                  v-model="form.listIdKhachHang"
                  :items="customers"
                  item-title="ten"
                  item-value="id"
                  label="Chọn khách hàng nhận phiếu *"
                  multiple
                  chips
                  closable-chips
                  variant="outlined"
                  placeholder="Tìm kiếm khách hàng..."
                  hide-details
                  no-data-text="Không tìm thấy khách hàng nào"
                  class="mt-1"
                  :readonly="isDetailView"
                >
                  <template #item="{ props, item }">
                    <v-list-item v-bind="props" :subtitle="`${item.raw.sdt || ''} - ${item.raw.email || ''}`"></v-list-item>
                  </template>
                </v-autocomplete>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <SettingsIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Quy tắc giảm giá</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Loại ưu đãi *</div>
                <v-select v-model="form.loaiPhieu" :readonly="isDetailView" :items="[{title:'Phần trăm (%)',value:'PHAN_TRAM'},{title:'Số tiền mặt (VNĐ)',value:'TIEN_MAT'}]" variant="outlined" density="comfortable" hide-details></v-select>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">{{ form.loaiPhieu === 'PHAN_TRAM' ? 'Giá trị giảm (%) *' : 'Số tiền giảm (₫) *' }}</div>
                <v-text-field v-if="form.loaiPhieu === 'PHAN_TRAM'" v-model.number="form.phanTramGiamGia" :readonly="isDetailView" variant="outlined" density="comfortable" type="number" suffix="%" hide-details></v-text-field>
                <v-text-field v-else v-model.number="form.soTienGiam" :readonly="isDetailView" variant="outlined" density="comfortable" type="number" suffix="₫" hide-details></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Số lượng phát hành *</div>
                <v-text-field 
                  v-model.number="form.soLuong" 
                  :readonly="isDetailView || form.loaiHienThi === 'CA_NHAN'" 
                  variant="outlined" 
                  density="comfortable" 
                  type="number" 
                  prepend-inner-icon="mdi-numeric" 
                  hide-details
                  :messages="form.loaiHienThi === 'CA_NHAN' ? 'Số lượng tự động tính theo danh sách khách hàng' : ''"
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Giá trị đơn hàng tối thiểu (₫)</div>
                <v-text-field v-model.number="form.giatriToiThieu" :readonly="isDetailView" variant="outlined" density="comfortable" type="number" suffix="₫" hide-details></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Giảm tối đa (₫)</div>
                <v-text-field v-model.number="form.giamToiDa" :readonly="isDetailView" variant="outlined" density="comfortable" type="number" suffix="₫" hide-details></v-text-field>
              </v-col>
              <v-col cols="12">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Ghi chú</div>
                <v-textarea v-model="form.ghiChu" :readonly="isDetailView" variant="outlined" rows="2" placeholder="Lưu ý thêm..." hide-details></v-textarea>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="4">
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <CalendarIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Thời gian hiệu lực</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Ngày bắt đầu *</div>
            <v-text-field v-model="form.ngayBatDau" :readonly="isDetailView" variant="outlined" density="comfortable" type="datetime-local" class="mb-4" hide-details></v-text-field>
            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Ngày hết hạn *</div>
            <v-text-field v-model="form.ngayKetThuc" :readonly="isDetailView" variant="outlined" density="comfortable" type="datetime-local" hide-details></v-text-field>
          </v-card-text>
        </v-card>

        <v-card v-if="isDetailView" class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b font-weight-medium">Trạng thái phát hành</v-card-title>
          <v-card-text class="pa-6">
            <v-select
                v-model="form.trangThai"
                :readonly="isDetailView"
                :items="[{title:'Hoạt động',value:'DANG_HOAT_DONG'},{title:'Tạm ngưng',value:'KHONG_HOAT_DONG'}]"
                variant="outlined"
                density="comfortable"
                hide-details
            >
                <template #selection="{ item }">
                    <v-chip :color="item.value === 'DANG_HOAT_DONG' ? 'success' : 'error'" variant="flat" size="small" class="px-4 font-weight-bold">{{ item.title }}</v-chip>
                </template>
            </v-select>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.gap-3 { gap: 12px; }
</style>
