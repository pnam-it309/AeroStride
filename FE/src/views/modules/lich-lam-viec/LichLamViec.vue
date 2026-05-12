<script setup>
import { ref, onMounted, computed } from 'vue';
import { AdminFilter, AdminTable, AdminBreadcrumbs, AdminPagination } from '@/components/common';
import { CalendarIcon } from 'vue-tabler-icons';
import apiService from '@/services/apiService';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';

const loading = ref(false);
const isRefreshing = ref(false);
const items = ref([]);
const shiftOptions = ref(['Tất cả']);
const employeeOptions = ref([]);
const rawShifts = ref([]);
const viewMode = ref('table'); // 'table', 'month', 'week'

const filters = ref({
    search: '',
    ca: null,
    ngay: null
});

// Add Dialog State
const showAddDialog = ref(false);
const addForm = ref({
    nhanVien: [],
    ca: null,
    ngay: new Date().toISOString().substr(0, 10),
    trangThai: 'CHO_XAC_NHAN'
});

// Import Preview State
const showImportPreview = ref(false);
const importPreviewData = ref([]);

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch làm việc', disabled: true }
];

const tableHeaders = [
    { text: 'STT', width: '50px', align: 'center' },
    { text: 'Nhân viên', width: '200px', align: 'left' },
    { text: 'Ca làm việc', width: '150px', align: 'center' },
    { text: 'Ngày làm', width: '150px', align: 'center' },
    { text: 'Trạng thái', width: '150px', align: 'center' },
    { text: 'Hành động', width: '100px', align: 'center' }
];

const loadData = async () => {
    loading.value = true;
    try {
        const [scheduleRes, shiftRes, empRes] = await Promise.all([
            apiService.get(API_LICH_LAM_VIEC.SCHEDULES),
            apiService.get(API_LICH_LAM_VIEC.SHIFTS),
            apiService.get('/admin/nhan-vien/hien-thi')
        ]);

        if (scheduleRes.data.success) {
            items.value = scheduleRes.data.data;
        }
        
        if (shiftRes.data.success) {
            rawShifts.value = shiftRes.data.data;
            shiftOptions.value = ['Tất cả', ...shiftRes.data.data.map(s => s.tenCa)];
        }

        if (empRes.data.success) {
            const rawContent = empRes.data.data.content || empRes.data.data;
            employeeOptions.value = Array.isArray(rawContent) ? rawContent : [];
        }
    } catch (error) {
        console.error('Error fetching data:', error);
    } finally {
        loading.value = false;
    }
};

const handleAdd = () => {
    addForm.value.nhanVien = [];
    addForm.value.ngay = new Date().toISOString().substr(0, 10);
    showAddDialog.value = true;
};

const handleDayClick = (dayObj) => {
    if (dayObj && dayObj.date) {
        addForm.value.nhanVien = [];
        addForm.value.ngay = dayObj.date;
        showAddDialog.value = true;
    }
};

const saveSchedule = async () => {
    try {
        if (!addForm.value.nhanVien.length || !addForm.value.ca || !addForm.value.ngay) {
            alert('Vui lòng nhập đầy đủ thông tin!');
            return;
        }
        loading.value = true;
        const res = await apiService.post(API_LICH_LAM_VIEC.BASE + '/schedules', addForm.value);
        if (res.data.success) {
            alert('Thêm lịch thành công!');
            showAddDialog.value = false;
            loadData();
        }
    } catch (error) {
        console.error('Save error:', error);
    } finally {
        loading.value = false;
    }
};

const handleRefresh = async () => {
    isRefreshing.value = true;
    await loadData();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleFilter = () => {
    console.log('Filtering...', filters.value);
};

const handleDownloadTemplate = async () => {
    try {
        const response = await apiService.get(API_LICH_LAM_VIEC.BASE + '/export-template', { responseType: 'blob' });
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'mau_lich_lam_viec.xlsx');
        document.body.appendChild(link);
        link.click();
    } catch (error) {
        console.error('Error downloading template:', error);
    }
};

const handleImport = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.xlsx, .xls';
    input.onchange = async (e) => {
        const file = e.target.files[0];
        if (file) {
            const formData = new FormData();
            formData.append('file', file);
            try {
                loading.value = true;
                const response = await apiService.post(API_LICH_LAM_VIEC.BASE + '/preview-import', formData);
                if (response.data.success) {
                    importPreviewData.value = response.data.data;
                    showImportPreview.value = true;
                }
            } catch (error) {
                console.error('Error previewing import:', error);
                alert('Lỗi khi đọc file Excel!');
            } finally {
                loading.value = false;
            }
        }
    };
    input.click();
};

