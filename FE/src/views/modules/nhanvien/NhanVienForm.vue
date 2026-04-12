<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import { useNotifications } from '@/services/notificationService';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { ArrowLeftIcon, DeviceFloppyIcon, UserIcon } from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const isEditMode = ref(false);
const isDetailView = computed(() => route.path.includes('/detail'));
const showPassword = ref(false);

const employeeForm = ref({
  ma: '',
  ten: '',
  email: '',
  sdt: '',
  tenTaiKhoan: '',
  ngaySinh: '',
  idPhanQuyen: 1, 
  gioiTinh: true,
  trangThai: 'DANG_HOAT_DONG',
  hinhAnh: ''
});

const roles = ref([]);
 
const getRoleColor = (ma) => {
  if (ma === 'ADMIN' || ma?.includes('AD')) return 'error';
  if (ma === 'KHO' || ma?.includes('KH')) return 'primary';
  return 'success';
};

const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  color: 'success',
  action: null
});

const loadEmployee = async (id) => {
  loading.value = true;
  try {
    const data = await dichVuNhanVien.layNhanVienTheoId(id);
    employeeForm.value = { ...data };
    isEditMode.value = true;
  } catch (error) {
    console.error('Error loading employee:', error);
    addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin nhân viên', color: 'error' });
  } finally {
    loading.value = false;
  }
};

const handleSave = () => {
  confirmDialog.value = {
    show: true,
    title: isEditMode.value ? 'Cập nhật nhân viên' : 'Thêm nhân viên mới',
    message: `Xác nhận lưu thông tin nhân viên [${employeeForm.value.ten}]?`,
    color: 'primary',
    action: async () => {
      saving.value = true;
      try {
        if (isEditMode.value) {
          await dichVuNhanVien.capNhatNhanVien(route.params.id, employeeForm.value);
          addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật thông tin nhân viên', color: 'success' });
        } else {
          await dichVuNhanVien.taoNhanVien(employeeForm.value);
          addNotification({ title: 'Thành công', subtitle: 'Đã thêm nhân viên mới', color: 'success' });
        }
        router.push('/nhan-vien');
      } catch (error) {
        console.error('Error saving employee:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Có lỗi xảy ra khi lưu thông tin', color: 'error' });
      } finally {
        saving.value = false;
        confirmDialog.value.show = false;
      }
    }
  };
};

