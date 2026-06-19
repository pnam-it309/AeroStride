<script setup>
import { ref, onMounted, computed } from 'vue';
import { AdminFilter, AdminTable, AdminBreadcrumbs, AdminPagination } from '@/components/common';
import apiService from '@/services/apiService';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';

const loading = ref(false);
const isRefreshing = ref(false);
const items = ref([]);
const rawShifts = ref([]);
const employeeOptions = ref([]);

// Dialog chấm công thủ công
const showDialog = ref(false);
const isEdit = ref(false);
const editId = ref(null);
const form = ref({
    nhanVienId: null,
    ngay: new Date().toISOString().substr(0, 10),
    gioVao: '',
    gioRa: '',
    ghiChu: ''
});

const filters = ref({
    search: '',
    ngay: null,
    nhanVienId: null
});

const pagination = ref({ page: 1, size: 15, totalElements: 0, totalPages: 0 });

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Chấm công', disabled: true }
];

const tableHeaders = [
    { text: 'STT', width: '55px', align: 'center' },
    { text: 'Mã NV', width: '100px', align: 'center' },
    { text: 'Tên nhân viên', width: '160px', align: 'left' },
    { text: 'Ngày', width: '110px', align: 'center' },
    { text: 'Giờ vào', width: '90px', align: 'center' },
    { text: 'Giờ ra', width: '90px', align: 'center' },
    { text: 'Ca làm', width: '120px', align: 'center' },
    { text: 'Giờ TT', width: '85px', align: 'center' },
    { text: 'Tăng ca', width: '85px', align: 'center' },
    { text: 'Tổng giờ', width: '90px', align: 'center' },
    { text: 'Hành động', width: '100px', align: 'center' }
];

// Tính số phút từ chuỗi HH:MM
const toMinutes = (timeStr) => {
    if (!timeStr) return 0;
    const t = timeStr.substring(0, 5);
    const [h, m] = t.split(':').map(Number);
    return h * 60 + m;
};

// Tính số giờ thực tế (clamp theo ca đăng ký)
const calcActualHours = (row) => {
    if (!row.gioVao || !row.gioRa) return 0;
    const actualMins = toMinutes(row.gioRa) - toMinutes(row.gioVao);
    if (actualMins <= 0) return 0;
    // Lấy giới hạn ca đăng ký
    const shift = rawShifts.value.find(s => s.tenCa === row.ca);
    if (!shift) return parseFloat((actualMins / 60).toFixed(2));
    const shiftMins = toMinutes(shift.gioKetThuc) - toMinutes(shift.gioBatDau);
    return parseFloat((Math.min(actualMins, shiftMins) / 60).toFixed(2));
};

// Tính giờ tăng ca (vượt quá ca đăng ký)
const calcOvertimeHours = (row) => {
    if (!row.gioVao || !row.gioRa) return 0;
    const actualMins = toMinutes(row.gioRa) - toMinutes(row.gioVao);
    if (actualMins <= 0) return 0;
    const shift = rawShifts.value.find(s => s.tenCa === row.ca);
    if (!shift) return 0;
    const shiftMins = toMinutes(shift.gioKetThuc) - toMinutes(shift.gioBatDau);
    const overtime = actualMins - shiftMins;
    return overtime > 0 ? parseFloat((overtime / 60).toFixed(2)) : 0;
};

// Xác định tên ca dựa vào giờ vào
const detectShiftName = (gioVao) => {
    if (!gioVao) return '--';
    for (const shift of rawShifts.value) {
        const startMins = toMinutes(shift.gioBatDau);
        const endMins = toMinutes(shift.gioKetThuc);
        const vaoMins = toMinutes(gioVao);
        // Vào trong vòng 30 phút so với giờ bắt đầu ca
        if (Math.abs(vaoMins - startMins) <= 30 && vaoMins < endMins) {
            return shift.tenCa;
        }
    }
    return '--';
};

// Build danh sách chấm công từ schedules (mock từ dữ liệu lịch làm việc)
const buildAttendanceRows = (schedules) => {
    return schedules.map(s => ({
        id: s.id,
        maNhanVien: s.maNhanVien || 'N/A',
        nhanVien: s.nhanVien,
        nhanVienId: s.nhanVienId,
        ngay: s.ngay,
        gioVao: s.gioVao || '',
        gioRa: s.gioRa || '',
        ca: s.ca || '--',
        trangThai: s.trangThai
    }));
};