const confirmImport = async () => {
    try {
        loading.value = true;
        const response = await apiService.post(API_LICH_LAM_VIEC.BASE + '/confirm-import', importPreviewData.value);
        if (response.data.success) {
            alert(response.data.data);
            showImportPreview.value = false;
            loadData();
        }
    } catch (error) {
        console.error('Error confirming import:', error);
    } finally {
        loading.value = false;
    }
};

// Calendar Logic
const currentMonth = ref(new Date());

const daysInMonth = computed(() => {
    const year = currentMonth.value.getFullYear();
    const month = currentMonth.value.getMonth();
    
    if (viewMode.value === 'month') {
        const firstDay = new Date(year, month, 1).getDay();
        const days = new Date(year, month + 1, 0).getDate();
        const result = [];
        // Padding for start of month
        for (let i = 0; i < (firstDay === 0 ? 6 : firstDay - 1); i++) {
            result.push({ date: null });
        }
        for (let i = 1; i <= days; i++) {
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
            const schedules = items.value.filter(s => s.ngay === dateStr);
            result.push({ day: i, date: dateStr, schedules });
        }
        return result;
    } else if (viewMode.value === 'week') {
        const result = [];
        const today = new Date(currentMonth.value);
        const dayOfWeek = today.getDay() || 7; // 1-7
        const startOfWeek = new Date(today);
        startOfWeek.setDate(today.getDate() - dayOfWeek + 1);
        
        for (let i = 0; i < 7; i++) {
            const d = new Date(startOfWeek);
            d.setDate(d.getDate() + i);
            const dateStr = d.toISOString().substr(0, 10);
            const schedules = items.value.filter(s => s.ngay === dateStr);
            result.push({ day: d.getDate(), date: dateStr, schedules });
        }
        return result;
    }
    return [];
});

const nextPeriod = () => {
    const d = new Date(currentMonth.value);
    if (viewMode.value === 'month') {
        d.setMonth(d.getMonth() + 1);
    } else {
        d.setDate(d.getDate() + 7);
    }
    currentMonth.value = d;
};

const prevPeriod = () => {
    const d = new Date(currentMonth.value);
    if (viewMode.value === 'month') {
        d.setMonth(d.getMonth() - 1);
    } else {
        d.setDate(d.getDate() - 7);
    }
    currentMonth.value = d;
};