onMounted(async () => {
  loading.value = true;
  try {
    const rolesData = await dichVuNhanVien.layDanhSachPhanQuyen();
    roles.value = rolesData.map(r => ({
      title: r.ten,
      value: r.id,
      color: getRoleColor(r.ma)
    }));
 
    if (route.params.id) {
      await loadEmployee(route.params.id);
    }
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Breadcrumbs / Header -->
    <div class="d-flex align-center mb-6">
      <v-btn icon variant="tonal" color="secondary" class="mr-4 rounded-lg" @click="router.push('/nhan-vien')">
        <ArrowLeftIcon size="20" />
      </v-btn>
      <div>
        <h1 class="text-h4 font-weight-bold">{{ isDetailView ? 'Hồ sơ nhân viên' : (isEditMode ? 'Cập nhật tài khoản' : 'Thêm nhân viên mới') }}</h1>
        <p class="text-subtitle-1 text-medium-emphasis">
          {{ isDetailView ? 'Xem chi tiết thông tin nhân sự và quyền truy cập' : 'Quản lý thông tin hồ sơ và phân quyền hệ thống' }}
        </p>
      </div>
      <v-spacer></v-spacer>
      <div class="d-flex gap-2">
        <v-btn
          v-if="!isDetailView"
          color="primary"
          prepend-icon="mdi-content-save"
          class="text-none font-weight-bold px-8 rounded-lg"
          height="44"
          :loading="saving"
          @click="handleSave"
        >
          Lưu thông tin
        </v-btn>
        <v-btn
          v-if="isDetailView"
          color="primary"
          variant="elevated"
          prepend-icon="mdi-pencil"
          class="text-none font-weight-bold px-10 rounded-lg"
          height="44"
          @click="router.push(`/nhan-vien/form/${route.params.id}`)"
        >
          Chỉnh sửa hồ sơ
        </v-btn>
      </div>
    </div>

    <v-row v-if="loading">
      <v-col cols="12" class="text-center py-16">
        <v-progress-circular indeterminate color="primary" size="64" width="6"></v-progress-circular>
        <div class="mt-4 text-h6 font-weight-medium">Đang tải hồ sơ...</div>
      </v-col>
    </v-row>

    <!-- DASHBOARD VIEW (DETAIL) -->
    <v-row v-else-if="isDetailView">
      <v-col cols="12" lg="4">
        <v-card class="rounded-xl border shadow-none mb-6 text-center pa-8">
          <v-avatar size="150" color="primary-lighten-5" class="mb-4 border shadow-sm">
             <v-img v-if="employeeForm.hinhAnh" :src="employeeForm.hinhAnh"></v-img>
             <v-icon v-else size="64" color="primary">mdi-account</v-icon>
          </v-avatar>
          <h2 class="text-h5 font-weight-black mb-1 text-dark">{{ employeeForm.ten }}</h2>
          <div class="text-subtitle-2 text-medium-emphasis mb-4">{{ employeeForm.email }}</div>
          
          <v-chip 
            :color="roles.find(r => r.value === employeeForm.idPhanQuyen)?.color || 'grey'" 
            variant="flat" 
            class="px-6 font-weight-bold mb-4"
          >
            {{ roles.find(r => r.value === employeeForm.idPhanQuyen)?.title || 'Chưa phân quyền' }}
          </v-chip>

          <v-divider class="my-6"></v-divider>
          
          <div class="d-flex justify-center flex-column gap-2 text-left">
             <div class="d-flex align-center">
                <v-icon color="success" size="18" class="mr-3">mdi-check-circle</v-icon>
                <span class="text-subtitle-2 font-weight-bold text-dark">Trạng thái: </span>
                <span class="ml-2 text-subtitle-2 font-weight-medium" :class="employeeForm.trangThai === 'DANG_HOAT_DONG' ? 'text-success' : 'text-error'">
                   {{ employeeForm.trangThai === 'DANG_HOAT_DONG' ? 'Đang hoạt động' : 'Tạm khóa' }}
                </span>
             </div>
             <div class="d-flex align-center mt-2">
                <v-icon color="info" size="18" class="mr-3">mdi-account-key</v-icon>
                <span class="text-subtitle-2 font-weight-bold text-dark">Username: </span>
                <span class="ml-2 text-subtitle-2 font-weight-black">{{ employeeForm.tenTaiKhoan }}</span>
             </div>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" lg="8">
        <v-card class="rounded-xl border shadow-none mb-6">
           <v-card-title class="pa-6 border-b font-weight-black text-subtitle-1">Thông tin chi tiết hồ sơ</v-card-title>
           <v-card-text class="pa-6">
              <v-row>
                 <v-col cols="12" md="6" class="border-b-dashed pb-4 mb-2">
                    <div class="text-caption text-medium-emphasis font-weight-bold mb-1">HỌ VÀ TÊN</div>
                    <div class="text-subtitle-1 font-weight-black text-dark">{{ employeeForm.ten }}</div>
                 </v-col>
                 <v-col cols="12" md="6" class="border-b-dashed pb-4 mb-2">
                    <div class="text-caption text-medium-emphasis font-weight-bold mb-1">ĐỊA CHỈ EMAIL</div>
                    <div class="text-subtitle-1 font-weight-black text-dark">{{ employeeForm.email }}</div>
                 </v-col>
                 <v-col cols="12" md="6" class="border-b-dashed pb-4 mb-2">
                    <div class="text-caption text-medium-emphasis font-weight-bold mb-1">SỐ ĐIỆN THOẠI</div>
                    <div class="text-subtitle-1 font-weight-black text-dark">{{ employeeForm.sdt }}</div>
                 </v-col>
                 <v-col cols="12" md="6" class="border-b-dashed pb-4 mb-2">
                    <div class="text-caption text-medium-emphasis font-weight-bold mb-1">NGÀY SINH</div>
                    <div class="text-subtitle-1 font-weight-black text-dark">{{ employeeForm.ngaySinh || 'Chưa cập nhật' }}</div>
                 </v-col>
                 <v-col cols="12" md="6" class="border-b-dashed pb-4 mb-2">
                    <div class="text-caption text-medium-emphasis font-weight-bold mb-1">GIỚI TÍNH</div>
                    <div class="text-subtitle-1 font-weight-black text-dark">{{ employeeForm.gioiTinh ? 'Nam' : 'Nữ' }}</div>
                 </v-col>
                 <v-col cols="12" md="6" class="border-b-dashed pb-4 mb-2">
                    <div class="text-caption text-medium-emphasis font-weight-bold mb-1">NGÀY GIA NHẬP</div>
                    <div class="text-subtitle-1 font-weight-black text-dark">{{ new Date().toLocaleDateString('vi-VN') }}</div>
                 </v-col>
              </v-row>
           </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- FORM VIEW (CREATE/EDIT) -->
    <v-row v-else>
      <v-col cols="12" lg="8">
        <v-card class="rounded-xl border shadow-sm mb-6" elevation="0">
          <v-card-title class="pa-6 border-b d-flex align-center">
            <UserIcon class="mr-3 text-primary" size="24" />
            <span class="font-weight-bold">Thông tin định danh</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="4">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mã nhân viên <span class="text-caption text-primary">(Hệ thống tự tạo)</span></div>
                <v-text-field
                  v-model="employeeForm.ma"
                  readonly
                  placeholder="MV-XXXX"
                  variant="outlined"
                  density="comfortable"
                  class="font-weight-black"
                  hide-details
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="8">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Họ và tên *</div>
                <v-text-field
                  v-model="employeeForm.ten"
                  :readonly="isDetailView"
                  placeholder="Ví dụ: Nguyễn Văn A"
                  variant="outlined"
                  density="comfortable"
                  class="font-weight-medium"
                  hide-details
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Email *</div>
                <v-text-field
                  v-model="employeeForm.email"
                  :readonly="isDetailView"
                  placeholder="name@company.com"
                  variant="outlined"
                  density="comfortable"
                  class="font-weight-medium"
                  hide-details
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Số điện thoại *</div>
                <v-text-field
                  v-model="employeeForm.sdt"
                  :readonly="isDetailView"
                  placeholder="09xx.xxx.xxx"
                  variant="outlined"
                  density="comfortable"
                  class="font-weight-medium"
                  hide-details
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Ngày sinh</div>
                <v-text-field
                  v-model="employeeForm.ngaySinh"
                  :readonly="isDetailView"
                  type="date"
                  variant="outlined"
                  density="comfortable"
                  class="font-weight-medium"
                  hide-details
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Giới tính</div>
                <v-select
                  v-model="employeeForm.gioiTinh"
                  :readonly="isDetailView"
                  :items="[{title: 'Nam', value: true}, {title: 'Nữ', value: false}]"
                  variant="outlined"
                  density="comfortable"
                  class="font-weight-medium"
                  hide-details
                ></v-select>
              </v-col>

            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="rounded-xl border shadow-sm" elevation="0">
          <v-card-title class="pa-6 border-b d-flex align-center">
            <v-icon class="mr-3 text-primary" size="24">mdi-lock</v-icon>
            <span class="font-weight-bold">Tài khoản & Phân quyền</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tên đăng nhập</div>
                <v-text-field
                  v-model="employeeForm.tenTaiKhoan"
                  :readonly="true"
                  placeholder="Username"
                  variant="outlined"
                  density="comfortable"
                  class="font-weight-medium"
                  hide-details
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Lưu ý bảo mật</div>
                <div class="pa-4 bg-amber-lighten-5 rounded-lg border-amber-lighten-3 border">
                  <p class="text-caption text-amber-darken-4 font-weight-bold mb-0">
                    <v-icon start size="16">mdi-information</v-icon>
                    Hệ thống sẽ tự động gửi email thiết lập mật khẩu đến địa chỉ email của nhân viên. Admin không thể đặt mật khẩu thủ công.
                  </p>
                </div>
              </v-col>
              <v-col cols="12">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Vai trò & Phân quyền</div>
                <v-select
                  v-model="employeeForm.idPhanQuyen"
                  :readonly="isDetailView"
                  :items="roles"
                  item-title="title"
                  item-value="value"
                  variant="outlined"
                  density="comfortable"
                  class="font-weight-bold"
                  hide-details
                >
                    <template #selection="{ item }">
                        <v-chip :color="item.raw.color" size="small" variant="flat" class="px-4 font-weight-bold">{{ item.title }}</v-chip>
                    </template>
                </v-select>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="4">
        <v-card class="rounded-xl border shadow-sm mb-6" elevation="0">
          <v-card-title class="pa-6 border-b">Ảnh đại diện</v-card-title>
          <v-card-text class="pa-6 text-center">
            <v-avatar size="150" color="grey-lighten-4" class="mb-4 border border-dashed rounded-lg">
              <v-img v-if="employeeForm.hinhAnh" :src="employeeForm.hinhAnh"></v-img>
              <v-icon v-else size="64" color="grey-lighten-1">mdi-account-plus</v-icon>
            </v-avatar>
            <div v-if="!isDetailView" class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">URL Hình ảnh</div>
            <v-text-field
              v-if="!isDetailView"
              v-model="employeeForm.hinhAnh"
              placeholder="https://..."
              variant="outlined"
              density="compact"
              hide-details
              class="font-weight-medium"
            ></v-text-field>
            <p class="text-caption text-medium-emphasis mt-2">Dán URL hình ảnh nhân viên vào đây</p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- SHARED CONFIRM -->
    <AdminConfirm
      v-model:show="confirmDialog.show"
      :title="confirmDialog.title"
      :message="confirmDialog.message"
      :color="confirmDialog.color"
      @confirm="confirmDialog.action"
    />
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #f8fafc; }
.font-body { font-family: 'Public Sans', sans-serif; }
.gap-2 { gap: 8px; }
.border-b-dashed { border-bottom: 1px dashed #e2e8f0; }
:deep(.v-btn) { border-radius: 8px !important; }
</style>
