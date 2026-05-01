<script setup>
import { ref, watch, reactive } from 'vue';
import { 
    XIcon, PhotoIcon, DeviceFloppyIcon, 
    SettingsIcon, TrashIcon, InfoCircleIcon,
    PlusIcon, UploadIcon
} from 'vue-tabler-icons';
import { useNotifications } from '@/services/notificationService';
import { dichVuBienThe } from '@/services/product/dichVuBienThe';
import { dichVuFile } from '@/services/core/dichVuFile';
import AdminEmptyState from '@/components/common/AdminEmptyState.vue';

const props = defineProps({
  show: Boolean,
  variant: Object,
  initialTab: { type: Number, default: 0 }
});

const emit = defineEmits(['update:show', 'saved']);
const { addNotification } = useNotifications();

const loading = ref(false);
const submitting = ref(false);
const activeTab = ref(0);
const fileInput = ref(null);
const uploading = ref(false);

const formData = reactive({
    trangThai: 'DANG_HOAT_DONG',
    soLuong: 0,
    giaBan: 0,
    giaNhap: 0
});

const images = ref([]);

watch(() => props.variant, (v) => {
    if (v) {
        formData.trangThai = v.trangThai;
        formData.soLuong = v.soLuong;
        formData.giaBan = v.giaBan;
        formData.giaNhap = v.giaNhap;
        images.value = v.images || [];
    }
}, { immediate: true });

watch(() => props.show, (isShow) => {
    if (isShow) {
        activeTab.value = props.initialTab;
    }
});

const handleUpdateStatus = async () => {
    submitting.value = true;
    try {
        await dichVuBienThe.capNhatBienThe(props.variant.id, {
            ...formData,
            idMauSac: props.variant.idMauSac,
            idKichThuoc: props.variant.idKichThuoc
        });
        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật trạng thái', color: 'success' });
        emit('saved');
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Thao tác thất bại', color: 'error' });
    } finally {
        submitting.value = false;
    }
};

const formatCurrency = (value) => {
  if (value === null || value === undefined) return '--';
  return new Intl.NumberFormat('vi-VN', { 
    style: 'currency', 
    currency: 'VND', 
    maximumFractionDigits: 0 
  }).format(Number(value));
};

const triggerFileInput = () => {
    fileInput.value?.click();
};

const handleFileChange = async (event) => {
   const file = event.target.files[0];
   if (!file) return;

   // Validate 
   if (!file.type.startsWith('image/')) {
       addNotification({ title: 'Lỗi', subtitle: 'Vui lòng chọn tệp hình ảnh', color: 'error' });
       return;
   }

   uploading.value = true;
   try {
       // 1. Upload to Cloudinary
       const uploadResult = await dichVuFile.taiLenFile(file);
       
       // 2. Save to database for this variant
       const newImage = await dichVuBienThe.themAnh(props.variant.id, {
           duongDanAnh: uploadResult.fileUrl,
           moTa: `Ảnh của ${props.variant.maChiTietSanPham}`,
           hinhAnhDaiDien: images.value.length === 0
       });

       // Update local list for instant feedback
       images.value.push(newImage);

       addNotification({ title: 'Thành công', subtitle: 'Đã tải lên và lưu ảnh', color: 'success' });
       emit('saved');
   } catch (error) {
       addNotification({ title: 'Lỗi', subtitle: 'Không thể thêm ảnh', color: 'error' });
   } finally {
       uploading.value = false;
       if (event.target) event.target.value = '';
   }
};

const deleteImage = async (imgId) => {
    if (!confirm('Xác nhận xóa ảnh này?')) return;
    try {
        await dichVuBienThe.xoaAnh(imgId);
        
        // Update local list for instant feedback
        images.value = images.value.filter(img => img.id !== imgId);
        
        addNotification({ title: 'Thành công', subtitle: 'Đã xóa ảnh', color: 'success' });
        emit('saved');
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể xóa ảnh', color: 'error' });
    }
};

const setMainImage = async (imgId) => {
     try {
        await dichVuBienThe.datAnhChinh(props.variant.id, imgId);
        
        // Update local list for instant feedback
        images.value = images.value.map(img => ({
            ...img,
            hinhAnhDaiDien: img.id === imgId
        }));
        
        addNotification({ title: 'Thành công', subtitle: 'Đã đặt làm ảnh chính', color: 'success' });
        emit('saved');
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Thao tác thất bại', color: 'error' });
    }
};

</script>