onMounted(() => {
    loadData();
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body" style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important">
        <AdminBreadcrumbs :items="breadcrumbs" />
        
        <div class="mb-2"></div>

        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc lịch làm việc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm nhân viên</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Tên nhân viên..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        @input="handleFilter"
                    />
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Ca làm việc</div>
                    <v-select
                        v-model="filters.ca"
                        :items="shiftOptions"
                        variant="outlined"
                        density="compact"
                        hide-details
                        @update:model-value="handleFilter"
                    />
                </v-col>
            </AdminFilter>
        </div>

        <div v-if="viewMode === 'table'" class="flex-grow-1 overflow-hidden d-flex flex-column">
            <AdminTable
                title="Bảng phân lịch làm việc"
                :headers="tableHeaders"
                :items="items"
                :loading="loading"
                :show-add-button="true"
                addButtonText="Tạo mới"
                showTemplateButton
                showImportButton
                @add="handleAdd"
                @downloadTemplate="handleDownloadTemplate"
                @import="handleImport"
            >
                <template #extra-actions>
                    <v-btn-toggle v-model="viewMode" mandatory color="primary" density="compact" variant="flat" class="rounded-lg border">
                        <v-btn value="table" class="px-4">Dạng bảng</v-btn>
                        <v-btn value="month" class="px-4">Theo tháng</v-btn>
                        <v-btn value="week" class="px-4">Theo tuần</v-btn>
                    </v-btn-toggle>
                </template>
                <template #row="{ item, index }">
                    <tr class="data-row">
                        <td class="data-cell text-center">{{ index + 1 }}</td>
                        <td class="data-cell text-left font-weight-bold">{{ item.nhanVien }}</td>
                        <td class="data-cell text-center">
                            <v-chip size="small" color="info" variant="tonal">{{ item.ca }}</v-chip>
                        </td>
                        <td class="data-cell text-center">{{ item.ngay }}</td>
                        <td class="data-cell text-center">
                            <v-chip
                                size="small"
                                :color="item.trangThai === 'DA_XAC_NHAN' ? 'success' : 'warning'"
                                variant="flat"
                            >
                                {{ item.trangThai === 'DA_XAC_NHAN' ? 'Đã xác nhận' : 'Chờ xác nhận' }}
                            </v-chip>
                        </td>
                        <td class="data-cell text-center action-cell">
                            <div class="action-controls">
                                <v-btn icon="mdi-pencil-outline" variant="text" color="primary" class="action-icon-btn" size="small"></v-btn>
                                <v-btn icon="mdi-delete-outline" variant="text" color="error" class="action-icon-btn" size="small"></v-btn>
                            </div>
                        </td>
                    </tr>
                </template>
            </AdminTable>
        </div>

        <div v-else class="flex-grow-1 overflow-hidden d-flex flex-column bg-white rounded-xl border">
            <!-- Toolbar for Calendar View to match AdminTable style -->
            <div class="table-toolbar d-flex align-center justify-space-between pa-3 border-b">
                <div class="d-flex align-center">
                    <v-icon color="primary" class="mr-3">mdi-calendar-range</v-icon>
                    <h3 class="text-h6 font-weight-medium text-dark tracking-tight">
                        {{ viewMode === 'month' ? 'Lịch làm việc tháng' : 'Lịch làm việc tuần' }}
                    </h3>
                </div>
                <div class="d-flex align-center gap-2">
                    <v-btn-toggle v-model="viewMode" mandatory color="primary" density="compact" variant="flat" class="rounded-lg border">
                        <v-btn value="table" class="px-4">Dạng bảng</v-btn>
                        <v-btn value="month" class="px-4">Theo tháng</v-btn>
                        <v-btn value="week" class="px-4">Theo tuần</v-btn>
                    </v-btn-toggle>
                    <v-btn color="primary" prepend-icon="mdi-plus" variant="flat" @click="handleAdd" class="rounded-lg ml-2">Tạo mới</v-btn>
                </div>
            </div>

            <div class="pa-4 flex-grow-1 d-flex flex-column overflow-hidden">
                <div class="d-flex align-center justify-center mb-4">
                    <div class="d-flex align-center gap-4">
                        <v-btn icon="mdi-chevron-left" variant="text" @click="prevPeriod"></v-btn>
                        <div class="text-subtitle-1 font-weight-black text-primary px-4" style="min-width: 180px; text-align: center;">
                            {{ viewMode === 'month' ? `Tháng ${currentMonth.getMonth() + 1} / ${currentMonth.getFullYear()}` : `Tuần ngày ${currentMonth.getDate()}/${currentMonth.getMonth()+1}` }}
                        </div>
                        <v-btn icon="mdi-chevron-right" variant="text" @click="nextPeriod"></v-btn>
                    </div>
                </div>

                <div class="calendar-container flex-grow-1 overflow-y-auto">
                    <div class="calendar-header-row mb-2">
                        <div v-for="day in ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']" :key="day" class="day-header">
                            {{ day }}
                        </div>
                    </div>
                    <div class="calendar-grid-body">
                        <div v-for="(dayObj, idx) in daysInMonth" :key="idx" 
                            class="calendar-day-cell clickable-day"
                            :class="{ 'empty-cell': !dayObj.day, 'today-cell': dayObj.date === new Date().toISOString().substr(0, 10) }"
                            @click="handleDayClick(dayObj)">
                            <div v-if="dayObj.day" class="d-flex justify-space-between align-center mb-1">
                                <span class="day-number">{{ dayObj.day }}</span>
                            </div>
                            <div v-if="dayObj.schedules && dayObj.schedules.length > 0" class="schedule-items">
                                <div v-for="s in dayObj.schedules" :key="s.id" class="mini-schedule">
                                    <span class="dot" :class="s.trangThai === 'DA_XAC_NHAN' ? 'bg-success' : 'bg-warning'"></span>
                                    <span class="name text-truncate">{{ s.nhanVien }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Add Dialog -->
        <v-dialog v-model="showAddDialog" max-width="500">
            <v-card class="rounded-xl pa-4">
                <v-card-title class="text-h6 font-weight-bold d-flex align-center">
                    <v-icon color="primary" class="mr-3">mdi-plus-circle</v-icon>
                    Thêm lịch làm việc
                </v-card-title>
                <v-card-text>
                    <v-row>
                        <v-col cols="12">
                            <div class="filter-field-label">Nhân viên (Chọn nhiều)</div>
                            <v-select
                                v-model="addForm.nhanVien"
                                :items="employeeOptions"
                                item-title="ten"
                                item-value="ten"
                                placeholder="Chọn nhân viên"
                                variant="outlined"
                                density="compact"
                                multiple
                                chips
                                closable-chips
                                hide-details
                            />
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="filter-field-label">Ngày làm</div>
                            <v-text-field
                                v-model="addForm.ngay"
                                type="date"
                                variant="outlined"
                                density="compact"
                                hide-details
                            />
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="filter-field-label">Ca làm</div>
                            <v-select
                                v-model="addForm.ca"
                                :items="rawShifts"
                                item-title="tenCa"
                                item-value="tenCa"
                                placeholder="Chọn ca"
                                variant="outlined"
                                density="compact"
                                hide-details
                            />
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-card-actions class="pa-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" color="grey" @click="showAddDialog = false">Hủy</v-btn>
                    <v-btn color="primary" variant="flat" @click="saveSchedule" class="px-6 rounded-lg">Lưu lại</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Import Preview Dialog -->
        <v-dialog v-model="showImportPreview" max-width="800">
            <v-card class="rounded-xl pa-0 overflow-hidden">
                <div class="pa-4 bg-primary text-white d-flex align-center">
                    <v-icon color="white" class="mr-3">mdi-file-eye-outline</v-icon>
                    <h3 class="text-h6 font-weight-bold mb-0">Kiểm tra dữ liệu Excel</h3>
                    <v-spacer></v-spacer>
                    <v-btn icon="mdi-close" variant="text" color="white" @click="showImportPreview = false"></v-btn>
                </div>
                
                <v-card-text class="pa-4">
                    <v-alert type="info" variant="tonal" density="compact" class="mb-4 text-caption rounded-lg">
                        Vui lòng kiểm tra kỹ danh sách bên dưới trước khi xác nhận lưu vào hệ thống.
                    </v-alert>

                    <div class="preview-table-wrapper border rounded-lg">
                        <table class="native-admin-table w-100">
                            <thead>
                                <tr>
                                    <th class="header-cell text-center" style="width: 50px">STT</th>
                                    <th class="header-cell text-left">Nhân viên</th>
                                    <th class="header-cell text-center">Ca làm</th>
                                    <th class="header-cell text-center">Ngày làm</th>
                                    <th class="header-cell text-center">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(row, idx) in importPreviewData" :key="idx" class="data-row">
                                    <td class="data-cell text-center">{{ idx + 1 }}</td>
                                    <td class="data-cell text-left font-weight-bold">{{ row.nhanVien }}</td>
                                    <td class="data-cell text-center">
                                        <v-chip size="x-small" color="info" variant="tonal">{{ row.ca }}</v-chip>
                                    </td>
                                    <td class="data-cell text-center">{{ row.ngay }}</td>
                                    <td class="data-cell text-center">
                                        <v-chip size="x-small" :color="row.status === 'VALID' ? 'success' : 'error'" variant="flat">
                                            {{ row.status === 'VALID' ? 'Hợp lệ' : 'Lỗi' }}
                                        </v-chip>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </v-card-text>

                <v-card-actions class="pa-4 border-t bg-slate-50">
                    <v-spacer></v-spacer>
                    <v-btn variant="outlined" color="grey" @click="showImportPreview = false" class="rounded-lg">Hủy bỏ</v-btn>
                    <v-btn color="primary" variant="flat" @click="confirmImport" class="px-6 rounded-lg ml-2">Xác nhận lưu</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
.calendar-container {
    display: flex;
    flex-direction: column;
}

.calendar-header-row {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
}

.day-header {
    text-align: center;
    font-size: 12px;
    font-weight: 700;
    color: #94a3b8;
    padding: 8px;
    text-transform: uppercase;
}

.calendar-grid-body {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
}

.calendar-day-cell {
    min-height: 120px;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 10px;
    background: #fff;
    transition: all 0.2s ease;
    display: flex;
    flex-direction: column;
}

.clickable-day {
    cursor: pointer;
}

.clickable-day:hover {
    border-color: #1e257c;
    background-color: #f8fbff;
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    transform: translateY(-2px);
}

.empty-cell {
    background: #f8fafc;
    border-style: dashed;
    opacity: 0.5;
}

.today-cell {
    border: 2px solid #1e257c;
    background: #f0f1ff;
}

.day-number {
    font-size: 14px;
    font-weight: 800;
    color: #475569;
}

.today-cell .day-number {
    color: #1e257c;
}

.schedule-items {
    margin-top: 4px;
    display: flex;
    flex-direction: column;
    gap: 4px;
    overflow-y: auto;
    max-height: 80px;
}

.mini-schedule {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    background: #f1f5f9;
    padding: 4px 8px;
    border-radius: 6px;
    white-space: nowrap;
}

.mini-schedule .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;
}

.mini-schedule .name {
    font-weight: 600;
    color: #334155;
}

.bg-success { background-color: #22c55e !important; }
.bg-warning { background-color: #eab308 !important; }

.preview-table-wrapper {
    max-height: 400px;
    overflow-y: auto;
}
</style>