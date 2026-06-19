<script setup>
import { ref, onMounted, computed } from 'vue';
import { AdminFilter, AdminTable, AdminBreadcrumbs, AdminPagination, TableEmptyState } from '@/components/common';
import AppDatePicker from '@/components/common/AppDatePicker.vue';
import { CalendarIcon } from 'vue-tabler-icons';
import apiService from '@/services/apiService';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';
import { ADMIN_ICONS } from '@/constants/adminIcons';
import { useNotifications } from '@/services/notificationService';

const loading = ref(false);
const isRefreshing = ref(false);
const items = ref([]);
const shiftOptions = ref(['Tất cả']);
const employeeOptions = ref([]);
const rawShifts = ref([]);

// Main and Sub-tab views
const mainTab = ref('table'); // 'table', 'calendar'
const calendarTab = ref('week'); // 'day', 'week', 'month'

const filters = ref({
    search: '',
    ca: null,
    ngay: null
});

// Add Dialog State
const showAddDialog = ref(false);
const ngayRef = ref(null);
const openDatePicker = (fieldRef) => {
    const input = fieldRef?.$el?.querySelector('input[type="date"]');
    if (input) {
        if (typeof input.showPicker === 'function') {
            input.showPicker();
        } else {
            input.click();
        }
    }
};
const addForm = ref({
    nhanVien: [],
    ca: [],
    ngay: new Date().toISOString().substr(0, 10),
    trangThai: 'CHO_XAC_NHAN',
    tangCa: false,
    gioBatDauTangCa: '',
    gioKetThucTangCa: ''
});

// Import Preview State
const showImportPreview = ref(false);
const importPreviewData = ref([]);

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch làm việc', disabled: true }
];

const tableHeaders = [
    { text: 'STT', width: '60px', align: 'center' },
    { text: 'Mã nhân viên', width: '140px', align: 'center' },
    { text: 'Nhân viên', width: '150px', align: 'center' },
    { text: 'Ca làm', width: '150px', align: 'center' },
    { text: 'Thời gian', width: '150px', align: 'center' },
    { text: 'Ngày làm', width: '150px', align: 'center' },
    { text: 'Hành động', width: '100px', align: 'center' }
];

// Computed property for filtering schedules
const filteredItems = computed(() => {
    return items.value.filter((item) => {
        // Search filter: matching employee name or employee code
        const empCode = getEmployeeCode(item);
        const matchSearch = !filters.value.search || 
            (item.nhanVien && item.nhanVien.toLowerCase().includes(filters.value.search.toLowerCase())) ||
            (empCode && empCode.toLowerCase().includes(filters.value.search.toLowerCase()));
        
        // Shift filter: matching shift TenCa
        const matchShift = !filters.value.ca || 
            filters.value.ca === 'Tất cả' || 
            item.ca === filters.value.ca;
            
        // Date filter
        const matchDate = !filters.value.ngay || item.ngay === filters.value.ngay;
            
        return matchSearch && matchShift && matchDate;
    });
});

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
            if (items.value.length > 0) {
                const todayStr = new Date().toISOString().substr(0, 10);
                const hasToday = items.value.some(s => s.ngay === todayStr);
                if (!hasToday) {
                    const sorted = [...items.value].sort((a, b) => b.ngay.localeCompare(a.ngay));
                    currentMonth.value = new Date(sorted[0].ngay);
                }
            }
        }

        if (shiftRes.data.success) {
            rawShifts.value = shiftRes.data.data;
            shiftOptions.value = ['Tất cả', ...shiftRes.data.data.map((s) => s.tenCa)];
        }

        if (empRes.data.success) {
            const rawContent = empRes.data.data.content || empRes.data.data;
            const content = Array.isArray(rawContent) ? rawContent : [];
            // Remove duplicates by ID
            const uniqueEmp = [];
            const seenIds = new Set();
            content.forEach((emp) => {
                if (emp.id && !seenIds.has(emp.id)) {
                    uniqueEmp.push(emp);
                    seenIds.add(emp.id);
                }
            });
            employeeOptions.value = uniqueEmp;
        }
    } catch (error) {
        console.error('Error fetching data:', error);
    } finally {
        loading.value = false;
    }
};

const handleAdd = () => {
    isEditSchedule.value = false;
    editScheduleId.value = null;
    addForm.value.nhanVien = [];
    addForm.value.ca = [];
    addForm.value.ngay = new Date().toISOString().substr(0, 10);
    addForm.value.tangCa = false;
    addForm.value.gioBatDauTangCa = '';
    addForm.value.gioKetThucTangCa = '';
    showAddDialog.value = true;
};

const handleDayClick = (dayObj) => {
    if (dayObj && dayObj.date) {
        isEditSchedule.value = false;
        editScheduleId.value = null;
        addForm.value.nhanVien = [];
        addForm.value.ca = [];
        addForm.value.ngay = dayObj.date;
        addForm.value.tangCa = false;
        addForm.value.gioBatDauTangCa = '';
        addForm.value.gioKetThucTangCa = '';
        showAddDialog.value = true;
    }
};

