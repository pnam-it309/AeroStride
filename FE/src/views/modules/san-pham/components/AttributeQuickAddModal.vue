<script setup>
import { ref, reactive, watch } from 'vue';
import { XIcon, CheckIcon, DeviceFloppyIcon } from 'vue-tabler-icons';
import { useNotifications } from '@/services/notificationService';

const props = defineProps({
  show: Boolean,
  type: String, // 'THUONG_HIEU', 'DANH_MUC', etc.
  title: String,
  service: Object
});

const emit = defineEmits(['update:show', 'saved']);
const { addNotification } = useNotifications();

const loading = ref(false);
const form = reactive({
  ten: '',
  ma: '',
  moTa: ''
});

const resetForm = () => {
    form.ten = '';
    form.ma = '';
    form.moTa = '';
};

watch(() => props.show, (val) => {
    if (val) resetForm();
});

const handleSave = async () => {
    if (!form.ten) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập tên', color: 'error' });
        return;
    }
    
    loading.value = true;
    try {
        const payload = { ...form };
        const result = await props.service.taoEntity(payload);
        addNotification({ title: 'Thành công', subtitle: `Đã thêm ${props.title.toLowerCase()}`, color: 'success' });
        emit('saved', result);
        emit('update:show', false);
    } catch (error) {
        console.error(error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể thêm mới thuộc tính', color: 'error' });
    } finally {
        loading.value = false;
    }
};
</script>

<template>
  <v-dialog :model-value="show" @update:model-value="emit('update:show', $event)" max-width="450" scrollable transition="dialog-bottom-transition">
    <v-card class="rounded-xl overflow-hidden">
      <div class="px-6 py-4 bg-slate-50 border-b">
        <h3 class="font-weight-bold text-slate-800">Thêm {{ title }} mới</h3>
      </div>
      
      <v-card-text class="pa-6">
        <v-row>
            <v-col cols="12">
                <div class="field-label mb-1">Tên {{ title }} *</div>
                <v-text-field v-model="form.ten" placeholder="Nhập tên..." variant="outlined" density="comfortable" hide-details></v-text-field>
            </v-col>
            <v-col cols="12">
                <div class="field-label mb-1">Mã (Tùy chọn)</div>
                <v-text-field v-model="form.ma" placeholder="Hệ thống tự tạo nếu để trống" variant="outlined" density="comfortable" hide-details></v-text-field>
            </v-col>
            <v-col cols="12">
                <div class="field-label mb-1">Mô tả</div>
                <v-textarea v-model="form.moTa" placeholder="Nhập mô tả..." variant="outlined" rows="2" hide-details></v-textarea>
            </v-col>
        </v-row>
      </v-card-text>
      
      <v-card-actions class="pa-6 pt-2 bg-slate-50 border-t d-flex justify-end gap-2">
        <v-btn variant="tonal" color="slate-400" @click="emit('update:show', false)" class="rounded-lg font-weight-bold">Hủy</v-btn>
        <v-btn color="primary" variant="flat" @click="handleSave" :loading="loading" class="rounded-lg font-weight-black">
            <DeviceFloppyIcon size="18" class="mr-1" /> Lưu lại
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.field-label {
    font-size: 13px;
    font-weight: 700;
    color: #64748b;
}
</style>

