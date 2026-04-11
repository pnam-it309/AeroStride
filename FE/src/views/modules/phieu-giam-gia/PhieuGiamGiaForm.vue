<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';
import { useNotifications } from '@/services/notificationService';
import {
  ChevronLeftIcon, SaveIcon, TicketIcon, 
  SettingsIcon, InfoCircleIcon, CalendarIcon
} from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const isEditMode = computed(() => !!route.params.id);

const form = ref({
  ma: '',
  ten: '',
  moTa: '',
  loaiPhieu: 'PHAN_TRAM',
  phanTramGiamGia: 0,
  soTienGiam: 0,
  soLuong: 0,
  giatriToiThieu: 0,
  ngayBatDau: '',
  ngayKetThuc: '',
  trangThai: 'DANG_HOAT_DONG'
});

const init = async () => {
  if (isEditMode.value) {
    loading.value = true;
    try {
      const data = await dichVuPhieuGiamGia.layChiTietPhieuGiamGia(route.params.id);
      form.value = {
        ...data,
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
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <v-btn icon variant="tonal" color="primary" class="mr-4 rounded-lg" @click="router.back()">
          <ChevronLeftIcon size="24" />
        </v-btn>
        <div>
          <h2 class="text-h4 font-weight-black text-slate-900">
            {{ isEditMode ? 'Cập nhật voucher' : 'Tạo voucher mới' }}
          </h2>
          <div class="text-subtitle-1 text-slate-500">Thiết lập mã giảm giá và ưu đãi cho khách hàng</div>
        </div>
      </div>
      <div class="d-flex gap-3">
        <v-btn variant="outlined" color="grey" class="text-none font-weight-bold px-6 rounded-lg" @click="router.back()">Hủy bỏ</v-btn>
        <v-btn color="primary" class="text-none font-weight-bold px-8 rounded-lg" :loading="saving" prepend-icon="mdi-content-save" @click="handleSave">
          Lưu voucher
        </v-btn>
      </div>
    </div>

    <v-row>
      <v-col cols="12" lg="8">
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <TicketIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-black">Thông tin chung</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="6">
                <v-text-field v-model="form.ma" label="Mã Voucher *" variant="outlined" placeholder="VÍ DỤ: AERO2024" hide-details class="mb-6"></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field v-model="form.ten" label="Tên Voucher *" variant="outlined" placeholder="Ví dụ: Giảm giá ngày hè" hide-details class="mb-6"></v-text-field>
              </v-col>
              <v-col cols="12">
                <v-textarea v-model="form.moTa" label="Mô tả" variant="outlined" rows="3" placeholder="Điều kiện áp dụng..." hide-details></v-textarea>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <SettingsIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-black">Quy tắc giảm giá</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="6">
                <v-select v-model="form.loaiPhieu" label="Loại ưu đãi *" :items="[{title:'Phần trăm (%)',value:'PHAN_TRAM'},{title:'Số tiền mặt (VNĐ)',value:'TIEN_MAT'}]" variant="outlined"></v-select>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field v-model.number="form.loaiPhieu === 'PHAN_TRAM' ? form.phanTramGiamGia : form.soTienGiam" :label="form.loaiPhieu === 'PHAN_TRAM' ? 'Giá trị giảm (%) *' : 'Số tiền giảm (₫) *'" variant="outlined" type="number" :suffix="form.loaiPhieu === 'PHAN_TRAM' ? '%' : '₫'"></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field v-model.number="form.soLuong" label="Số lượng phát hành *" variant="outlined" type="number" prepend-inner-icon="mdi-numeric"></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field v-model.number="form.giatriToiThieu" label="Đơn hàng tối thiểu (₫)" variant="outlined" type="number" suffix="₫"></v-text-field>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="4">
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <CalendarIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-black">Thời gian hiệu lực</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-text-field v-model="form.ngayBatDau" label="Ngày bắt đầu *" variant="outlined" type="datetime-local" class="mb-4"></v-text-field>
            <v-text-field v-model="form.ngayKetThuc" label="Ngày hết hạn *" variant="outlined" type="datetime-local"></v-text-field>
          </v-card-text>
        </v-card>

        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b font-weight-black">Trạng thái phát hành</v-card-title>
          <v-card-text class="pa-6">
            <v-radio-group v-model="form.trangThai" hide-details>
              <v-radio label="Đang hoạt động" value="DANG_HOAT_DONG" color="success" class="mb-4"></v-radio>
              <v-radio label="Tạm ngưng / Hết hạn" value="KHONG_HOAT_DONG" color="error"></v-radio>
            </v-radio-group>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.gap-3 { gap: 12px; }
</style>