const attendanceRows = computed(() => {
    let rows = buildAttendanceRows(items.value);
    if (filters.value.search) {
        const q = filters.value.search.toLowerCase();
        rows = rows.filter(r =>
            (r.nhanVien && r.nhanVien.toLowerCase().includes(q)) ||
            (r.maNhanVien && r.maNhanVien.toLowerCase().includes(q))
        );
    }
    if (filters.value.ngay) {
        rows = rows.filter(r => r.ngay === filters.value.ngay);
    }
    return rows;
});

const paginatedRows = computed(() => {
    const start = (pagination.value.page - 1) * pagination.value.size;
    return attendanceRows.value.slice(start, start + pagination.value.size);
});

const shiftColorMap = {
    'Ca Sáng': 'success',
    'Ca Chiều': 'warning',
    'Ca Tối': 'deep-purple',
};
const getShiftColor = (ca) => shiftColorMap[ca] || 'info';

const formatDate = (d) => {
    if (!d) return '';
    const p = d.split('-');
    return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : d;
};

const loadData = async () => {
    loading.value = true;
    try {
        const [schedRes, shiftRes, empRes] = await Promise.all([
            apiService.get(API_LICH_LAM_VIEC.SCHEDULES),
            apiService.get(API_LICH_LAM_VIEC.SHIFTS),
            apiService.get('/admin/nhan-vien/hien-thi')
        ]);
        if (schedRes.data.success) items.value = schedRes.data.data;
        if (shiftRes.data.success) rawShifts.value = shiftRes.data.data;
        if (empRes.data.success) {
            const raw = empRes.data.data.content || empRes.data.data;
            employeeOptions.value = Array.isArray(raw) ? raw : [];
        }
        pagination.value.totalElements = attendanceRows.value.length;
        pagination.value.totalPages = Math.ceil(attendanceRows.value.length / pagination.value.size);
    } catch (e) {
        console.error(e);
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
    pagination.value.page = 1;
    pagination.value.totalElements = attendanceRows.value.length;
    pagination.value.totalPages = Math.ceil(attendanceRows.value.length / pagination.value.size);
};

// Ghi nhận giờ vào/ra thủ công
const openAddDialog = () => {
    isEdit.value = false;
    editId.value = null;
    form.value = { nhanVienId: null, ngay: new Date().toISOString().substr(0, 10), gioVao: '', gioRa: '', ghiChu: '' };
    showDialog.value = true;
};

const openEditDialog = (row) => {
    isEdit.value = true;
    editId.value = row.id;
    form.value = {
        nhanVienId: row.nhanVienId,
        ngay: row.ngay,
        gioVao: row.gioVao || '',
        gioRa: row.gioRa || '',
        ghiChu: row.ghiChu || ''
    };
    showDialog.value = true;
};

const saveChamCong = async () => {
    if (!form.value.nhanVienId || !form.value.ngay || !form.value.gioVao) {
        alert('Vui lòng nhập đầy đủ thông tin!');
        return;
    }
    loading.value = true;
    try {
        let res;
        if (isEdit.value) {
            res = await apiService.put(`${API_LICH_LAM_VIEC.BASE}/attendance/${editId.value}`, form.value);
        } else {
            res = await apiService.post(`${API_LICH_LAM_VIEC.BASE}/attendance`, form.value);
        }
        if (res.data.success) {
            showDialog.value = false;
            loadData();
        }
    } catch (e) {
        console.error(e);
        alert('Có lỗi xảy ra!');
    } finally {
        loading.value = false;
    }
};

onMounted(loadData);
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <AdminBreadcrumbs :items="breadcrumbs" />
        <div class="mb-3"></div>

        <!-- Filter -->
        <div class="filter-shell mb-4">
            <AdminFilter title="Bộ lọc chấm công" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="5" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm nhân viên</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Tên hoặc mã nhân viên..."
                        variant="outlined" density="compact" hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input"
                        @input="handleFilter"
                    />
                </v-col>
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Lọc theo ngày</div>
                    <v-text-field
                        v-model="filters.ngay"
                        type="date" variant="outlined" density="compact" hide-details
                        class="compact-input"
                        @change="handleFilter"
                    />
                </v-col>
            </AdminFilter>
        </div>

        <!-- Table -->
        <div class="bg-white rounded-xl border overflow-hidden">
            <AdminTable
                title="Bảng chấm công nhân viên"
                :headers="tableHeaders"
                :items="paginatedRows"
                :loading="loading"
                :show-add-button="true"
                addButtonText="Ghi nhận"
                @add="openAddDialog"
            >
                <template #row="{ item, index }">
                    <tr class="data-row">
                        <td class="data-cell text-center">
                            {{ (pagination.page - 1) * pagination.size + index + 1 }}
                        </td>
                        <td class="data-cell text-center">
                            <span class="emp-code-badge">{{ item.maNhanVien }}</span>
                        </td>
                        <td class="data-cell text-left px-3">
                            <span class="font-weight-medium text-slate-800">{{ item.nhanVien }}</span>
                        </td>
                        <td class="data-cell text-center text-slate-600">
                            {{ formatDate(item.ngay) }}
                        </td>
                        <!-- Giờ vào -->
                        <td class="data-cell text-center">
                            <div v-if="item.gioVao" class="time-chip time-in">
                                <v-icon size="11" class="mr-1">mdi-login</v-icon>
                                {{ item.gioVao.substring(0,5) }}
                            </div>
                            <span v-else class="text-slate-400 text-caption">--</span>
                        </td>
                        <!-- Giờ ra -->
                        <td class="data-cell text-center">
                            <div v-if="item.gioRa" class="time-chip time-out">
                                <v-icon size="11" class="mr-1">mdi-logout</v-icon>
                                {{ item.gioRa.substring(0,5) }}
                            </div>
                            <span v-else class="text-slate-400 text-caption">--</span>
                        </td>
                        <!-- Ca làm (auto detect) -->
                        <td class="data-cell text-center">
                            <v-chip
                                v-if="item.ca && item.ca !== '--'"
                                size="small"
                                :color="getShiftColor(item.ca)"
                                variant="flat"
                                class="font-weight-medium"
                            >{{ item.ca }}</v-chip>
                            <span v-else class="text-slate-400 text-caption">--</span>
                        </td>
                        <!-- Số giờ thực tế -->
                        <td class="data-cell text-center">
                            <span v-if="calcActualHours(item) > 0" class="hours-badge actual">
                                {{ calcActualHours(item) }}h
                            </span>
                            <span v-else class="text-slate-400 text-caption">--</span>
                        </td>
                        <!-- Giờ làm thêm -->
                        <td class="data-cell text-center">
                            <span v-if="calcOvertimeHours(item) > 0" class="hours-badge overtime">
                                +{{ calcOvertimeHours(item) }}h
                            </span>
                            <span v-else class="text-slate-400 text-caption">--</span>
                        </td>
                        <!-- Tổng số giờ -->
                        <td class="data-cell text-center">
                            <span v-if="calcActualHours(item) > 0" class="hours-badge total">
                                {{ (calcActualHours(item) + calcOvertimeHours(item)).toFixed(2) }}h
                            </span>
                            <span v-else class="text-slate-400 text-caption">--</span>
                        </td>
                        <!-- Hành động -->
                        <td class="data-cell action-cell">
                            <div class="action-controls">
                                <v-btn variant="text" color="primary" size="small" class="action-icon-btn" @click="openEditDialog(item)">
                                    <v-icon size="17">mdi-clock-edit-outline</v-icon>
                                    <v-tooltip activator="parent" location="top">Ghi giờ</v-tooltip>
                                </v-btn>
                            </div>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination
                        v-model="pagination.page"
                        :page-size="pagination.size"
                        :total-pages="pagination.totalPages"
                        :total-elements="pagination.totalElements"
                        @change="handleFilter"
                    />
                </template>
            </AdminTable>
        </div>

        <!-- Summary Cards -->
        <v-row class="mt-4" v-if="attendanceRows.length > 0">
            <v-col cols="6" md="3">
                <div class="summary-card summary-total">
                    <div class="summary-icon"><v-icon color="primary">mdi-account-group</v-icon></div>
                    <div class="summary-info">
                        <div class="summary-value">{{ attendanceRows.length }}</div>
                        <div class="summary-label">Tổng bản ghi</div>
                    </div>
                </div>
            </v-col>
            <v-col cols="6" md="3">
                <div class="summary-card summary-present">
                    <div class="summary-icon"><v-icon color="success">mdi-check-circle</v-icon></div>
                    <div class="summary-info">
                        <div class="summary-value">{{ attendanceRows.filter(r => r.gioVao).length }}</div>
                        <div class="summary-label">Đã chấm công</div>
                    </div>
                </div>
            </v-col>
            <v-col cols="6" md="3">
                <div class="summary-card summary-overtime">
                    <div class="summary-icon"><v-icon color="orange">mdi-clock-plus</v-icon></div>
                    <div class="summary-info">
                        <div class="summary-value">{{ attendanceRows.filter(r => calcOvertimeHours(r) > 0).length }}</div>
                        <div class="summary-label">Có tăng ca</div>
                    </div>
                </div>
            </v-col>
            <v-col cols="6" md="3">
                <div class="summary-card summary-missing">
                    <div class="summary-icon"><v-icon color="error">mdi-account-off</v-icon></div>
                    <div class="summary-info">
                        <div class="summary-value">{{ attendanceRows.filter(r => !r.gioVao).length }}</div>
                        <div class="summary-label">Chưa chấm</div>
                    </div>
                </div>
            </v-col>
        </v-row>

        <!-- Dialog ghi nhận giờ -->
        <v-dialog v-model="showDialog" max-width="480">
            <v-card class="rounded-xl pa-2">
                <v-card-title class="text-h6 font-weight-bold d-flex align-center pa-4">
                    <v-icon color="primary" class="mr-3">mdi-clock-check-outline</v-icon>
                    {{ isEdit ? 'Cập nhật giờ làm' : 'Ghi nhận chấm công' }}
                </v-card-title>
                <v-card-text>
                    <v-row dense>
                        <v-col cols="12">
                            <div class="filter-field-label">Nhân viên</div>
                            <v-autocomplete
                                v-model="form.nhanVienId"
                                :items="employeeOptions"
                                :item-title="(i) => `${i.ma} - ${i.ten}`"
                                item-value="id"
                                placeholder="Chọn nhân viên"
                                variant="outlined" density="compact" hide-details
                            />
                        </v-col>
                        <v-col cols="12">
                            <div class="filter-field-label">Ngày làm</div>
                            <v-text-field v-model="form.ngay" type="date" variant="outlined" density="compact" hide-details />
                        </v-col>
                        <v-col cols="6">
                            <div class="filter-field-label">Giờ vào</div>
                            <v-text-field v-model="form.gioVao" type="time" variant="outlined" density="compact" hide-details />
                        </v-col>
                        <v-col cols="6">
                            <div class="filter-field-label">Giờ ra</div>
                            <v-text-field v-model="form.gioRa" type="time" variant="outlined" density="compact" hide-details />
                        </v-col>
                        <v-col cols="12" v-if="form.gioVao && form.gioRa">
                            <v-alert type="info" variant="tonal" density="compact" class="rounded-lg text-caption">
                                <strong>Ca nhận diện:</strong> {{ detectShiftName(form.gioVao) }}
                            </v-alert>
                        </v-col>
                        <v-col cols="12">
                            <div class="filter-field-label">Ghi chú</div>
                            <v-textarea v-model="form.ghiChu" placeholder="Ghi chú (nếu có)..." variant="outlined" density="compact" rows="2" hide-details />
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-card-actions class="pa-4">
                    <v-spacer />
                    <v-btn variant="text" color="grey" @click="showDialog = false">Hủy</v-btn>
                    <v-btn color="primary" variant="flat" class="px-6 rounded-lg" @click="saveChamCong">Lưu lại</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
.emp-code-badge {
    display: inline-block;
    background: #f1f5f9;
    color: #475569;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 6px;
    border: 1px solid #e2e8f0;
}

.time-chip {
    display: inline-flex;
    align-items: center;
    font-size: 12px;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 6px;
}

.time-in {
    background: #dcfce7;
    color: #16a34a;
}

.time-out {
    background: #fee2e2;
    color: #dc2626;
}

.hours-badge {
    display: inline-block;
    font-size: 12px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 6px;
}

.hours-badge.actual {
    background: #dbeafe;
    color: #1d4ed8;
}

.hours-badge.overtime {
    background: #ffedd5;
    color: #c2410c;
}

.hours-badge.total {
    background: #f0fdf4;
    color: #15803d;
    border: 1px solid #bbf7d0;
}

/* Summary cards */
.summary-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px 20px;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    background: #fff;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.summary-icon {
    width: 42px;
    height: 42px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f8fafc;
}

.summary-total .summary-icon { background: #eff6ff; }
.summary-present .summary-icon { background: #f0fdf4; }
.summary-overtime .summary-icon { background: #fff7ed; }
.summary-missing .summary-icon { background: #fef2f2; }

.summary-value {
    font-size: 22px;
    font-weight: 800;
    color: #1e293b;
    line-height: 1.1;
}

.summary-label {
    font-size: 12px;
    color: #94a3b8;
    font-weight: 500;
    margin-top: 2px;
}
</style>