<template>
  <v-navigation-drawer
    :model-value="show"
    @update:model-value="emit('update:show', $event)"
    location="right"
    temporary
    width="450"
    class="variant-drawer"
  >
    <div v-if="variant" class="d-flex flex-column h-100 bg-white">
      <!-- Header Area: High-end design -->
      <div class="pa-6 border-b d-flex align-center justify-space-between bg-slate-50/50">
        <div>
            <div class="d-flex align-center gap-2 mb-1">
                <v-icon icon="mdi-package-variant" size="18" color="primary" />
                <h2 class="text-h6 text-slate-900 mb-0" style="font-size: 1.15rem !important;">Quản lý nâng cao</h2>
            </div>
            <p class="text-primary" style="font-size: 13px !important;">{{ variant.maChiTietSanPham }}</p>
        </div>
        <v-btn icon variant="tonal" density="comfortable" color="slate-400" @click="emit('update:show', false)" class="rounded-lg">
          <XIcon size="20" />
        </v-btn>
      </div>

      <!-- Main Scrollable Content -->
      <div class="flex-grow-1 overflow-y-auto pa-6 custom-scrollbar">
        <!-- Persistent Variant Summary Grid -->
        <div class="variant-detail-grid mb-8 pa-4 rounded-lg border bg-slate-50/30">
            <v-row dense>
                <v-col cols="6">
                    <div class="info-group">
                        <label class="info-label">Màu biến thể</label>
                        <div class="info-value-box">{{ variant.tenMauSac }}</div>
                    </div>
                </v-col>
                <v-col cols="6">
                    <div class="info-group">
                        <label class="info-label">Kích thước</label>
                        <div class="info-value-box">{{ variant.tenKichThuoc }}</div>
                    </div>
                </v-col>
                <v-col cols="6" class="mt-3">
                    <div class="info-group">
                        <label class="info-label">Tồn kho</label>
                        <div class="info-value-box">{{ variant.soLuong }}</div>
                    </div>
                </v-col>
                <v-col cols="6" class="mt-3">
                    <div class="info-group">
                        <label class="info-label">Giá bán</label>
                        <div class="info-value-box text-primary font-bold-force">{{ formatCurrency(variant.giaBan) }}</div>
                    </div>
                </v-col>
            </v-row>
        </div>

        <!-- Custom Styled Tabs -->
        <v-tabs v-model="activeTab" color="primary" align-tabs="start" class="mb-6 mb-tabs-border" grow hide-slider>
            <v-tab :value="0" class="custom-tab px-6" rounded="lg" style="font-size: 13px !important;">
                <SettingsIcon size="18" class="mr-2" /> 
                <span>Trạng thái</span>
            </v-tab>
            <v-tab :value="1" class="custom-tab px-6" rounded="lg" style="font-size: 13px !important;">
                <PhotoIcon size="18" class="mr-2" /> 
                <span>Hình ảnh</span>
            </v-tab>
        </v-tabs>

        <v-window v-model="activeTab">
            <!-- TAB 0: STATUS & OPS -->
            <v-window-item :value="0">
                <div class="space-y-6">
                    <div class="form-group">
                        <label class="drawer-label d-block mb-3" style="font-size: 13px !important;">Hiển thị trên gian hàng</label>
                        <v-select
                            v-model="formData.trangThai"
                            :items="[
                                { title: 'Đang hoạt động (Hiển thị)', value: 'DANG_HOAT_DONG' },
                                { title: 'Ngừng hoạt động (Ẩn)', value: 'KHONG_HOAT_DONG' }
                            ]"
                            variant="outlined"
                            density="comfortable"
                            hide-details
                            class="rounded-xl custom-select"
                        >
                            <template #prepend-inner>
                                <v-icon icon="mdi-eye-outline" size="20" class="text-slate-400 mr-2" />
                            </template>
                        </v-select>
                    </div>

                    <div class="info-banner rounded-xl bg-primary-lighten-5 border border-primary-lighten-4 pa-4">
                        <div class="d-flex gap-3 text-primary-darken-1">
                            <v-icon icon="mdi-information-variant" size="20" class="mt-0-5" />
                            <div style="font-size: 13px !important;">
                                Việc thay đổi trạng thái sẽ cập nhật ngay lập tức khả năng hiển thị của SKU này với khách hàng. Hãy chắc chắn số lượng tồn kho là chính xác.
                            </div>
                        </div>
                    </div>

                    <v-btn 
                        block 
                        color="primary" 
                        height="52" 
                        class="rounded-xl shadow-lg hover-lift mt-4 text-white-force" 
                        style="font-size: 13px !important;"
                        :loading="submitting" 
                        @click="handleUpdateStatus"
                    >
                        Lưu thay đổi trạng thái
                    </v-btn>
                </div>
            </v-window-item>

            <!-- TAB 1: IMAGE GALLERY -->
            <v-window-item :value="1">
                <!-- Advanced Upload Zone -->
                <div class="mb-6">
                    <input 
                        type="file" 
                        ref="fileInput" 
                        class="d-none" 
                        accept="image/*" 
                        @change="handleFileChange"
                    />
                    <div 
                        class="upload-dropzone rounded-xl border border-dashed border-slate-100 pa-8 text-center cursor-pointer hover-border-primary transition-all"
                        @click="triggerFileInput"
                    >
                        <div class="upload-icon-wrapper mb-3">
                            <v-icon v-if="!uploading" icon="mdi-cloud-upload" size="40" color="primary" />
                            <v-progress-circular v-else indeterminate color="primary" size="40" />
                        </div>
                        <h4 class="text-slate-900 mb-1" style="font-size: 13px !important;">
                            {{ uploading ? 'Đang xử lý tập tin...' : 'Tải lên hình ảnh mới' }}
                        </h4>
                        <p class="text-slate-400 mb-0" style="font-size: 13px !important;">Hỗ trợ JPG, PNG • Tối đa 5MB</p>
                    </div>
                </div>

                <div class="d-flex align-center justify-space-between mb-4 px-1">
                    <h5 class="text-slate-700" style="font-size: 13px !important;">Thư viện ({{ images.length }})</h5>
                    <span class="text-slate-400" style="font-size: 13px !important;">* Click để đặt làm ảnh chính</span>
                </div>

                <v-row v-if="images.length > 0" dense class="gallery-grid">
                    <v-col v-for="img in images" :key="img.id" cols="6" sm="4">
                        <div class="image-card rounded-xl overflow-hidden border position-relative" :class="{ 'is-main': img.hinhAnhDaiDien }">
                            <v-img 
                                :src="img.duongDanAnh" 
                                cover 
                                height="100" 
                                class="cursor-pointer hover-zoom"
                                @click="setMainImage(img.id)"
                            >
                                <template #placeholder>
                                    <div class="d-flex align-center justify-center h-100 bg-slate-50">
                                        <v-progress-circular indeterminate color="primary-lighten-4" size="20" />
                                    </div>
                                </template>
                            </v-img>

                            <!-- Labels & Actions -->
                            <div v-if="img.hinhAnhDaiDien" class="main-badge">CHÍNH</div>
                            <v-btn
                                icon
                                size="24"
                                color="rose-darken-1"
                                variant="flat"
                                class="delete-btn shadow-md"
                                @click.stop="deleteImage(img.id)"
                            >
                                <XIcon size="14" class="text-white" />
                            </v-btn>
                        </div>
                    </v-col>
                </v-row>

                <AdminEmptyState v-else 
                    icon="mdi-image-plus-outline" 
                    title="Chưa có hình ảnh" 
                    subtitle="Hãy tải lên hình ảnh đầu tiên để biến thể trông thu hút hơn."
                    class="py-10"
                    icon-size="40"
                />
            </v-window-item>
        </v-window>
      </div>
    </div>
  </v-navigation-drawer>