const saveSchedule = async () => {
    try {
        if (!addForm.value.nhanVien.length || !addForm.value.ca || (Array.isArray(addForm.value.ca) && !addForm.value.ca.length) || !addForm.value.ngay) {
            alert('Vui lòng nhập đầy đủ thông tin!');
            return;
        }
        loading.value = true;
        let res;
        
        // Ensure ca is an array for the API
        const submitData = {
            ...addForm.value,
            ca: Array.isArray(addForm.value.ca) ? addForm.value.ca : [addForm.value.ca]
        };
        
        if (isEditSchedule.value && editScheduleId.value) {
            res = await apiService.put(`${API_LICH_LAM_VIEC.BASE}/schedules/${editScheduleId.value}`, submitData);
        } else {
            res = await apiService.post(API_LICH_LAM_VIEC.BASE + '/schedules', submitData);
        }
        
        if (res.data.success) {
            alert(isEditSchedule.value ? 'Cập nhật lịch thành công!' : 'Thêm lịch thành công!');
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

// Calendar Navigation State
const currentMonth = ref(new Date());

// Edit/Delete Schedule States
const isEditSchedule = ref(false);
const editScheduleId = ref(null);

// Lookup helper: Find employee code by name or ID
const getEmployeeCode = (item) => {
    if (!item) return '';
    const empName = typeof item === 'object' ? (item.nhanVien || item.ten) : item;
    const empId = typeof item === 'object' ? item.nhanVienId : null;
    const emp = employeeOptions.value.find(e => e.id === empId || e.ten === empName);
    return emp ? emp.ma : 'N/A';
};

// Lookup helper: Find shift time range (e.g., 08:00 - 12:00) by shift name
const getShiftTimeRange = (caName) => {
    if (!caName) return '';
    const shift = rawShifts.value.find(s => s.tenCa === caName);
    return shift ? `${shift.gioBatDau.substring(0, 5)} - ${shift.gioKetThuc.substring(0, 5)}` : caName;
};

// Helper: CSS class cho chip ca trong matrix table
const getMatrixShiftClass = (caName) => {
    if (!caName) return 'shift-default';
    const name = caName.toLowerCase();
    if (name.includes('sáng') || name.includes('sang')) return 'shift-morning';
    if (name.includes('chiều') || name.includes('chieu')) return 'shift-afternoon';
    if (name.includes('tối') || name.includes('toi') || name.includes('đêm')) return 'shift-night';
    return 'shift-default';
};

// Format date from yyyy-MM-dd to dd/MM/yyyy
const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
};

// Computed property for Monday-to-Sunday dates of the week containing currentMonth
const tableWeekDays = computed(() => {
    const today = new Date(currentMonth.value);
    const day = today.getDay(); // 0 is Sunday, 1 is Monday, ..., 6 is Saturday
    const mondayDiff = day === 0 ? -6 : 1 - day;
    
    const monday = new Date(today);
    monday.setDate(today.getDate() + mondayDiff);
    
    const result = [];
    for (let i = 0; i < 7; i++) {
        const d = new Date(monday);
        d.setDate(monday.getDate() + i);
        const dateStr = d.getFullYear() + '-' + 
            String(d.getMonth() + 1).padStart(2, '0') + '-' + 
            String(d.getDate()).padStart(2, '0');
        result.push(dateStr);
    }
    return result;
});

// Computed label for week range "dd/mm - dd/mm"
const tableWeekLabel = computed(() => {
    if (!tableWeekDays.value.length) return '';
    const monStr = tableWeekDays.value[0];
    const sunStr = tableWeekDays.value[6];
    
    const parseDate = (str) => {
        const parts = str.split('-');
        return `${parts[2]}/${parts[1]}`;
    };
    
    return `${parseDate(monStr)} - ${parseDate(sunStr)}`;
});

// Computed rows for the weekly schedule matrix (TT, Mã nv, Tên, Thứ 2 -> CN)
const tableRows = computed(() => {
    const weekDates = tableWeekDays.value;
    const weekSchedules = filteredItems.value.filter(item => weekDates.includes(item.ngay));
    
    const employeeMap = {};
    const searchFilter = filters.value.search ? filters.value.search.toLowerCase() : '';
    
    // Add all active employees to the map first
    employeeOptions.value.forEach(emp => {
        if (searchFilter && (!emp.ten || !emp.ten.toLowerCase().includes(searchFilter)) && (!emp.ma || !emp.ma.toLowerCase().includes(searchFilter))) {
            return;
        }
        
        employeeMap[emp.ten] = {
            maNhanVien: emp.ma || 'N/A',
            nhanVien: emp.ten,
            nhanVienId: emp.id,
            schedules: {}
        };
        weekDates.forEach(date => {
            employeeMap[emp.ten].schedules[date] = [];
        });
    });
    
    // Fill in existing schedules
    weekSchedules.forEach(item => {
        if (!employeeMap[item.nhanVien]) {
            const matchesSearch = !searchFilter || 
                item.nhanVien.toLowerCase().includes(searchFilter) || 
                (getEmployeeCode(item) && getEmployeeCode(item).toLowerCase().includes(searchFilter));
                
            if (!matchesSearch) return;
            
            employeeMap[item.nhanVien] = {
                maNhanVien: getEmployeeCode(item) || 'N/A',
                nhanVien: item.nhanVien,
                nhanVienId: item.nhanVienId,
                schedules: {}
            };
            weekDates.forEach(date => {
                employeeMap[item.nhanVien].schedules[date] = [];
            });
        }
        if (employeeMap[item.nhanVien].schedules[item.ngay]) {
            employeeMap[item.nhanVien].schedules[item.ngay].push(item);
        }
    });
    
    return Object.values(employeeMap).sort((a, b) => a.maNhanVien.localeCompare(b.maNhanVien));
});

// Edit & Delete actions for schedule entries
const handleEditSchedule = (s) => {
    isEditSchedule.value = true;
    editScheduleId.value = s.id;
    addForm.value.nhanVien = [s.nhanVienId];
    addForm.value.ca = [s.ca];
    addForm.value.ngay = s.ngay;
    addForm.value.trangThai = s.trangThai;
    addForm.value.tangCa = s.tangCa || false;
    addForm.value.gioBatDauTangCa = s.gioBatDauTangCa || '';
    addForm.value.gioKetThucTangCa = s.gioKetThucTangCa || '';
    showAddDialog.value = true;
};

const handleDeleteSchedule = async (schedule) => {
    if (!confirm(`Bạn có chắc chắn muốn xóa lịch làm việc của nhân viên "${schedule.nhanVien}" vào ngày ${schedule.ngay} không?`)) {
        return;
    }
    loading.value = true;
    try {
        const res = await apiService.delete(`${API_LICH_LAM_VIEC.BASE}/schedules/${schedule.id}`);
        if (res.data.success) {
            alert('Xóa lịch thành công!');
            loadData();
        }
    } catch (error) {
        console.error('Delete schedule error:', error);
        alert('Có lỗi xảy ra khi xóa lịch làm việc!');
    } finally {
        loading.value = false;
    }
};

const handleAddForCell = (employeeId, dateStr) => {
    isEditSchedule.value = false;
    editScheduleId.value = null;
    addForm.value.nhanVien = [employeeId];
    addForm.value.ca = [];
    addForm.value.ngay = dateStr;
    addForm.value.trangThai = 'CHO_XAC_NHAN';
    showAddDialog.value = true;
};

// Format date range or month or day title label
const periodLabel = computed(() => {
    const year = currentMonth.value.getFullYear();
    const month = currentMonth.value.getMonth();
    
    if (calendarTab.value === 'month') {
        return `Tháng ${month + 1} / ${year}`;
    } else if (calendarTab.value === 'week') {
        const today = new Date(currentMonth.value);
        const dayOfWeek = today.getDay(); // 0 is Sunday
        const mondayDiff = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
        const startOfWeek = new Date(today);
        startOfWeek.setDate(today.getDate() + mondayDiff);
        const endOfWeek = new Date(startOfWeek);
        endOfWeek.setDate(startOfWeek.getDate() + 6);
        
        const formatD = (d) => `${d.getDate()}/${d.getMonth() + 1}`;
        return `Tuần: ${formatD(startOfWeek)} - ${formatD(endOfWeek)} / ${year}`;
    } else if (calendarTab.value === 'day') {
        const d = currentMonth.value;
        const dayOfWeek = d.getDay();
        const days = ['Chủ Nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
        return `${days[dayOfWeek]}, ${d.getDate()}/${d.getMonth() + 1}/${year}`;
    }
    return '';
});

// Navigate backward in time
const prevPeriod = () => {
    const d = new Date(currentMonth.value);
    if (calendarTab.value === 'month' || mainTab.value === 'table') {
        d.setMonth(d.getMonth() - 1);
    } else if (calendarTab.value === 'week') {
        d.setDate(d.getDate() - 7);
    } else if (calendarTab.value === 'day') {
        d.setDate(d.getDate() - 1);
    }
    currentMonth.value = d;
};

// Navigate forward in time
const nextPeriod = () => {
    const d = new Date(currentMonth.value);
    if (calendarTab.value === 'month' || mainTab.value === 'table') {
        d.setMonth(d.getMonth() + 1);
    } else if (calendarTab.value === 'week') {
        d.setDate(d.getDate() + 7);
    } else if (calendarTab.value === 'day') {
        d.setDate(d.getDate() + 1);
    }
    currentMonth.value = d;
};

// Computed property for 7 columns of the week (Monday first)
const weekDays = computed(() => {
    const result = [];
    const today = new Date(currentMonth.value);
    const dayOfWeek = today.getDay(); // 0 is Sunday
    const mondayDiff = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const startOfWeek = new Date(today);
    startOfWeek.setDate(today.getDate() + mondayDiff); // Go back to Monday

    const dayNames = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật'];

    for (let i = 0; i < 7; i++) {
        const d = new Date(startOfWeek);
        d.setDate(startOfWeek.getDate() + i);
        const dateStr = d.getFullYear() + '-' + 
            String(d.getMonth() + 1).padStart(2, '0') + '-' + 
            String(d.getDate()).padStart(2, '0');
        
        const schedules = filteredItems.value.filter((s) => s.ngay === dateStr);
        const isToday = dateStr === new Date().toISOString().substr(0, 10);

        result.push({
            dayName: dayNames[i],
            dateLabel: `${d.getDate()}/${d.getMonth() + 1}`,
            date: dateStr,
            dayNum: d.getDate(),
            schedules,
            isToday
        });
    }
    return result;
});

// Computed property for days of the month (Monday first)
const monthDays = computed(() => {
    const year = currentMonth.value.getFullYear();
    const month = currentMonth.value.getMonth();

    const firstDay = new Date(year, month, 1).getDay(); // 0 is Sunday
    const startOffset = firstDay === 0 ? 6 : firstDay - 1; // Convert to Monday start
    const daysInCurrentMonth = new Date(year, month + 1, 0).getDate();
    const result = [];

    // Padding for start of month
    for (let i = 0; i < startOffset; i++) {
        result.push({ dayNum: null, date: null, schedules: [] });
    }

    // Days in current month
    for (let i = 1; i <= daysInCurrentMonth; i++) {
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
        const schedules = filteredItems.value.filter((s) => s.ngay === dateStr);
        const isToday = dateStr === new Date().toISOString().substr(0, 10);

        result.push({
            dayNum: i,
            date: dateStr,
            schedules,
            isToday
        });
    }
    return result;
});

// Day schedules computed
const daySchedules = computed(() => {
    const d = currentMonth.value;
    const dateStr = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    return filteredItems.value.filter((s) => s.ngay === dateStr);
});

onMounted(() => {
    loadData();
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <AdminBreadcrumbs :items="breadcrumbs" />

        <div class="mb-3"></div>


        <div class="filter-shell mb-4">
            <AdminFilter title="Bộ lọc lịch làm việc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm nhân viên</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Tên, mã nhân viên..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input"
                        @input="handleFilter"
                    />
                </v-col>
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Ca làm việc</div>
                    <v-select
                        v-model="filters.ca"
                        :items="shiftOptions"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="handleFilter"
                    />
                </v-col>
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Lọc theo ngày</div>
                    <AppDatePicker
                        :model-value="filters.ngay"
                        @update:model-value="val => { filters.ngay = val ? new Date(val.getTime() - val.getTimezoneOffset() * 60000).toISOString().substr(0, 10) : null; handleFilter(); }"
                        placeholder="Chọn ngày"
                        clearable
                    />
                </v-col>
            </AdminFilter>
        </div>

        <!-- Table View Mode -->
        <div v-if="mainTab === 'table'" class="flex-grow-1 overflow-hidden d-flex flex-column bg-white rounded-xl border">
            <AdminTable
                title="Bảng phân lịch làm việc"
                :headers="tableHeaders"
                :items="filteredItems"
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
                    <!-- Tab Bảng / Lịch -->
                    <div class="main-view-tabs">
                        <button
                            class="view-tab-btn" 
                            :class="{ 'view-tab-btn--active': mainTab === 'table' }"
                            @click="mainTab = 'table'"
                        >
                            <v-icon size="15" class="mr-1">mdi-table-large</v-icon>
                            Bảng
                        </button>
                        <button
                            class="view-tab-btn"
                            :class="{ 'view-tab-btn--active': mainTab === 'calendar' }"
                            @click="mainTab = 'calendar'"
                        >
                            <v-icon size="15" class="mr-1">mdi-calendar</v-icon>
                            Lịch
                        </button>
                    </div>
                </template>

                <template #row="{ item, index }">
                    <tr class="data-row">
                        <!-- STT -->
                        <td class="data-cell text-center">{{ index + 1 }}</td>
                        <!-- Mã nhân viên -->
                        <td class="data-cell text-center">
                            <div class="text-truncate" :title="item.maNhanVien || getEmployeeCode(item)">{{ item.maNhanVien || getEmployeeCode(item) }}</div>
                        </td>
                        <!-- Nhân viên -->
                        <td class="data-cell text-left px-4">
                            <div class="text-slate-800 text-truncate" :title="item.nhanVien">{{ item.nhanVien }}</div>
                        </td>
                        <!-- Ca làm -->
                        <td class="data-cell text-center">
                            <v-chip size="small" color="info" variant="flat" class="font-weight-medium">
                                {{ item.ca }}
                            </v-chip>
                        </td>
                        <!-- Thời gian -->
                        <td class="data-cell text-center">
                            <span class="font-weight-medium text-slate-800">{{ getShiftTimeRange(item.ca) }}</span>
                        </td>
                        <!-- Ngày làm -->
                        <td class="data-cell text-center">
                            <span class="text-slate-600">{{ formatDate(item.ngay) }}</span>
                        </td>
                        <!-- Thao tác -->
                        <td class="data-cell action-cell">
                            <div class="action-controls">
                                <v-btn variant="text" color="primary" class="action-icon-btn" size="small" @click="handleEditSchedule(item)">
                                    <v-icon size="18">mdi-eye-outline</v-icon>
                                    <v-tooltip activator="parent" location="top">Xem / Sửa</v-tooltip>
                                </v-btn>
                                <v-btn variant="text" color="error" class="action-icon-btn" size="small" @click="handleDeleteSchedule(item)">
                                    <component :is="ADMIN_ICONS.ACTION.DELETE" size="16" />
                                    <v-tooltip activator="parent" location="top">Xóa</v-tooltip>
                                </v-btn>
                            </div>
                        </td>
                    </tr>
                </template>
            </AdminTable>
        </div>

        <!-- Calendar View Mode (Tuần / Tháng / Năm) -->
        <div v-else class="flex-grow-1 overflow-hidden d-flex flex-column bg-white rounded-xl border">
            <!-- Calendar Toolbar: Title + action buttons -->
            <div class="table-toolbar d-flex align-center justify-space-between pa-3 border-b flex-wrap gap-2">
                <div class="d-flex align-center">
                    <v-icon color="primary" class="mr-3">mdi-calendar-range</v-icon>
                    <h3 class="text-h6 font-weight-medium" style="color: #1e293b;">Lịch làm việc</h3>
                </div>
                <div class="d-flex align-center flex-wrap justify-end gap-2">
                    <v-btn prepend-icon="mdi-download" variant="flat" class="admin-btn-secondary" @click="handleDownloadTemplate">
                        Tải mẫu
                    </v-btn>
                    <v-btn prepend-icon="mdi-upload" variant="flat" class="admin-btn-secondary" @click="handleImport">
                        Nhập Excel
                    </v-btn>
                    <!-- Tab Bảng / Lịch -->
                    <div class="main-view-tabs">
                        <button
                            class="view-tab-btn"
                            :class="{ 'view-tab-btn--active': mainTab === 'table' }"
                            @click="mainTab = 'table'"
                        >
                            <v-icon size="15" class="mr-1">mdi-table-large</v-icon>
                            Bảng
                        </button>
                        <button
                            class="view-tab-btn view-tab-btn--active"
                            :class="{ 'view-tab-btn--active': mainTab === 'calendar' }"
                            @click="mainTab = 'calendar'"
                        >
                            <v-icon size="15" class="mr-1">mdi-calendar</v-icon>
                            Lịch
                        </button>
                    </div>
                    <v-btn prepend-icon="mdi-plus" variant="flat" color="primary" class="add-btn-primary" @click="handleAdd">
                        Tạo mới
                    </v-btn>
                </div>
            </div>

            <!-- Calendar Sub-tab: Tuần / Tháng / Năm + Period Navigation -->
            <div class="d-flex align-center justify-space-between pa-3 border-b" style="background: #f8fafc; flex-wrap: wrap; gap: 8px;">
                <v-btn-toggle
                    v-model="calendarTab"
                    mandatory
                    color="primary"
                    variant="flat"
                    class="rounded-xl border sub-tab-toggle"
                    style="background: #f1f5f9; padding: 4px; border-radius: 12px !important;"
                >
                    <v-btn value="day" class="px-4 rounded-lg sub-tab-btn" :ripple="false">
                        <v-icon start size="16" class="mr-1">mdi-calendar-today</v-icon>
                        Ngày
                    </v-btn>
                    <v-btn value="week" class="px-4 rounded-lg sub-tab-btn" :ripple="false">
                        <v-icon start size="16" class="mr-1">mdi-calendar-week</v-icon>
                        Tuần
                    </v-btn>
                    <v-btn value="month" class="px-4 rounded-lg sub-tab-btn" :ripple="false">
                        <v-icon start size="16" class="mr-1">mdi-calendar-month</v-icon>
                        Tháng
                    </v-btn>
                </v-btn-toggle>

                <div class="d-flex align-center gap-2 bg-white px-3 py-1 rounded-xl border">
                    <v-btn icon="mdi-chevron-left" variant="text" size="small" color="primary" @click="prevPeriod"></v-btn>
                    <div class="text-subtitle-2 font-weight-black text-primary px-3 text-center" style="min-width: 200px;">
                        {{ periodLabel }}
                    </div>
                    <v-btn icon="mdi-chevron-right" variant="text" size="small" color="primary" @click="nextPeriod"></v-btn>
                </div>
            </div>

            <!-- Calendar Display Bodies -->
            <div class="pa-4 flex-grow-1 d-flex flex-column overflow-hidden">
                <!-- 1. WEEK VIEW - Dạng bảng matrix (STT | Mã NV | Tên | T2~CN | Hành động) -->
                <div v-if="calendarTab === 'week'" class="overflow-x-auto flex-grow-1">
                    <table class="schedule-matrix-table w-100">
                        <thead>
                            <tr>
                                <th class="matrix-th" style="width:50px">STT</th>
                                <th class="matrix-th" style="width:110px">Mã NV</th>
                                <th class="matrix-th" style="width:155px;text-align:left;padding-left:14px">Tên Nhân Viên</th>
                                <th
                                    v-for="dayObj in weekDays"
                                    :key="dayObj.date"
                                    class="matrix-th day-col-header"
                                    :class="{ 'today-col-header': dayObj.isToday }"
                                >
                                    <div class="day-col-label">{{ dayObj.dayName }}</div>
                                    <div class="day-col-date">{{ dayObj.dateLabel }}</div>
                                </th>
                                <th class="matrix-th" style="width:80px">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-if="tableRows.length === 0">
                                <td colspan="11" class="text-center py-10 text-slate-400 text-caption">Không có dữ liệu lịch trong tuần này</td>
                            </tr>
                            <tr v-for="(row, idx) in tableRows" :key="row.nhanVienId" class="matrix-row">
                                <td class="matrix-td text-center text-slate-500 text-caption">{{ idx + 1 }}</td>
                                <td class="matrix-td text-center">
                                    <span class="matrix-emp-code">{{ row.maNhanVien }}</span>
                                </td>
                                <td class="matrix-td" style="padding-left:14px">
                                    <span class="font-weight-medium text-slate-800" style="font-size:13px">{{ row.nhanVien }}</span>
                                </td>
                                <td
                                    v-for="dayDate in tableWeekDays"
                                    :key="dayDate"
                                    class="matrix-td matrix-day-cell"
                                    :class="{ 'matrix-today-cell': dayDate === new Date().toISOString().substr(0,10) }"
                                    @click="handleAddForCell(row.nhanVienId, dayDate)"
                                >
                                    <div v-if="row.schedules[dayDate] && row.schedules[dayDate].length > 0" class="matrix-shifts">
                                        <div
                                            v-for="s in row.schedules[dayDate]"
                                            :key="s.id"
                                            class="matrix-shift-chip"
                                            :class="getMatrixShiftClass(s.ca)"
                                            @click.stop="handleEditSchedule(s)"
                                        >
                                            <div class="d-flex align-center justify-space-between">
                                                <span class="matrix-shift-name">{{ s.ca }}</span>
                                                <div class="d-flex gap-1">
                                                    <v-btn icon variant="text" size="x-small" color="primary" style="width:16px;height:16px" @click.stop="handleEditSchedule(s)">
                                                        <component :is="ADMIN_ICONS.ACTION.EDIT" size="9" />
                                                    </v-btn>
                                                    <v-btn icon variant="text" size="x-small" color="error" style="width:16px;height:16px" @click.stop="handleDeleteSchedule(s)">
                                                        <component :is="ADMIN_ICONS.ACTION.DELETE" size="9" />
                                                    </v-btn>
                                                </div>
                                            </div>
                                            <div class="matrix-shift-time">{{ getShiftTimeRange(s.ca) }}</div>
                                            <div v-if="s.tangCa && s.gioBatDauTangCa" class="matrix-overtime-badge">
                                                <v-icon size="9" class="mr-1">mdi-clock-plus</v-icon>
                                                TC: {{ s.gioBatDauTangCa.substring(0,5) }}-{{ s.gioKetThucTangCa ? s.gioKetThucTangCa.substring(0,5) : '' }}
                                            </div>
                                            <div class="matrix-shift-status">{{ s.trangThai === 'DA_XAC_NHAN' ? 'Đã duyệt' : 'Chờ duyệt' }}</div>
                                        </div>
                                    </div>
                                    <div v-else class="matrix-empty-cell">--</div>
                                </td>
                                <td class="matrix-td text-center">
                                    <v-btn icon variant="text" size="small" color="primary" @click.stop="handleAddForCell(row.nhanVienId, tableWeekDays[0])">
                                        <v-icon size="16">mdi-plus-circle-outline</v-icon>
                                        <v-tooltip activator="parent" location="top">Thêm lịch</v-tooltip>
                                    </v-btn>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- 2. MONTH VIEW (Chia đều) -->
                <div v-else-if="calendarTab === 'month'" class="calendar-container flex-grow-1 overflow-y-auto">
                    <div class="calendar-header-row mb-2">
                        <div v-for="day in ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật']" :key="day" class="day-header">
                            {{ day }}
                        </div>
                    </div>
                    <div class="calendar-grid-month">
                        <div
                            v-for="(dayObj, idx) in monthDays"
                            :key="idx"
                            class="month-day-cell"
                            :class="{ 'empty-cell': !dayObj.dayNum, 'today-cell': dayObj.isToday, 'clickable-day': dayObj.dayNum }"
                            @click="dayObj.dayNum && handleDayClick(dayObj)"
                        >
                            <div v-if="dayObj.dayNum" class="month-day-header">
                                <span class="month-day-number">{{ dayObj.dayNum }}</span>
                                <span v-if="dayObj.isToday" class="month-day-today-label">Hôm nay</span>
                            </div>
                            <div v-if="dayObj.schedules && dayObj.schedules.length > 0" class="month-schedule-list">
                                <div 
                                    v-for="s in dayObj.schedules" 
                                    :key="s.id" 
                                    class="schedule-item-card py-1 px-2" 
                                    style="font-size: 11px; padding: 4px 6px !important;"
                                    :class="s.trangThai === 'DA_XAC_NHAN' ? 'status-confirmed' : 'status-pending'"
                                    @click.stop="handleEditSchedule(s)"
                                >
                                    <div class="d-flex align-center justify-space-between w-100 text-truncate" style="gap: 4px;">
                                        <span class="font-weight-bold" style="color: #475569; font-size: 9px;">
                                            {{ getShiftTimeRange(s.ca) }}
                                        </span>
                                        <span class="text-truncate font-weight-black text-slate-800" style="flex: 1;">{{ s.nhanVien }}</span>
                                        <span class="status-dot flex-shrink-0" :class="s.trangThai === 'DA_XAC_NHAN' ? 'dot-success' : 'dot-warning'"></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 3. DAY VIEW (Danh sách ca trong 1 ngày) -->
                <div v-else-if="calendarTab === 'day'" class="calendar-container flex-grow-1 overflow-y-auto">
                    <div class="calendar-grid-day px-2 py-4 max-w-4xl mx-auto">
                        <div v-if="daySchedules.length === 0" class="empty-state py-12 text-center rounded-xl bg-slate-50 border border-dashed">
                            <v-icon size="48" color="grey-lighten-1" class="mb-3">mdi-calendar-blank</v-icon>
                            <div class="text-subtitle-1 text-slate-500">Không có lịch làm việc trong ngày này</div>
                            <v-btn variant="outlined" color="primary" class="mt-4 rounded-lg" @click="handleDayClick({date: currentMonth.toISOString().substr(0, 10)})">
                                <v-icon start>mdi-plus</v-icon>
                                Phân lịch ngay
                            </v-btn>
                        </div>
                        <div v-else class="day-schedule-list d-flex flex-column gap-3">
                            <div 
                                v-for="s in daySchedules" 
                                :key="s.id" 
                                class="schedule-item-card px-4 py-3 d-flex align-center justify-space-between" 
                                :class="s.trangThai === 'DA_XAC_NHAN' ? 'status-confirmed' : 'status-pending'"
                            >
                                <div class="d-flex align-center gap-4">
                                    <div class="time-block d-flex flex-column justify-center align-center py-2 px-3 rounded-lg" style="background: rgba(255,255,255,0.7); min-width: 110px;">
                                        <span class="status-dot mb-1" :class="s.trangThai === 'DA_XAC_NHAN' ? 'dot-success' : 'dot-warning'"></span>
                                        <span class="font-weight-black text-primary" style="font-size: 13px;">
                                            {{ getShiftTimeRange(s.ca) }}
                                        </span>
                                    </div>
                                    <div class="employee-info d-flex flex-column">
                                        <div class="d-flex align-center mb-1">
                                            <span class="font-weight-bold text-slate-800" style="font-size: 15px;">{{ s.nhanVien }}</span>
                                            <v-chip size="x-small" color="slate" variant="tonal" class="ml-2 px-2 font-weight-medium">{{ getEmployeeCode(s) }}</v-chip>
                                        </div>
                                        <div class="d-flex align-center gap-2">
                                            <v-chip size="small" :color="s.trangThai === 'DA_XAC_NHAN' ? 'success' : 'warning'" variant="flat" class="font-weight-medium text-caption px-2">
                                                {{ s.trangThai === 'DA_XAC_NHAN' ? 'Đã duyệt' : 'Chờ duyệt' }}
                                            </v-chip>
                                            <span class="text-caption text-slate-500"><v-icon size="12" class="mr-1">mdi-briefcase-outline</v-icon>{{ s.ca }}</span>
                                        </div>
                                    </div>
                                </div>
                                <div class="action-buttons d-flex gap-2">
                                    <v-btn variant="tonal" color="primary" class="rounded-lg" size="small" @click.stop="handleEditSchedule(s)">
                                        <v-icon start size="14">mdi-pencil</v-icon> Sửa
                                    </v-btn>
                                    <v-btn variant="tonal" color="error" class="rounded-lg" size="small" @click.stop="handleDeleteSchedule(s)">
                                        <v-icon start size="14">mdi-delete</v-icon> Xóa
                                    </v-btn>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Add/Edit Dialog -->
        <v-dialog v-model="showAddDialog" max-width="500">
            <v-card class="rounded-xl pa-4">
                <v-card-title class="text-h6 font-weight-bold d-flex align-center">
                    <v-icon color="primary" class="mr-3">{{ isEditSchedule ? 'mdi-pencil-circle' : 'mdi-plus-circle' }}</v-icon>
                    {{ isEditSchedule ? 'Cập nhật lịch làm việc' : 'Thêm lịch làm việc' }}
                </v-card-title>
                <v-card-text>
                    <v-row>
                        <v-col cols="12">
                            <div class="filter-field-label">Nhân viên {{ isEditSchedule ? '' : '(Chọn nhiều)' }}</div>
                            <v-autocomplete
                                v-model="addForm.nhanVien"
                                :items="employeeOptions"
                                :item-title="(item) => `${item.ma} - ${item.ten}`"
                                item-value="id"
                                placeholder="Tìm kiếm nhân viên (Tên, Mã)"
                                variant="outlined"
                                density="compact"
                                :multiple="!isEditSchedule"
                                chips
                                closable-chips
                                hide-details
                            >
                                <template v-slot:item="{ props, item }">
                                    <v-list-item v-bind="props" :title="item.raw.ten" :subtitle="item.raw.ma"></v-list-item>
                                </template>
                            </v-autocomplete>
                        </v-col>
                        <v-col cols="12">
                            <div class="filter-field-label">Ca làm {{ isEditSchedule ? '' : '(Chọn nhiều)' }}</div>
                            <v-select
                                v-model="addForm.ca"
                                :items="rawShifts"
                                item-title="tenCa"
                                item-value="tenCa"
                                placeholder="Chọn ca làm việc"
                                variant="outlined"
                                density="compact"
                                :multiple="!isEditSchedule"
                                chips
                                closable-chips
                                hide-details
                            />
                        </v-col>
                        <v-col cols="12">
                            <div class="filter-field-label">Ngày làm</div>
                            <AppDatePicker 
                                :model-value="addForm.ngay" 
                                @update:model-value="val => addForm.ngay = val ? new Date(val.getTime() - val.getTimezoneOffset() * 60000).toISOString().substr(0, 10) : null" 
                                placeholder="Chọn ngày làm" 
                            />
                        </v-col>
                        <v-col cols="12" v-if="isEditSchedule">
                            <div class="filter-field-label">Trạng thái</div>
                            <v-select
                                v-model="addForm.trangThai"
                                :items="[
                                    { title: 'Chờ xác nhận', value: 'CHO_XAC_NHAN' },
                                    { title: 'Đã xác nhận', value: 'DA_XAC_NHAN' }
                                ]"
                                item-title="title"
                                item-value="value"
                                variant="outlined"
                                density="compact"
                                hide-details
                            />
                        </v-col>
                        <!-- ===== TĂNG CA ===== -->
                        <v-col cols="12">
                            <v-divider class="my-1" />
                            <div class="d-flex align-center justify-space-between py-1">
                                <div>
                                    <div class="filter-field-label mb-0" style="font-weight:700">Tăng ca (ngoài ca đăng ký)</div>
                                    <div class="text-caption text-slate-400">Bật nếu nhân viên làm thêm giờ</div>
                                </div>
                                <v-switch v-model="addForm.tangCa" color="primary" hide-details density="compact" inset />
                            </div>
                            <div v-if="addForm.tangCa" class="mt-2">
                                <v-alert type="warning" variant="tonal" density="compact" class="mb-3 rounded-lg text-caption">
                                    <v-icon size="13" class="mr-1">mdi-clock-alert-outline</v-icon>
                                    Nhân viên sẽ làm thêm ngoài khung giờ ca đã đăng ký
                                </v-alert>
                                <v-row dense>
                                    <v-col cols="6">
                                        <div class="filter-field-label">Bắt đầu tăng ca</div>
                                        <v-text-field v-model="addForm.gioBatDauTangCa" type="time" variant="outlined" density="compact" hide-details />
                                    </v-col>
                                    <v-col cols="6">
                                        <div class="filter-field-label">Kết thúc tăng ca</div>
                                        <v-text-field v-model="addForm.gioKetThucTangCa" type="time" variant="outlined" density="compact" hide-details />
                                    </v-col>
                                    <v-col cols="12" v-if="addForm.gioBatDauTangCa && addForm.gioKetThucTangCa">
                                        <v-alert type="success" variant="tonal" density="compact" class="rounded-lg text-caption">
                                            Tổng tăng ca: <strong>{{ ((parseInt(addForm.gioKetThucTangCa.split(':')[0]) * 60 + parseInt(addForm.gioKetThucTangCa.split(':')[1])) - (parseInt(addForm.gioBatDauTangCa.split(':')[0]) * 60 + parseInt(addForm.gioBatDauTangCa.split(':')[1])) > 0 ? (((parseInt(addForm.gioKetThucTangCa.split(':')[0]) * 60 + parseInt(addForm.gioKetThucTangCa.split(':')[1])) - (parseInt(addForm.gioBatDauTangCa.split(':')[0]) * 60 + parseInt(addForm.gioBatDauTangCa.split(':')[1]))) / 60).toFixed(1) : 0) }} giờ</strong>
                                        </v-alert>
                                    </v-col>
                                </v-row>
                            </div>
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
                                    <th class="header-cell" style="width: 50px">STT</th>
                                    <th class="header-cell">Nhân viên</th>
                                    <th class="header-cell">Ca làm</th>
                                    <th class="header-cell">Ngày làm</th>
                                    <th class="header-cell">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(row, idx) in importPreviewData" :key="idx" class="data-row">
                                    <td class="data-cell">{{ idx + 1 }}</td>
                                    <td class="data-cell font-weight-bold">{{ row.nhanVien }}</td>
                                    <td class="data-cell">
                                        <v-chip size="x-small" color="info" variant="tonal">{{ row.ca }}</v-chip>
                                    </td>
                                    <td class="data-cell">{{ row.ngay }}</td>
                                    <td class="data-cell">
                                        <v-chip size="x-small" :color="row.status === 'VALID' ? 'success' : 'error'" variant="flat">
                                            {{ row.status === 'VALID' ? 'Hợp lệ' : 'Lỗi' }}
                                        </v-chip>
                                    </td>
                                </tr>
                                <tr v-if="!importPreviewData || importPreviewData.length === 0">
                                    <td colspan="5" class="empty-state py-16 text-center">
                                        <div class="d-flex flex-column align-center py-12 bg-slate-50/30 rounded-lg mx-4 my-2">
                                            <v-icon icon="mdi-package-variant" size="48" style="color: #94a3b8 !important; opacity: 0.6;" class="mb-3" />
                                            <span class="text-slate-500" style="font-size: 14px !important; font-weight: 400 !important;">Không có dữ liệu hợp lệ trong file Excel.</span>
                                        </div>
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
/* ============ Tab Bảng / Lịch Toggle ============ */
.main-view-tabs {
    display: flex;
    align-items: center;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 3px;
    gap: 2px;
    height: 44px;
}

.view-tab-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    padding: 0 16px;
    border-radius: 6px;
    border: none;
    background: transparent;
    color: #64748b;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.18s ease;
    white-space: nowrap;
    letter-spacing: normal;
}

.view-tab-btn:hover {
    background: #f1f5f9;
    color: #334155;
}

.view-tab-btn--active {
    background: #1e257c !important;
    color: #ffffff !important;
    box-shadow: 0 2px 6px rgba(30, 37, 124, 0.25);
}

.view-tab-btn--active:hover {
    background: #1a2070 !important;
    color: #ffffff !important;
}

/* Legacy toggle (sub-tabs Tuan/Thang/Nam) */
.main-tab-toggle, .sub-tab-toggle {
    border: 1px solid #e2e8f0 !important;
    height: 42px !important;
}

.main-tab-btn, .sub-tab-btn {
    height: 34px !important;
    min-height: 34px !important;
    text-transform: none !important;
    font-weight: 600 !important;
    font-size: 13px !important;
    letter-spacing: normal !important;
}

/* Week View styling (ô dài) */
.calendar-grid-week {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 12px;
    background: #ffffff;
    padding: 12px;
    border-radius: 12px;
}

.week-column {
    display: flex;
    flex-direction: column;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    min-height: 480px;
    max-height: 600px;
    transition: all 0.2s ease;
}

.week-column.today-column {
    border: 2px solid #1e257c;
    box-shadow: 0 4px 12px rgba(30, 37, 124, 0.08);
}

.week-column-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 10px;
    border-bottom: 1px solid #e2e8f0;
    background: #f8fafc;
    border-top-left-radius: 10px;
    border-top-right-radius: 10px;
}

.week-column.today-column .week-column-header {
    background: #f0f1ff;
}

.week-column-header .day-name {
    font-size: 13px;
    font-weight: 700;
    color: #64748b;
    text-transform: uppercase;
}

.week-column.today-column .week-column-header .day-name {
    color: #1e257c;
}

.week-column-header .day-date {
    font-size: 12px;
    color: #94a3b8;
    margin-top: 2px;
}

.week-column.today-column .week-column-header .day-date {
    color: #6366f1;
    font-weight: 600;
}

.week-column-body {
    padding: 8px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    overflow-y: auto;
    flex-grow: 1;
}

/* Schedule Card styling */
.schedule-item-card {
    position: relative;
    border: 1px solid #e2e8f0;
    background: #f8fafc;
    border-radius: 8px;
    padding: 8px 10px 8px 12px;
    transition: all 0.2s ease;
    cursor: pointer;
    overflow: hidden;
}

.schedule-item-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.04);
}

