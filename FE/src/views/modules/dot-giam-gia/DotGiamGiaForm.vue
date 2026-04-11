<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';
import { useNotifications } from '@/services/notificationService';
import {
  ChevronLeftIcon, SaveIcon, CalendarIcon, 
  GiftIcon, InfoCircleIcon, TagIcon
} from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const isEditMode = computed(() => !!route.params.id);

const form = ref({
  ten: '',
  moTa: '',
  loaiGiamGia: 'PHAN_TRAM',
  soTienGiam: 0,
  ngayBatDau: '',
  ngayKetThuc: '',
  trangThai: 'DANG_HOAT_DONG'
});

const init = async () => {
  if (isEditMode.value) {
    loading.value = true;
    try {
      const data = await dichVuDotGiamGia.layChiTietDotGiamGia(route.params.id);
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
  if (!form.value.ten || !form.value.soTienGiam || !form.value.ngayBatDau || !form.value.ngayKetThuc) {
    addNotification({ title: 'Thiếu thông tin', subtitle: 'Vui lòng điền các trường bắt buộc', color: 'warning' });
    return;
  }

  saving.value = true;
  try {
    const payload = {
      ...form.value,
      ngayBatDau: new Date(form.value.ngayBatDau).getTime(),
      ngayKetThuc: new Date(form.value.ngayKetThuc).getTime()
    };

    if (isEditMode.value) {
      await dichVuDotGiamGia.capNhatDotGiamGia(route.params.id, payload);
      addNotification({ title: 'Thành công', subtitle: 'Cập nhật chiến dịch hoàn tất', color: 'success' });
    } else {
      await dichVuDotGiamGia.taoDotGiamGia(payload);
      addNotification({ title: 'Thành công', subtitle: 'Đã tạo chiến dịch mới', color: 'success' });
    }
    router.push('/dot-giam-gia');
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
          <h2 class="text-h4 font-weight-bold">
            {{ isEditMode ? 'Cập nhật chiến dịch' : 'Tạo đợt giảm giá mới' }}
          </h2>
          <p class="text-subtitle-1 text-medium-emphasis">Thiết lập chương trình khuyến mãi cho hệ thống</p>
        </div>
      </div>
      <div class="d-flex gap-3">
        <v-btn variant="outlined" color="grey" class="text-none font-weight-bold px-6 rounded-lg" @click="router.back()">Hủy bỏ</v-btn>
        <v-btn color="primary" class="text-none font-weight-bold px-8 rounded-lg" :loading="saving" prepend-icon="mdi-content-save" @click="handleSave">
          Lưu chiến dịch
        </v-btn>
      </div>
    </div>

    <v-row>
      <v-col cols="12" lg="8">
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <InfoCircleIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-black">Thông tin chiến dịch</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12">
                <v-text-field v-model="form.ten" label="Tên đợt giảm giá *" variant="outlined" placeholder="Ví dụ: Siêu Sale Hè 2024" hide-details class="mb-6"></v-text-field>
              </v-col>
              <v-col cols="12">
                <v-textarea v-model="form.moTa" label="Mô tả" variant="outlined" rows="3" placeholder="Chi tiết chương trình..." hide-details></v-textarea>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <TagIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-black">Cấu hình giảm giá</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="6">
                <v-select v-model="form.loaiGiamGia" label="Loại giảm giá *" :items="[{title:'Phần trăm (%)',value:'PHAN_TRAM'},{title:'Số tiền mặt (VNĐ)',value:'TIEN_MAT'}]" variant="outlined"></v-select>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field v-model.number="form.soTienGiam" :label="form.loaiGiamGia === 'PHAN_TRAM' ? 'Phần trăm giảm *' : 'Số tiền giảm *'" variant="outlined" type="number" :suffix="form.loaiGiamGia === 'PHAN_TRAM' ? '%' : '₫'"></v-text-field>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="4">
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <CalendarIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-black">Thời gian áp dụng</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-text-field v-model="form.ngayBatDau" label="Ngày bắt đầu *" variant="outlined" type="datetime-local" class="mb-4"></v-text-field>
            <v-text-field v-model="form.ngayKetThuc" label="Ngày kết thúc *" variant="outlined" type="datetime-local"></v-text-field>
          </v-card-text>
        </v-card>

        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b font-weight-black">Trạng thái</v-card-title>
          <v-card-text class="pa-6">
            <v-radio-group v-model="form.trangThai" hide-details>
              <v-radio label="Đang hoạt động" value="DANG_HOAT_DONG" color="success" class="mb-4"></v-radio>
              <v-radio label="Tạm ngưng" value="KHONG_HOAT_DONG" color="error"></v-radio>
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