</template>

<style scoped>
.variant-drawer {
    z-index: 2000 !important;
}

.variant-drawer :deep(.v-navigation-drawer__content) {
    overflow: hidden;
}

.custom-scrollbar::-webkit-scrollbar {
    width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
    background: #e2e8f0;
    border-radius: 10px;
}

.drawer-label {
    font-size: 13px;
    font-weight: 400;
    color: #475569;
    letter-spacing: 0.01em;
}

.info-label {
    display: block;
    font-size: 13px;
    color: #64748b;
    margin-bottom: 4px;
}

.info-value-box {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 4px;
    padding: 8px 12px;
    font-size: 13px;
    color: #1e293b;
    min-height: 38px;
    display: flex;
    align-items: center;
}

.font-bold-force {
    font-weight: 600 !important;
}


.upload-dropzone {
    background-color: #f8fafc;
    transition: all 0.3s ease;
}
.upload-dropzone:hover {
    background-color: #f1f7ff;
    border-color: #3b82f6 !important;
}

.image-card {
    transition: all 0.3s ease;
    border-color: #e2e8f0 !important;
    background: white;
}
.image-card.is-main {
    border-color: #3b82f6 !important;
    border-width: 2px !important;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.main-badge {
    position: absolute;
    top: 0;
    left: 0;
    background: #3b82f6;
    color: white;
    font-size: 9px;
    font-weight: 900;
    padding: 2px 8px;
    border-bottom-right-radius: 10px;
    z-index: 1;
    box-shadow: 2px 2px 4px rgba(0,0,0,0.1);
}

.delete-btn {
    position: absolute;
    top: 6px;
    right: 6px;
    z-index: 2;
    opacity: 0;
    transform: scale(0.8);
    transition: all 0.2s ease;
}

.image-card:hover .delete-btn {
    opacity: 1;
    transform: scale(1);
}

.hover-zoom {
    transition: transform 0.5s ease;
}
.image-card:hover .hover-zoom {
    transform: scale(1.1);
}

.custom-tab {
    opacity: 0.6;
    transition: all 0.3s ease;
    border: 1px solid transparent;
}
.custom-tab.v-tab--selected {
    opacity: 1;
    background-color: #f1f5f9;
}

.mb-tabs-border {
    border-bottom: 2px solid #f1f5f9;
}

.hover-lift {
    transition: all 0.3s ease;
}
.hover-lift:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05) !important;
}

.info-banner {
    transition: all 0.3s ease;
}

.text-white-force {
    color: white !important;
}

.text-white-force :deep(.v-chip__content) {
    color: white !important;
}

.space-y-6 > * + * {
    margin-top: 1.5rem;
}
</style>