.schedule-item-card.status-confirmed {
    border-left: 4px solid #22c55e;
}

.schedule-item-card.status-pending {
    border-left: 4px solid #eab308;
}

.schedule-card-content {
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.schedule-card-shift {
    font-size: 11px;
    font-weight: 700;
    color: #64748b;
    display: flex;
    align-items: center;
    gap: 4px;
}

.schedule-item-card.status-confirmed .schedule-card-shift {
    color: #166534;
}

.schedule-item-card.status-pending .schedule-card-shift {
    color: #854d0e;
}

.schedule-card-employee {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
}

.schedule-card-status {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 10px;
    margin-top: 2px;
}

.status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
}

.dot-success {
    background-color: #22c55e;
}

.dot-warning {
    background-color: #eab308;
}

.status-text {
    font-weight: 500;
    color: #64748b;
}

.empty-column-state {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 45px;
    border: 1px dashed #cbd5e1;
    border-radius: 6px;
    font-size: 11px;
    color: #94a3b8;
    cursor: pointer;
    transition: all 0.2s ease;
    margin-bottom: 8px;
}

.empty-column-state:hover {
    background: #f1f5f9;
    border-color: #64748b;
}

/* Month View grid (chia đều) */
.calendar-grid-month {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
    background: #ffffff;
    padding: 12px;
    border-radius: 12px;
}

