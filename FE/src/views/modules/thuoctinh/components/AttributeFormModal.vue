<script setup>
import { computed } from 'vue';
import { XIcon, DeviceFloppyIcon } from 'vue-tabler-icons';

const props = defineProps({
    show: Boolean,
    isEditMode: Boolean,
    title: String,
    selectedTab: String,
    form: Object
});

const emit = defineEmits(['update:show', 'update:form', 'save']);

const closeModal = () => emit('update:show', false);
const handleSave = () => emit('save');
const updateFormField = (field, value) => {
    emit('update:form', { ...props.form, [field]: value });
};

const submitButtonText = computed(() => (props.isEditMode ? 'Cập nhật' : 'Thêm') + ' ' + props.title.toLowerCase());
const headerTitle = computed(() => (props.isEditMode ? 'Cập nhật' : 'Thêm mới') + ' ' + props.title.toLowerCase());
</script>

<template>
    <v-dialog :model-value="show" @update:model-value="emit('update:show', $event)" max-width="750" persistent transition="dialog-bottom-transition">
        <v-card class="rounded-xl border shadow-2xl overflow-hidden p-0">
            <v-card-title class="px-8 py-6 border-b bg-slate-50/50">
                <div>
                    <h3 class="text-h6 text-slate-900 mb-0">
                        {{ headerTitle }}
                    </h3>
                </div>
            </v-card-title>

            <v-card-text class="pa-4">
                <v-form v-if="form">
                    <v-row>
                        <v-col cols="12">
                            <div class="form-group mb-5">
                                <div class="mb-2 px-1">
                                    <span class="text-slate-700" style="font-size: 13px !important;">Mã {{ title }} <span class="text-lowercase text-slate-400">(Tự động)</span></span>
                                </div>
                                <v-text-field
                                    :model-value="form.ma"
                                    readonly
                                    placeholder="Hệ thống tự tạo..."
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    class="modern-input bg-slate-50"
                                    style="font-size: 13px !important;"
                                ></v-text-field>
                            </div>

                            <div class="form-group mb-5">
                                <div class="mb-2 px-1">
                                    <span class="text-slate-700" style="font-size: 13px !important;">Tên {{ title }}</span>
                                </div>
                                <v-text-field
                                    :model-value="form.ten"
                                    @update:model-value="updateFormField('ten', $event)"
                                    placeholder="Nhập tên..."
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    class="modern-input"
                                    style="font-size: 13px !important;"
                                ></v-text-field>
                            </div>

                            <!-- Color Specific -->
                            <div v-if="selectedTab === 'colors'" class="form-group mb-5">
                                <div class="mb-2 px-1">
                                    <span class="text-slate-700" style="font-size: 13px !important;">Mã màu (Hex)</span>
                                </div>
                                <div class="d-flex align-center gap-3">
                                    <v-text-field
                                        :model-value="form.maMauHex"
                                        @update:model-value="updateFormField('maMauHex', $event)"
                                        variant="outlined"
                                        density="comfortable"
                                        hide-details
                                        class="modern-input flex-grow-1"
                                        style="font-size: 13px !important;"
                                    ></v-text-field>
                                    <input
                                        type="color"
                                        :value="form.maMauHex"
                                        @input="updateFormField('maMauHex', $event.target.value)"
                                        class="color-picker-input"
                                    />
                                </div>
                            </div>

                            <!-- Size Specific -->
                            <div v-if="selectedTab === 'sizes'" class="form-group mb-5">
                                <div class="mb-2 px-1">
                                    <span class="text-slate-700" style="font-size: 13px !important;">Giá trị (Số)</span>
                                </div>
                                <v-text-field
                                    :model-value="form.giaTriKichThuoc"
                                    @update:model-value="updateFormField('giaTriKichThuoc', $event)"
                                    type="number"
                                    placeholder="Ví dụ: 42"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    class="modern-input"
                                    style="font-size: 13px !important;"
                                ></v-text-field>
                            </div>

                            <div class="form-group">
                                <div class="mb-2 px-1">
                                    <span class="text-slate-700" style="font-size: 13px !important;">Mô tả</span>
                                </div>
                                <v-textarea
                                    :model-value="form.moTa"
                                    @update:model-value="updateFormField('moTa', $event)"
                                    placeholder="Nhập mô tả thêm..."
                                    variant="outlined"
                                    rows="3"
                                    class="modern-input"
                                    style="font-size: 13px !important;"
                                    hide-details
                                ></v-textarea>
                            </div>
                        </v-col>
                    </v-row>
                </v-form>
            </v-card-text>

            <v-divider></v-divider>

            <v-card-actions class="px-8 py-6 bg-slate-50 d-flex justify-end gap-3">
                <v-btn variant="tonal" color="slate-500" class="px-6 h-11" style="font-size: 13px !important;" @click="closeModal">
                    Hủy bỏ
                </v-btn>
                <v-btn
                    color="primary"
                    variant="flat"
                    class="px-8 h-11"
                    style="font-size: 13px !important;"
                    @click="handleSave"
                    :disabled="!form?.ten"
                >
                    <template #prepend>
                        <DeviceFloppyIcon size="18" />
                    </template>
                    {{ submitButtonText }}
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.accent-bar { height: 6px; }
.shadow-2xl { box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); }
.gap-3 { gap: 12px; }

:deep(.modern-input .v-field) {
    border-radius: 12px !important;
    background-color: #fcfdfe !important;
}

:deep(.v-field__input::placeholder),
:deep(.v-field__input),
:deep(.v-field__input input),
:deep(.v-textarea textarea) {
    font-size: 13px !important;
}

:deep(.v-field--focused .v-field__outline) {
    --v-field-border-width: 2px;
}

.color-picker-input {
    width: 48px;
    height: 48px;
    border: 2px solid #e2e8f0;
    border-radius: 12px;
    padding: 2px;
    cursor: pointer;
    background: white;
}

:deep(.v-btn) {
    letter-spacing: 0.02em;
    text-transform: none;
}
</style>

