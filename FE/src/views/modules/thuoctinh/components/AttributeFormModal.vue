<script setup>
import { computed } from 'vue';
import { DeviceFloppyIcon } from 'vue-tabler-icons';

const props = defineProps({
    show: Boolean,
    isEditMode: Boolean,
    title: String,
    selectedTab: String,
    form: Object,
    activator: Object
});

const emit = defineEmits(['update:show', 'update:form', 'save']);

const closeModal = () => emit('update:show', false);
const handleSave = () => emit('save');
const updateFormField = (field, value) => {
    emit('update:form', { ...props.form, [field]: value });
};

const submitButtonText = computed(() => (props.isEditMode ? 'Cập nhật' : 'Thêm'));
const headerTitle = computed(() => (props.isEditMode ? 'Cập nhật' : 'Thêm mới') + ' ' + props.title.toLowerCase());
</script>

<template>
    <v-menu 
        :model-value="show" 
        @update:model-value="emit('update:show', $event)"
        :activator="activator"
        :open-on-click="false"
        :close-on-content-click="false"
        location="bottom end"
        :offset="[10, 40]"
        transition="scale-transition"
        min-width="480"
        max-width="530"
    >
        <v-card class="attribute-popover-card rounded-xl border-0 shadow-2xl overflow-hidden">
            <div class="px-6 py-4 border-b bg-slate-50/80 d-flex align-center justify-space-between">
                <h3 class="text-subtitle-1 font-weight-bold text-slate-900 mb-0">
                    {{ headerTitle }}
                </h3>
            </div>

            <v-card-text class="pa-6">
                <v-form v-if="form" @submit.prevent="handleSave">
                    <v-row dense>
                        <v-col cols="12">
                            <div class="form-group mb-4">
                                <label class="popover-label">Mã {{ title }} <span class="text-lowercase text-slate-400">(Tự động)</span></label>
                                <v-text-field
                                    :model-value="form.ma"
                                    readonly
                                    placeholder="Hệ thống tự tạo..."
                                    variant="outlined"
                                    density="compact"
                                    hide-details
                                    class="modern-input bg-slate-50"
                                ></v-text-field>
                            </div>

                            <div class="form-group mb-4">
                                <label class="popover-label">Tên {{ title }}</label>
                                <v-text-field
                                    :model-value="form.ten"
                                    @update:model-value="updateFormField('ten', $event)"
                                    placeholder="Nhập tên..."
                                    variant="outlined"
                                    density="compact"
                                    hide-details
                                    autofocus
                                    class="modern-input"
                                ></v-text-field>
                            </div>

                            <!-- Color Specific -->
                            <div v-if="selectedTab === 'colors'" class="form-group mb-4">
                                <label class="popover-label">Mã màu (Hex)</label>
                                <div class="d-flex align-center gap-2">
                                    <v-text-field
                                        :model-value="form.maMauHex"
                                        @update:model-value="updateFormField('maMauHex', $event)"
                                        variant="outlined"
                                        density="compact"
                                        hide-details
                                        class="modern-input flex-grow-1"
                                    ></v-text-field>
                                    <div class="color-preview-wrapper shadow-sm">
                                        <input
                                            type="color"
                                            :value="form.maMauHex"
                                            @input="updateFormField('maMauHex', $event.target.value)"
                                            class="color-picker-input-hidden"
                                        />
                                        <div class="color-preview-circle" :style="{ backgroundColor: form.maMauHex }"></div>
                                    </div>
                                </div>
                            </div>

                            <!-- Size Specific -->
                            <div v-if="selectedTab === 'sizes'" class="form-group mb-4">
                                <label class="popover-label">Giá trị (Số)</label>
                                <v-text-field
                                    :model-value="form.giaTriKichThuoc"
                                    @update:model-value="updateFormField('giaTriKichThuoc', $event)"
                                    type="number"
                                    placeholder="Ví dụ: 42"
                                    variant="outlined"
                                    density="compact"
                                    hide-details
                                    class="modern-input"
                                ></v-text-field>
                            </div>

                            <div class="form-group">
                                <label class="popover-label">Mô tả</label>
                                <v-textarea
                                    :model-value="form.moTa"
                                    @update:model-value="updateFormField('moTa', $event)"
                                    placeholder="Nhập mô tả thêm..."
                                    variant="outlined"
                                    rows="2"
                                    density="compact"
                                    class="modern-input"
                                    hide-details
                                ></v-textarea>
                            </div>
                        </v-col>
                    </v-row>
                </v-form>
            </v-card-text>

            <div class="px-6 py-4 bg-slate-50 border-t d-flex justify-end gap-2">
                <v-btn variant="text" color="slate-600" class="text-none px-4" @click="closeModal">
                    Hủy
                </v-btn>
                <v-btn
                    color="primary"
                    variant="flat"
                    class="text-none px-6 shadow-sm"
                    @click="handleSave"
                    :disabled="!form?.ten"
                >
                    <template #prepend>
                        <DeviceFloppyIcon size="16" />
                    </template>
                    {{ submitButtonText }}
                </v-btn>
            </div>
        </v-card>
    </v-menu>
</template>

<style scoped>
.attribute-popover-card {
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04) !important;
}

.popover-label {
    font-size: 12px;
    font-weight: 600;
    color: #64748b;
    margin-bottom: 6px;
    display: block;
    padding-left: 4px;
}

.gap-2 { gap: 8px; }

:deep(.modern-input .v-field) {
    border-radius: 10px !important;
    background-color: #ffffff !important;
    font-size: 13px !important;
}

:deep(.v-field__outline) {
    --v-field-border-opacity: 0.15 !important;
}

:deep(.v-field--focused .v-field__outline) {
    --v-field-border-opacity: 1 !important;
    --v-field-border-width: 1px !important;
}

.color-preview-wrapper {
    position: relative;
    width: 40px;
    height: 40px;
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid #e2e8f0;
    cursor: pointer;
}

.color-picker-input-hidden {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
    cursor: pointer;
    z-index: 2;
}

.color-preview-circle {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
}

:deep(.v-btn) {
    border-radius: 8px !important;
    font-weight: 600;
}
</style>