.month-day-cell {
    min-height: 110px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 8px;
    display: flex;
    flex-direction: column;
    transition: all 0.2s ease;
}

.month-day-cell.clickable-day {
    cursor: pointer;
}

.month-day-cell.clickable-day:hover {
    border-color: #1e257c;
    background-color: #f8fbff;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.04);
}

.month-day-cell.today-cell {
    border: 2px solid #1e257c;
    background: #f0f1ff;
}

.month-day-cell.empty-cell {
    background: #f8fafc;
    border-style: dashed;
    opacity: 0.4;
    cursor: default;
}

.month-day-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
}

.month-day-number {
    font-size: 12px;
    font-weight: 700;
    color: #475569;
}

.month-day-today-label {
    font-size: 10px;
    background: #1e257c;
    color: #ffffff;
    padding: 1px 6px;
    border-radius: 10px;
    font-weight: 600;
}

.month-schedule-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
    overflow-y: auto;
    max-height: 75px;
}

/* Day View Grid */
.calendar-grid-day {
    width: 100%;
}
.day-schedule-list {
    display: flex;
    flex-direction: column;
}

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

/* ===== MATRIX WEEK TABLE ===== */
.schedule-matrix-table {
    border-collapse: collapse;
    background: #fff;
    min-width: 900px;
}
.matrix-th {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    padding: 10px 8px;
    font-size: 12px;
    font-weight: 700;
    color: #64748b;
    text-align: center;
    white-space: nowrap;
}
.day-col-header { min-width: 120px; }
.today-col-header { background: #eff6ff !important; }
.day-col-label { font-size: 12px; font-weight: 700; color: #475569; }
.today-col-header .day-col-label { color: #1e40af; }
.day-col-date { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.today-col-header .day-col-date { color: #6366f1; font-weight: 600; }
.matrix-row:hover { background: #f8fafc; }
.matrix-td {
    border: 1px solid #e2e8f0;
    padding: 8px 6px;
    vertical-align: middle;
    font-size: 13px;
}
.matrix-emp-code {
    display: inline-block;
    background: #f1f5f9;
    color: #475569;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 6px;
    border: 1px solid #e2e8f0;
}
.matrix-day-cell { cursor: pointer; min-width: 120px; }
.matrix-day-cell:hover { background: #f0f9ff; }
.matrix-today-cell { background: #eff6ff !important; }
.matrix-shifts { display: flex; flex-direction: column; gap: 4px; }
.matrix-shift-chip {
    border-radius: 8px;
    padding: 5px 7px;
    font-size: 11px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.15s;
    border-left: 3px solid transparent;
}
.matrix-shift-chip:hover { transform: translateY(-1px); box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
.matrix-shift-name { font-size: 11px; font-weight: 700; }
.shift-morning  { background: #dcfce7; color: #15803d; border-left-color: #22c55e; }
.shift-afternoon { background: #fef9c3; color: #a16207; border-left-color: #eab308; }
.shift-night    { background: #ede9fe; color: #6d28d9; border-left-color: #8b5cf6; }
.shift-default  { background: #dbeafe; color: #1d4ed8; border-left-color: #3b82f6; }
.matrix-shift-time { font-size: 10px; font-weight: 500; opacity: 0.75; margin-top: 1px; }
.matrix-overtime-badge {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-size: 9px;
    background: rgba(234,88,12,0.15);
    color: #c2410c;
    padding: 1px 5px;
    border-radius: 4px;
    margin-top: 2px;
    font-weight: 600;
}
.matrix-shift-status { font-size: 9px; opacity: 0.6; margin-top: 1px; }
.matrix-empty-cell {
    text-align: center;
    color: #cbd5e1;
    font-size: 16px;
    font-weight: 300;
    padding: 4px;
}
</style>
