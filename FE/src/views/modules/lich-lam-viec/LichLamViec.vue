<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { PATH } from '@/router/routePaths';
import { AdminFilter, AdminTable, AdminBreadcrumbs, AdminPagination, TableEmptyState } from '@/components/common';
import AppDatePicker from '@/components/common/AppDatePicker.vue';
import { CalendarIcon, LayoutGridIcon } from 'vue-tabler-icons';
import apiService from '@/services/apiService';
import { ADMIN_ICONS } from '@/constants/adminIcons';
import { useNotifications } from '@/services/notificationService';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';
import { useRoleAccess } from '@/composables/useRoleAccess';
import { isManagementRole } from '@/constants/appConstants';

const router = useRouter();
const { addNotification } = useNotifications();
const { currentUser, isStaff, canManageSchedule } = useRoleAccess();

const loading = ref(true);
const isRefreshing = ref(false);
const items = ref([]);
const shiftOptions = ref(['Tất cả']);
const employeeOptions = ref([]);
const rawShifts = ref([]);

const staffProfile = ref(null);

const currentStaffInfo = computed(() => {
    if (staffProfile.value) return staffProfile.value;
    if (!currentUser.value) return null;
    return (
        employeeOptions.value.find(
            (emp) =>
                (emp.id && emp.id === currentUser.value.id) ||
                (emp.tenTaiKhoan && emp.tenTaiKhoan === currentUser.value.username) ||
                (emp.ma && emp.ma === currentUser.value.username) ||
                (emp.ten && emp.ten === currentUser.value.username)
        ) || null
    );
});

// Main and Sub-tab views
const mainTab = ref('table'); // 'table', 'calendar'
const calendarTab = ref('week'); // 'day', 'week', 'month'

const toolbarIcon = computed(() => (mainTab.value === 'table' ? 'mdi-table-large' : 'mdi-calendar-range'));
const toolbarTitle = computed(() => (mainTab.value === 'table' ? 'Bảng phân lịch làm việc' : 'Lịch làm việc'));

const filters = ref({
    search: '',
    ca: 'Tất cả',
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
    trangThai: 'CHO_XAC_NHAN'
});

// Import Preview State
const showImportPreview = ref(false);
const importPreviewData = ref([]);

// Auto Schedule State
const showAutoScheduleDialog = ref(false);
const selectedAutoScheduleWeek = ref('current');

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch làm việc', disabled: true }
];

const tableHeaders = computed(() => {
    const headers = [
        { text: 'STT', width: '60px', align: 'center' },
        { text: 'Mã nhân viên', width: '140px', align: 'center' },
        { text: 'Nhân viên', width: '150px', align: 'center' },
        { text: 'Ca làm', width: '150px', align: 'center' },
        { text: 'Thời gian', width: '150px', align: 'center' },
        { text: 'Ngày làm', width: '150px', align: 'center' }
    ];
    if (canManageSchedule.value) {
        headers.push({ text: 'Thao tác', width: '100px', align: 'center' });
    }
    return headers;
});

// Computed property for filtering schedules
const filteredItems = computed(() => {
    if (!items.value) return [];
    let list = [...items.value];

    // Filter out schedules of admins/managers
    if (employeeOptions.value && employeeOptions.value.length > 0) {
        list = list.filter((item) => {
            return employeeOptions.value.some(
                (emp) =>
                    emp.id === item.nhanVienId ||
                    (emp.ma && item.maNhanVien && emp.ma.toLowerCase() === item.maNhanVien.toLowerCase()) ||
                    (emp.ten && item.nhanVien && emp.ten.toLowerCase() === item.nhanVien.toLowerCase())
            );
        });
    }

    // If logged in user is Staff (ROLE_NHAN_VIEN), ONLY show their own schedule
    if (isStaff.value) {
        const staff = currentStaffInfo.value;
        const username = currentUser.value?.username;
        list = list.filter((item) => {
            if (staff) {
                if (staff.id && item.nhanVienId === staff.id) return true;
                if (staff.ma && item.maNhanVien && item.maNhanVien.toLowerCase() === staff.ma.toLowerCase()) return true;
                if (staff.ten && item.nhanVien && item.nhanVien.toLowerCase() === staff.ten.toLowerCase()) return true;
                if (staff.tenTaiKhoan && item.nhanVien && item.nhanVien.toLowerCase() === staff.tenTaiKhoan.toLowerCase()) return true;
            }
            if (username) {
                if (item.maNhanVien && item.maNhanVien.toLowerCase() === username.toLowerCase()) return true;
                if (item.nhanVien && item.nhanVien.toLowerCase() === username.toLowerCase()) return true;
            }
            return false;
        });
    }

    return list.sort((a, b) => {
        // Sort by date ascending (from 1st of month onwards)
        const dateCompare = a.ngay.localeCompare(b.ngay);
        if (dateCompare !== 0) return dateCompare;
        // If same date, sort by shift name
        return (a.ca || '').localeCompare(b.ca || '');
    });
});

// Filtered items specifically for the main schedule table view (restricted to the target month)
const tableFilteredItems = computed(() => {
    // Determine the target YYYY-MM
    let targetYearMonth = '';
    if (filters.value.ngay) {
        targetYearMonth = filters.value.ngay.substring(0, 7);
    } else {
        const today = new Date();
        const yyyy = today.getFullYear();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        targetYearMonth = `${yyyy}-${mm}`;
    }

    return filteredItems.value.filter((item) => {
        return item.ngay && item.ngay.startsWith(targetYearMonth);
    });
});

// Pagination State
const pagination = ref({ page: 1, size: 10, totalElements: 0, totalPages: 0 });

const updatePaginationSize = (newSize) => {
    pagination.value.size = newSize;
    pagination.value.page = 1;
};

const paginatedItems = computed(() => {
    const start = (pagination.value.page - 1) * pagination.value.size;
    return tableFilteredItems.value.slice(start, start + pagination.value.size);
});

watch(
    [tableFilteredItems, () => pagination.value.size],
    ([newItems, newSize]) => {
        const total = newItems ? newItems.length : 0;
        pagination.value.totalElements = total;
        pagination.value.totalPages = Math.ceil(total / newSize);
        if (pagination.value.page > pagination.value.totalPages) {
            pagination.value.page = Math.max(1, pagination.value.totalPages);
        }
    },
    { immediate: true }
);

const loadData = async () => {
    loading.value = true;
    try {
        if (isStaff.value && !staffProfile.value) {
            try {
                const profile = await dichVuXacThuc.layThongTinCaNhan();
                if (profile) staffProfile.value = profile;
            } catch (e) {
                console.error('Lỗi tải hồ sơ cá nhân:', e);
            }
        }

        const [scheduleRes, shiftRes, empRes] = await Promise.all([
            apiService.get(API_LICH_LAM_VIEC.SCHEDULES, {
                params: {
                    search: filters.value.search,
                    ca: filters.value.ca === 'Tất cả' ? null : filters.value.ca,
                    ngay: null
                }
            }),
            apiService.get(API_LICH_LAM_VIEC.SHIFTS),
            apiService.get('/admin/nhan-vien/hien-thi')
        ]);

        // if (scheduleRes.data.success) {
        //     items.value = scheduleRes.data.data;
        //     if (items.value.length > 0) {
        //         const todayStr = new Date().toISOString().substr(0, 10);
        //         const hasToday = items.value.some((s) => s.ngay === todayStr);
        //         if (!hasToday) {
        //             const sorted = [...items.value].sort((a, b) => b.ngay.localeCompare(a.ngay));
        //             currentMonth.value = new Date(sorted[0].ngay);
        //         }
        //     }
        // }
        if (scheduleRes.data.success) {
    items.value = scheduleRes.data.data;
}

        if (shiftRes.data.success) {
            rawShifts.value = shiftRes.data.data;
            shiftOptions.value = ['Tất cả', ...shiftRes.data.data.map((s) => s.tenCa)];
        }

        if (empRes.data.success) {
            const rawContent = empRes.data.data.content || empRes.data.data;
            const content = Array.isArray(rawContent) ? rawContent : [];
            // Remove duplicates by ID and filter out management/admin roles
            const uniqueEmp = [];
            const seenIds = new Set();
            content.forEach((emp) => {
                if (emp.id && !seenIds.has(emp.id) && !isManagementRole(emp)) {
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
    if (!canManageSchedule.value) return;
    isEditSchedule.value = false;
    editScheduleId.value = null;
    addForm.value.nhanVien = [];
    addForm.value.ca = [];
    addForm.value.ngay = new Date().toISOString().substr(0, 10);
    showAddDialog.value = true;
};

const handleDayClick = (dayObj) => {
    if (!canManageSchedule.value) return;
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
        if (
            !addForm.value.nhanVien.length ||
            !addForm.value.ca ||
            (Array.isArray(addForm.value.ca) && !addForm.value.ca.length) ||
            !addForm.value.ngay
        ) {
            addNotification({ title: 'Lỗi', subtitle: 'Vui lòng chọn đầy đủ nhân viên, ca làm và ngày!', color: 'error' });
            return;
        }

        if (addForm.value.tangCa) {
            if (!addForm.value.gioBatDauTangCa || !addForm.value.gioKetThucTangCa) {
                addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập đầy đủ thời gian bắt đầu và kết thúc tăng ca!', color: 'error' });
                return;
            }
            if (addForm.value.gioBatDauTangCa >= addForm.value.gioKetThucTangCa) {
                addNotification({ title: 'Lỗi', subtitle: 'Giờ bắt đầu tăng ca phải nhỏ hơn giờ kết thúc tăng ca!', color: 'error' });
                return;
            }
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
            addNotification({
                title: 'Thành công',
                subtitle: isEditSchedule.value ? 'Cập nhật lịch thành công!' : 'Thêm lịch thành công!',
                color: 'success'
            });
            showAddDialog.value = false;
            loadData();
        }
    } catch (error) {
        console.error('Save error:', error);
        addNotification({ title: 'Lỗi', subtitle: error.response?.data?.message || 'Lỗi khi lưu lịch làm việc!', color: 'error' });
    } finally {
        loading.value = false;
    }
};

const handleRefresh = async () => {
    isRefreshing.value = true;
    filters.value.search = '';
    filters.value.ca = 'Tất cả';
    filters.value.ngay = null;
    currentMonth.value = new Date();
    pagination.value.page = 1;
    await loadData();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleFilter = () => {
    pagination.value.page = 1;
    if (filters.value.ngay) {
        currentMonth.value = new Date(filters.value.ngay);
    }
    loadData();
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

const autoSchedulePeriodStr = computed(() => {
    if (!tableWeekDays.value || tableWeekDays.value.length < 7) return '';
    const monStr = tableWeekDays.value[MonStrIndex() || 0];
    const sunStr = tableWeekDays.value[SunStrIndex() || 6];
    
    const format = (str) => {
        if (!str) return '';
        const parts = str.split('-');
        if (parts.length === 3) {
            return `${parts[2]}/${parts[1]}/${parts[0]}`;
        }
        return str;
    };
    
    return `${format(monStr)} - ${format(sunStr)}`;
});

const MonStrIndex = () => 0;
const SunStrIndex = () => 6;

const getWeekDays = (baseDate) => {
    const today = new Date(baseDate);
    const day = today.getDay(); // 0 is Sunday, 1 is Monday, ..., 6 is Saturday
    const mondayDiff = day === 0 ? -6 : 1 - day;

    const monday = new Date(today);
    monday.setDate(today.getDate() + mondayDiff);

    const result = [];
    for (let i = 0; i < 7; i++) {
        const d = new Date(monday);
        d.setDate(monday.getDate() + i);
        const dateStr = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
        result.push(dateStr);
    }
    return result;
};

const currentWeekDays = computed(() => {
    return getWeekDays(new Date());
});

const nextWeekDays = computed(() => {
    const nextWeekDate = new Date();
    nextWeekDate.setDate(nextWeekDate.getDate() + 7);
    return getWeekDays(nextWeekDate);
});

const currentWeekStr = computed(() => getWeekRangeStr(currentWeekDays.value));
const nextWeekStr = computed(() => getWeekRangeStr(nextWeekDays.value));

const getWeekRangeStr = (days) => {
    if (!days || days.length < 7) return '';
    return `${formatDateDDMMYYYY(days[0])} - ${formatDateDDMMYYYY(days[6])}`;
};

const openAutoScheduleDialog = () => {
    selectedAutoScheduleWeek.value = 'current';
    showAutoScheduleDialog.value = true;
};

const executeAutoSchedule = async () => {
    let targetDays = [];
    if (selectedAutoScheduleWeek.value === 'current') {
        targetDays = currentWeekDays.value;
    } else {
        targetDays = nextWeekDays.value;
    }

    if (!targetDays || targetDays.length < 7) {
        addNotification({ title: 'Lỗi', subtitle: 'Không xác định được khoảng thời gian tuần!', color: 'error' });
        return;
    }
    
    loading.value = true;
    showAutoScheduleDialog.value = false;
    
    try {
        const res = await apiService.post(API_LICH_LAM_VIEC.AUTO_SCHEDULE, {
            startDate: targetDays[0],
            endDate: targetDays[6]
        });
        
        if (res.data.success) {
            addNotification({
                title: 'Thành công',
                subtitle: res.data.data || 'Xếp ca tự động thành công!',
                color: 'success'
            });
            await loadData();
        }
    } catch (error) {
        console.error('Auto schedule error:', error);
        addNotification({
            title: 'Lỗi',
            subtitle: error.response?.data?.message || 'Có lỗi xảy ra khi xếp ca tự động!',
            color: 'error'
        });
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
    return item.maNhanVien || 'N/A';
};

// Lookup helper: Find shift time range (e.g., 08:00 - 12:00) by shift name
const getShiftTimeRange = (caName) => {
    if (!caName) return '';
    const shift = rawShifts.value.find((s) => s.tenCa === caName);
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

// Helper: class chip ca dạng tròn (nền nhạt + chữ màu) dùng cho bảng thường
const getShiftChipClass = (caName) => {
    if (!caName) return 'shift-chip-default';
    const name = caName.toLowerCase();
    if (name.includes('sáng') || name.includes('sang')) return 'shift-chip-morning';
    if (name.includes('chiều') || name.includes('chieu')) return 'shift-chip-afternoon';
    if (name.includes('tối') || name.includes('toi') || name.includes('đêm')) return 'shift-chip-night';
    return 'shift-chip-default';
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
        const dateStr = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
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
    const weekSchedules = filteredItems.value.filter((item) => weekDates.includes(item.ngay));

    const employeeMap = {};
    const searchFilter = filters.value.search ? filters.value.search.toLowerCase() : '';

    let targetEmployees = employeeOptions.value;
    if (isStaff.value && currentStaffInfo.value) {
        targetEmployees = targetEmployees.filter((emp) => emp.id === currentStaffInfo.value.id);
    }

    // Add all active employees to the map first
    targetEmployees.forEach((emp) => {
        if (
            searchFilter &&
            (!emp.ten || !emp.ten.toLowerCase().includes(searchFilter)) &&
            (!emp.ma || !emp.ma.toLowerCase().includes(searchFilter))
        ) {
            return;
        }

        employeeMap[emp.ten] = {
            maNhanVien: emp.ma || 'N/A',
            nhanVien: emp.ten,
            nhanVienId: emp.id,
            schedules: {}
        };
        weekDates.forEach((date) => {
            employeeMap[emp.ten].schedules[date] = [];
        });
    });

    // Fill in existing schedules
    weekSchedules.forEach((item) => {
        if (!employeeMap[item.nhanVien]) {
            if (isStaff.value) {
                const staff = currentStaffInfo.value;
                const isCurrentStaff = staff && (
                    item.nhanVienId === staff.id ||
                    (staff.ma && item.maNhanVien && item.maNhanVien.toLowerCase() === staff.ma.toLowerCase()) ||
                    (staff.ten && item.nhanVien && item.nhanVien.toLowerCase() === staff.ten.toLowerCase())
                );
                if (!isCurrentStaff) return;
            }

            const matchesSearch =
                !searchFilter ||
                item.nhanVien.toLowerCase().includes(searchFilter) ||
                (getEmployeeCode(item) && getEmployeeCode(item).toLowerCase().includes(searchFilter));

            if (!matchesSearch) return;

            employeeMap[item.nhanVien] = {
                maNhanVien: getEmployeeCode(item) || 'N/A',
                nhanVien: item.nhanVien,
                nhanVienId: item.nhanVienId,
                schedules: {}
            };
            weekDates.forEach((date) => {
                employeeMap[item.nhanVien].schedules[date] = [];
            });
        }
        if (employeeMap[item.nhanVien]?.schedules[item.ngay]) {
            employeeMap[item.nhanVien].schedules[item.ngay].push(item);
        }
    });

    return Object.values(employeeMap).sort((a, b) => a.maNhanVien.localeCompare(b.maNhanVien));
});

// Edit & Delete actions for schedule entries
const handleEditSchedule = (s) => {
    if (!canManageSchedule.value) return;
    isEditSchedule.value = true;
    editScheduleId.value = s.id;
    addForm.value.nhanVien = [s.nhanVienId];
    addForm.value.ca = [s.ca];
    addForm.value.ngay = s.ngay;
    addForm.value.trangThai = s.trangThai;
    showAddDialog.value = true;
};

// const handleDeleteSchedule = async (schedule) => {
//     if (!confirm(`Bạn có chắc chắn muốn xóa lịch làm việc của nhân viên "${schedule.nhanVien}" vào ngày ${schedule.ngay} không?`)) {
//         return;
//     }
//     loading.value = true;
//     try {
//         const res = await apiService.delete(`${API_LICH_LAM_VIEC.BASE}/schedules/${schedule.id}`);
//         if (res.data.success) {
//             alert('Xóa lịch thành công!');
//             loadData();
//         }
//     } catch (error) {
//         console.error('Delete schedule error:', error);
//         alert('Có lỗi xảy ra khi xóa lịch làm việc!');
//     } finally {
//         loading.value = false;
//     }
// };

// const handleAddForCell = (employeeId, dateStr) => {
//     isEditSchedule.value = false;
//     editScheduleId.value = null;
//     addForm.value.nhanVien = [employeeId];
//     addForm.value.ca = [];
//     addForm.value.ngay = dateStr;
//     addForm.value.trangThai = 'CHO_XAC_NHAN';
//     showAddDialog.value = true;
// };

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

        const formatFull = (d) => `${d.getDate()} tháng ${d.getMonth() + 1}, ${d.getFullYear()}`;
        return `${formatFull(startOfWeek)} - ${formatFull(endOfWeek)}`;
    }
    return '';
});

const goToday = () => {
    currentMonth.value = new Date();
};

const getDayAbbr = (dayName) => {
    if (dayName === 'Chủ Nhật') return 'CN';
    return dayName.replace('Thứ ', 'T');
};

const formatDateDDMMYYYY = (dateStr) => {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
};

const getShiftColorClass = (tenCa) => {
    const name = tenCa.toLowerCase();
    if (name.includes('sáng')) return 'color-green';
    if (name.includes('chiều')) return 'color-orange';
    if (name.includes('tối') || name.includes('đêm')) return 'color-purple';
    return 'color-slate';
};

const getCellCardClass = (shiftName) => {
    const name = shiftName.toLowerCase();
    if (name.includes('sáng')) return 'card-green';
    if (name.includes('chiều')) return 'card-orange';
    if (name.includes('tối') || name.includes('đêm')) return 'card-purple';
    return 'card-slate';
};

const getSchedulesForCell = (dateStr, shiftName) => {
    return filteredItems.value.filter((s) => s.ngay === dateStr && s.ca === shiftName);
};

const showSwapDialog = ref(false);
const swapSource = ref({});
const swapTargetEmployeeId = ref(null);

const openSwapDialog = (s) => {
    swapSource.value = s;
    swapTargetEmployeeId.value = null;
    showSwapDialog.value = true;
};

const swapEmployeeList = computed(() => {
    if (!swapSource.value || !swapSource.value.ngay) return [];

    // Get all schedules on the same day
    const schedulesOnDate = items.value.filter((item) => item.ngay === swapSource.value.ngay);

    return employeeOptions.value
        .filter((emp) => emp.id !== swapSource.value.nhanVienId)
        .map((emp) => {
            const empSchedule = schedulesOnDate.find((item) => item.nhanVienId === emp.id);
            let statusText = 'Không có ca';
            let hasShift = false;
            let currentShiftName = '';
            let scheduleId = null;
            let trangThai = 'CHO_XAC_NHAN';
            if (empSchedule) {
                statusText = `Đang làm ca: ${empSchedule.ca}`;
                hasShift = true;
                currentShiftName = empSchedule.ca;
                scheduleId = empSchedule.id;
                trangThai = empSchedule.trangThai;
            }
            return {
                id: emp.id,
                ten: emp.ten,
                ma: emp.ma,
                displayText: `${emp.ma} - ${emp.ten} (${statusText})`,
                statusText,
                hasShift,
                currentShiftName,
                scheduleId,
                trangThai
            };
        });
});

const executeSwap = async () => {
    if (!swapTargetEmployeeId.value) return;

    const targetEmp = swapEmployeeList.value.find((emp) => emp.id === swapTargetEmployeeId.value);
    if (!targetEmp) return;

    loading.value = true;
    try {
        if (targetEmp.hasShift) {
            // Case 1: Swap shifts between source employee and target employee
            const payload1 = {
                nhanVien: [swapSource.value.nhanVienId],
                ca: [targetEmp.currentShiftName],
                ngay: swapSource.value.ngay,
                trangThai: swapSource.value.trangThai
            };

            const payload2 = {
                nhanVien: [targetEmp.id],
                ca: [swapSource.value.ca],
                ngay: swapSource.value.ngay,
                trangThai: targetEmp.trangThai
            };

            await Promise.all([
                apiService.put(`${API_LICH_LAM_VIEC.BASE}/schedules/${swapSource.value.id}`, payload1),
                apiService.put(`${API_LICH_LAM_VIEC.BASE}/schedules/${targetEmp.scheduleId}`, payload2)
            ]);
        } else {
            // Case 2: Transfer shift to target employee
            const payload = {
                nhanVien: [targetEmp.id],
                ca: [swapSource.value.ca],
                ngay: swapSource.value.ngay,
                trangThai: swapSource.value.trangThai
            };

            await apiService.put(`${API_LICH_LAM_VIEC.BASE}/schedules/${swapSource.value.id}`, payload);
        }

        addNotification({
            title: 'Thành công',
            subtitle: 'Đổi ca làm việc thành công!',
            icon: 'CircleCheckIcon',
            color: 'success'
        });
        showSwapDialog.value = false;
        showCellDetailDialog.value = false;
        await loadData();
    } catch (error) {
        console.error('Swap shift error:', error);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Có lỗi xảy ra khi đổi ca làm việc!',
            icon: 'CircleXIcon',
            color: 'error'
        });
    } finally {
        loading.value = false;
    }
};

const showCellDetailDialog = ref(false);
const selectedCellDate = ref('');
const selectedCellShift = ref('');
const selectedCellSchedules = ref([]);

const openCellEmployeesDialog = (dateStr, shiftName, schedules) => {
    selectedCellDate.value = dateStr;
    selectedCellShift.value = shiftName;
    selectedCellSchedules.value = schedules;
    showCellDetailDialog.value = true;
};

const handleAddForCellAndShift = (dateStr, shiftName) => {
    if (!canManageSchedule.value) return;
    isEditSchedule.value = false;
    editScheduleId.value = null;
    addForm.value.nhanVien = [];
    addForm.value.ca = [shiftName];
    addForm.value.ngay = dateStr;
    addForm.value.trangThai = 'CHO_XAC_NHAN';
    showCellDetailDialog.value = false;
    showAddDialog.value = true;
};

const handleDeleteCellSchedule = async (schedule) => {
    if (!confirm(`Bạn có chắc chắn muốn xóa lịch làm việc của nhân viên "${schedule.nhanVien}" không?`)) {
        return;
    }
    loading.value = true;
    try {
        const res = await apiService.delete(`${API_LICH_LAM_VIEC.BASE}/schedules/${schedule.id}`);
        if (res.data.success) {
            addNotification({
                title: 'Thành công',
                subtitle: 'Xóa lịch thành công!',
                icon: 'CircleCheckIcon',
                color: 'success'
            });
            selectedCellSchedules.value = selectedCellSchedules.value.filter((item) => item.id !== schedule.id);
            await loadData();
        }
    } catch (error) {
        console.error('Delete schedule error:', error);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Có lỗi xảy ra khi xóa lịch làm việc!',
            icon: 'CircleXIcon',
            color: 'error'
        });
    } finally {
        loading.value = false;
    }
};

// Navigate backward in time
const prevPeriod = () => {
    const d = new Date(currentMonth.value);
    if (calendarTab.value === 'month' || mainTab.value === 'table') {
        d.setMonth(d.getMonth() - 1);
    } else if (calendarTab.value === 'week') {
        d.setDate(d.getDate() - 7);
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
        const dateStr = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');

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

onMounted(() => {
    loadData();
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <AdminBreadcrumbs :items="breadcrumbs" />

        <div class="mb-3"></div>

        <AdminFilter title="Bộ lọc" :isRefreshing="isRefreshing" @refresh="handleRefresh" class="mb-4">
            <v-col cols="12" md="4" class="px-2">
                <div class="filter-field-label">Nhân viên</div>
                <v-text-field
                    v-if="canManageSchedule"
                    v-model="filters.search"
                    placeholder="Tìm kiếm nhân viên..."
                    variant="outlined"
                    density="compact"
                    hide-details
                    prepend-inner-icon="mdi-magnify"
                    class="compact-input"
                    clearable
                    @input="handleFilter"
                />
                <v-text-field
                    v-else
                    :model-value="currentStaffInfo ? `${currentStaffInfo.ten} (${currentStaffInfo.ma})` : (currentUser?.username || 'Nhân viên')"
                    variant="outlined"
                    density="compact"
                    hide-details
                    readonly
                    bg-color="slate-50"
                    prepend-inner-icon="mdi-account"
                    class="compact-input"
                />
            </v-col>
            <v-col cols="12" md="3" class="px-2">
                <div class="filter-field-label">Ca làm</div>
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
            <v-col cols="12" md="3" class="px-2">
                <div class="filter-field-label">Ngày làm</div>
                <AppDatePicker
                    :model-value="filters.ngay"
                    @update:model-value="
                        (val) => {
                            filters.ngay = val
                                ? new Date(val.getTime() - val.getTimezoneOffset() * 60000).toISOString().substr(0, 10)
                                : null;
                            handleFilter();
                        }
                    "
                    placeholder="Chọn ngày"
                    clearable
                />
            </v-col>
        </AdminFilter>

        <!-- Shared Wrapper for View Mode -->
        <v-card class="admin-table-container elevation-0 flex-grow-1 overflow-hidden d-flex flex-column mb-0" elevation="0">
            <!-- Shared Toolbar -->
            <div class="table-toolbar d-flex align-center justify-space-between pa-3 border-b">
                <div class="d-flex align-center">
                    <LayoutGridIcon size="20" class="text-primary mr-3" />
                    <h3 class="text-h6 font-weight-bold text-black tracking-tight">Danh sách lịch làm việc</h3>
                </div>
                <div class="d-flex align-center flex-wrap justify-end admin-toolbar-actions ga-2">
                    <template v-if="canManageSchedule">
                        <v-btn prepend-icon="mdi-calendar-sync" variant="flat" class="admin-btn-orange" :disabled="loading" @click="openAutoScheduleDialog">
                            Xếp ca tự động
                        </v-btn>
                        <v-btn prepend-icon="mdi-download" variant="flat" class="admin-btn-export" @click="handleDownloadTemplate">
                            Xuất Excel
                        </v-btn>
                        <v-btn prepend-icon="mdi-upload" variant="flat" class="admin-btn-sky" @click="handleImport">
                            Nhập Excel
                        </v-btn>
                        <v-btn prepend-icon="mdi-plus" variant="flat" color="primary" class="add-btn-primary" @click="handleAdd">
                            Tạo lịch mới
                        </v-btn>
                    </template>
                </div>
            </div>

            <!-- View Selection Tabs -->
            <div class="border-b bg-white px-3">
                <v-tabs
                    v-model="mainTab"
                    bg-color="transparent"
                    color="primary"
                    class="admin-tabs"
                    height="48"
                >
                    <v-tab value="table" class="text-none px-6 font-weight-bold">
                        <v-icon start size="16" class="mr-1">mdi-table-large</v-icon>
                        Bảng lịch làm việc
                    </v-tab>
                    <v-tab value="calendar" class="text-none px-6 font-weight-bold">
                        <v-icon start size="16" class="mr-1">mdi-calendar</v-icon>
                        Lịch tuần/tháng
                    </v-tab>
                </v-tabs>
            </div>

            <!-- Table View Mode Content -->
            <div v-if="mainTab === 'table'" class="flex-grow-1 overflow-hidden d-flex flex-column" style="min-height: 0">
                <AdminTable
                    :hideToolbar="true"
                    :headers="tableHeaders"
                    :items="paginatedItems"
                    :loading="loading"
                    class="elevation-0 border-0"
                    style="border: none !important; box-shadow: none !important; margin-bottom: 0 !important"
                >
                    <template #row="{ item, index }">
                        <tr class="data-row">
                            <!-- STT -->
                            <td class="data-cell text-center">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                            <!-- Mã nhân viên -->
                            <td class="data-cell text-center">
                                <div class="text-truncate" :title="item.maNhanVien || getEmployeeCode(item)">
                                    {{ item.maNhanVien || getEmployeeCode(item) }}
                                </div>
                            </td>
                            <!-- Nhân viên -->
                            <td class="data-cell text-left px-4">
                                <div class="text-slate-800 text-truncate" :title="item.nhanVien">{{ item.nhanVien }}</div>
                            </td>
                            <!-- Ca làm -->
                            <td class="data-cell text-center">
                                <v-chip size="small" variant="flat" :class="['shift-chip', getShiftChipClass(item.ca)]">
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
                            <!-- Thao tác (Chỉ hiển thị cho Quản trị viên/Admin) -->
                            <td v-if="canManageSchedule" class="data-cell action-cell">
                                <div class="action-controls">
                                    <v-btn
                                        variant="text"
                                        color="primary"
                                        class="action-icon-btn"
                                        size="small"
                                        @click="handleEditSchedule(item)"
                                    >
                                        <component :is="ADMIN_ICONS.ACTION.EDIT" size="15" />
                                        <v-tooltip activator="parent" location="top">Sửa lịch</v-tooltip>
                                    </v-btn>
                                    <v-btn
                                        variant="text"
                                        color="primary"
                                        class="action-icon-btn"
                                        size="small"
                                        @click="openSwapDialog(item)"
                                    >
                                        <v-icon size="18">mdi-swap-horizontal</v-icon>
                                        <v-tooltip activator="parent" location="top">Đổi ca</v-tooltip>
                                    </v-btn>
                                </div>
                            </td>
                        </tr>
                    </template>
                    <template #pagination>
                        <AdminPagination
                            v-model="pagination.page"
                            :page-size="pagination.size"
                            @update:page-size="updatePaginationSize"
                            :total-pages="pagination.totalPages"
                            :total-elements="pagination.totalElements"
                            :current-size="paginatedItems.length"
                        />
                    </template>
                </AdminTable>
            </div>

            <!-- Calendar View Mode Content -->
            <div v-else class="flex-grow-1 overflow-hidden d-flex flex-column" style="background-color: #ffffff !important; min-height: 0">
                <!-- Calendar Sub-tab: Period Navigation + View toggles -->
                <div
                    class="d-flex align-center justify-space-between pa-3 border-b"
                    style="background-color: #ffffff !important; flex-wrap: wrap; gap: 8px"
                >
                    <!-- LEFT side: Navigation & Today -->
                    <div class="d-flex align-center ga-3">
                        <div class="d-flex align-center border rounded-lg bg-white px-2 py-1" style="height: 38px">
                            <v-btn
                                icon="mdi-chevron-left"
                                variant="text"
                                size="x-small"
                                color="slate-700"
                                class="mr-2"
                                @click="prevPeriod"
                            ></v-btn>
                            <span class="text-caption font-weight-bold text-slate-800 px-2 text-center" style="min-width: 210px">
                                {{ periodLabel }}
                            </span>
                            <v-btn
                                icon="mdi-chevron-right"
                                variant="text"
                                size="x-small"
                                color="slate-700"
                                class="ml-2"
                                @click="nextPeriod"
                            ></v-btn>
                        </div>
                    </div>

                    <!-- RIGHT side: Toggle buttons -->
                    <div class="calendar-tabs">
                        <button
                            class="calendar-tab-btn"
                            :class="{ 'calendar-tab-btn--active': calendarTab === 'week' }"
                            @click="calendarTab = 'week'"
                        >
                            Tuần
                        </button>
                        <button
                            class="calendar-tab-btn"
                            :class="{ 'calendar-tab-btn--active': calendarTab === 'month' }"
                            @click="calendarTab = 'month'"
                        >
                            Tháng
                        </button>
                    </div>
                </div>

                <!-- Calendar Display Bodies -->
                <div class="flex-grow-1 d-flex flex-column overflow-hidden" style="min-height: 0">
                    <!-- 1. WEEK VIEW - Dạng bảng grid (CA LÀM VIỆC | T2 | T3 | T4 | T5 | T6 | T7 | CN) -->
                    <div
                        v-if="calendarTab === 'week'"
                        class="d-flex flex-column flex-grow-1 overflow-hidden"
                        style="background-color: #ffffff !important; min-height: 0"
                    >
                        <div class="overflow-x-auto flex-grow-1" style="background-color: #ffffff !important">
                            <table class="cal-grid-table w-100">
                                <thead>
                                    <tr>
                                        <th class="cal-grid-th" style="width: 150px; min-width: 150px">CA LÀM VIỆC</th>
                                        <th
                                            v-for="dayObj in weekDays"
                                            :key="dayObj.date"
                                            class="cal-grid-th"
                                            :class="{ 'cal-grid-th-today': dayObj.isToday }"
                                            style="min-width: 140px"
                                        >
                                            <div class="cal-grid-day-name">{{ getDayAbbr(dayObj.dayName) }}</div>
                                            <div class="cal-grid-day-date">{{ formatDateDDMMYYYY(dayObj.date) }}</div>
                                        </th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="shift in rawShifts" :key="shift.id" class="cal-grid-row">
                                        <!-- Shift Column Header on Left -->
                                        <td class="cal-grid-shift-cell">
                                            <div class="cal-grid-shift-name" :class="getShiftColorClass(shift.tenCa)">
                                                {{ shift.tenCa }}
                                            </div>
                                            <div class="cal-grid-shift-time">
                                                {{ shift.gioBatDau.substring(0, 5) }} - {{ shift.gioKetThuc.substring(0, 5) }}
                                            </div>
                                        </td>
                                        <!-- Daily cells -->
                                        <td v-for="dayObj in weekDays" :key="dayObj.date" class="cal-grid-cell">
                                            <!-- Card container if there are schedules -->
                                            <div
                                                v-if="getSchedulesForCell(dayObj.date, shift.tenCa).length > 0"
                                                class="cal-grid-card"
                                                :class="getCellCardClass(shift.tenCa)"
                                            >
                                                <div class="cal-grid-card-title">{{ shift.tenCa }}</div>
                                                <div class="cal-grid-card-time">
                                                    {{ shift.gioBatDau.substring(0, 5) }} - {{ shift.gioKetThuc.substring(0, 5) }}
                                                </div>
                                                <div class="cal-grid-card-ratio">
                                                    {{ getSchedulesForCell(dayObj.date, shift.tenCa).length }}/{{ employeeOptions.length }}
                                                    Nhân viên
                                                </div>
                                                <button
                                                    class="cal-grid-btn-more"
                                                    @click.stop="
                                                        openCellEmployeesDialog(
                                                            dayObj.date,
                                                            shift.tenCa,
                                                            getSchedulesForCell(dayObj.date, shift.tenCa)
                                                        )
                                                    "
                                                >
                                                    Xem thêm
                                                </button>
                                            </div>
                                            <!-- Empty slot -->
                                            <div
                                                v-else
                                                class="cal-grid-cell-empty"
                                                :class="{ 'cursor-pointer': canManageSchedule }"
                                                @click="canManageSchedule ? handleAddForCellAndShift(dayObj.date, shift.tenCa) : null"
                                            >
                                                <v-icon v-if="canManageSchedule" size="18" color="grey-lighten-1">mdi-plus</v-icon>
                                                <div class="text-caption text-grey-lighten-1" :class="{ 'mt-1': canManageSchedule }">Trống</div>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr v-if="rawShifts.length === 0">
                                        <td colspan="8" class="text-center py-10 text-slate-400 text-caption">
                                            Không có danh mục ca làm việc nào.
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- 2. MONTH VIEW (Chia đều) -->
                    <div v-else-if="calendarTab === 'month'" class="calendar-container flex-grow-1 overflow-y-auto pa-4 bg-white" style="min-height: 0">
                        <div class="calendar-header-row mb-2">
                            <div
                                v-for="day in ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật']"
                                :key="day"
                                class="day-header"
                            >
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
                                        :class="['schedule-item-card py-1 px-2', getCellCardClass(s.ca)]"
                                        style="font-size: 11px; padding: 4px 6px !important"
                                        @click.stop="canManageSchedule ? handleEditSchedule(s) : null"
                                    >
                                        <div class="d-flex align-center justify-space-between w-100 text-truncate" style="gap: 4px">
                                            <span class="shift-time font-weight-bold" style="font-size: 9px">
                                                {{ getShiftTimeRange(s.ca) }}
                                            </span>
                                            <span class="text-truncate font-weight-black shift-employee" style="flex: 1">
                                                {{ s.nhanVien }}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </v-card>

        <!-- Cell Details Dialog (Xem thêm) -->
        <v-dialog v-model="showCellDetailDialog" max-width="750">
            <v-card class="rounded-xl pa-0 overflow-hidden">
                <div class="pa-4 bg-primary text-white d-flex align-center">
                    <v-icon color="white" class="mr-3">mdi-account-group-outline</v-icon>
                    <h3 class="text-h6 font-weight-bold mb-0">Nhân viên ca làm việc</h3>
                    <v-spacer></v-spacer>
                    <v-btn icon="mdi-close" variant="text" color="white" @click="showCellDetailDialog = false"></v-btn>
                </div>

                <v-card-text class="pa-4">
                    <div class="d-flex align-center justify-space-between mb-4">
                        <div>
                            <div class="text-subtitle-1 font-weight-bold text-slate-800">{{ selectedCellShift }}</div>
                            <div class="text-caption text-slate-500">
                                Ngày: {{ formatDateDDMMYYYY(selectedCellDate) }} ({{ getShiftTimeRange(selectedCellShift) }})
                            </div>
                        </div>
                        <v-btn
                            v-if="canManageSchedule"
                            prepend-icon="mdi-plus"
                            color="primary"
                            size="small"
                            variant="flat"
                            class="text-none"
                            style="border-radius: 6px !important"
                            @click="handleAddForCellAndShift(selectedCellDate, selectedCellShift)"
                        >
                            Thêm nhân viên
                        </v-btn>
                    </div>

                    <div class="border rounded-lg overflow-hidden">
                        <table class="native-admin-table w-100">
                            <thead>
                                <tr>
                                    <th class="header-cell text-center" style="width: 70px; white-space: nowrap">STT</th>
                                    <th class="header-cell text-center" style="width: 140px; white-space: nowrap">Mã NV</th>
                                    <th class="header-cell">Họ tên</th>
                                    <th v-if="canManageSchedule" class="header-cell text-center" style="width: 110px; white-space: nowrap">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(s, idx) in selectedCellSchedules" :key="s.id" class="data-row">
                                    <td class="data-cell text-center">{{ idx + 1 }}</td>
                                    <td class="data-cell text-center font-weight-bold">{{ s.maNhanVien || getEmployeeCode(s) }}</td>
                                    <td class="data-cell">{{ s.nhanVien }}</td>
                                    <td v-if="canManageSchedule" class="data-cell action-cell text-center">
                                        <div class="action-controls justify-center">
                                            <v-btn
                                                variant="text"
                                                color="primary"
                                                class="action-icon-btn"
                                                size="small"
                                                @click="openSwapDialog(s)"
                                            >
                                                <v-icon size="18">mdi-swap-horizontal</v-icon>
                                                <v-tooltip activator="parent" location="top">Đổi ca</v-tooltip>
                                            </v-btn>
                                        </div>
                                    </td>
                                </tr>
                                <TableEmptyState
                                    v-if="!selectedCellSchedules || selectedCellSchedules.length === 0"
                                    :colspan="4"
                                    text="Chưa có nhân viên nào đăng ký ca này."
                                />
                            </tbody>
                        </table>
                    </div>
                </v-card-text>
                <v-card-actions class="pa-4 border-t bg-slate-50">
                    <v-spacer></v-spacer>
                    <v-btn variant="outlined" color="grey" @click="showCellDetailDialog = false" class="rounded-lg">Đóng</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Swap Shift Dialog -->
        <v-dialog v-model="showSwapDialog" max-width="500">
            <v-card class="rounded-xl pa-4">
                <v-card-title class="text-h6 font-weight-bold d-flex align-center">
                    <v-icon color="primary" class="mr-3">mdi-swap-horizontal</v-icon>
                    Đổi ca làm việc
                </v-card-title>

                <v-card-text class="py-2">
                    <div class="mb-4 pa-3 bg-slate-50 rounded-lg text-caption text-slate-600 border border-slate-100">
                        Nhân viên nguồn: <strong class="text-slate-800">{{ swapSource.nhanVien }}</strong> ({{
                            swapSource.maNhanVien || getEmployeeCode(swapSource)
                        }})
                        <br />
                        Ca hiện tại: <strong class="text-primary">{{ swapSource.ca }}</strong> ngày
                        {{ formatDateDDMMYYYY(swapSource.ngay) }}
                    </div>

                    <div class="filter-field-label">Chọn nhân viên muốn đổi ca:</div>
                    <v-autocomplete
                        v-model="swapTargetEmployeeId"
                        :items="swapEmployeeList"
                        :item-title="(item) => item.displayText"
                        item-value="id"
                        placeholder="Tìm kiếm nhân viên (Tên, Mã)..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="mt-1 swap-autocomplete"
                        :menu-props="{ contentClass: 'swap-autocomplete-menu' }"
                    >
                        <template v-slot:item="{ props, item }">
                            <v-list-item v-bind="props" :title="`${item.raw.ma} - ${item.raw.ten}`" :subtitle="item.raw.statusText">
                                <template v-slot:prepend>
                                    <v-icon size="18" color="slate-400">mdi-account</v-icon>
                                </template>
                            </v-list-item>
                        </template>
                    </v-autocomplete>
                </v-card-text>

                <v-card-actions class="pa-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" color="grey" @click="showSwapDialog = false">Hủy</v-btn>
                    <v-btn color="primary" variant="flat" :disabled="!swapTargetEmployeeId" @click="executeSwap" class="px-6 rounded-lg"
                        >Xác nhận đổi</v-btn
                    >
                </v-card-actions>
            </v-card>
        </v-dialog>

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
                                disable-past
                                @update:model-value="
                                    (val) =>
                                        (addForm.ngay = val
                                            ? new Date(val.getTime() - val.getTimezoneOffset() * 60000).toISOString().substr(0, 10)
                                            : null)
                                "
                                placeholder="Chọn ngày làm"
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
                                        <v-chip
                                            size="x-small"
                                            variant="flat"
                                            :class="row.status === 'VALID' ? 'status-success' : 'status-error'"
                                        >
                                            {{ row.status === 'VALID' ? 'Hợp lệ' : 'Lỗi' }}
                                        </v-chip>
                                    </td>
                                </tr>
                                <TableEmptyState
                                    v-if="!importPreviewData || importPreviewData.length === 0"
                                    :colspan="5"
                                    text="Không có dữ liệu xem trước."
                                />
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

        <!-- Auto Schedule Confirm Dialog -->
        <v-dialog v-model="showAutoScheduleDialog" max-width="500">
            <v-card class="rounded-xl pa-4">
                <v-card-title class="text-h6 font-weight-bold d-flex align-center" style="color: #b91c1c;">
                    <v-icon color="red-darken-2" class="mr-3">mdi-alert-circle-outline</v-icon>
                    Xác nhận xếp ca tự động
                </v-card-title>
                <v-card-text class="py-2">
                    <p class="text-slate-700 mb-1" style="font-size: 14px;">
                        Chọn khoảng thời gian muốn xếp ca tự động:
                    </p>
                    <v-radio-group v-model="selectedAutoScheduleWeek" column hide-details class="mt-2 mb-4">
                        <v-radio value="current" color="primary" class="mb-2">
                            <template v-slot:label>
                                <div>
                                    <div class="font-weight-bold text-slate-800" style="font-size: 13px;">Tuần này</div>
                                    <div class="text-caption text-slate-500">{{ currentWeekStr }}</div>
                                </div>
                            </template>
                        </v-radio>
                        <v-radio value="next" color="primary">
                            <template v-slot:label>
                                <div>
                                    <div class="font-weight-bold text-slate-800" style="font-size: 13px;">Tuần sau</div>
                                    <div class="text-caption text-slate-500">{{ nextWeekStr }}</div>
                                </div>
                            </template>
                        </v-radio>
                    </v-radio-group>
                    <v-alert type="warning" variant="tonal" density="compact" class="text-caption rounded-lg">
                        <strong>Cảnh báo:</strong> Hành động này sẽ <strong>XÓA và GHI ĐÈ</strong> toàn bộ lịch làm việc hiện tại trong tuần được chọn. Dữ liệu cũ sẽ không thể khôi phục.
                    </v-alert>
                </v-card-text>
                <v-card-actions class="pa-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" color="grey" @click="showAutoScheduleDialog = false" :disabled="loading">Hủy</v-btn>
                    <v-btn color="error" variant="flat" @click="executeAutoSchedule" :loading="loading" class="px-6 rounded-lg">Đồng ý</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped lang="scss">
@use '@/scss/tokens' as *;

.admin-module-page,
.admin-table-container,
.main-view-tabs,
.calendar-view-mode {
    font-family: $body-font-family !important;

    * {
        font-family: $body-font-family;
    }
}

/* ============ Tab Bảng / Lịch Toggle ============ */
.admin-tabs :deep(.v-tab) {
    font-size: 13px !important;
    font-weight: 600 !important;
    text-transform: none !important;
    letter-spacing: 0.3px !important;
    min-height: 48px !important;
}

.admin-tabs :deep(.v-tab--selected) {
    color: #1e257c !important;
}

/* Legacy toggle (sub-tabs Tuan/Thang/Nam) */
.main-tab-toggle,
.sub-tab-toggle {
    border: 1px solid #e2e8f0 !important;
    height: 42px !important;
}

.main-tab-btn,
.sub-tab-btn {
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
    background: #ffffff;
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

.schedule-card-employee {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
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
    border: 2px solid #1e257c !important;
    background: #f0f1ff !important;
}

.month-day-cell.empty-cell {
    background: #f8fafc !important;
    border-style: dashed;
    opacity: 0.4;
    cursor: default;
}

/* Differentiate Saturday/Sunday columns in the month grid */
.calendar-grid-month .month-day-cell:nth-child(7n+6) {
    background-color: #f0f9ff;
}
.calendar-grid-month .month-day-cell:nth-child(7n+6):hover {
    background-color: #e0f2fe;
}

.calendar-grid-month .month-day-cell:nth-child(7n) {
    background-color: #fffafb;
}
.calendar-grid-month .month-day-cell:nth-child(7n):hover {
    background-color: #ffeef1;
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

/* Styling for schedule item cards in Month View with shift variations */
.schedule-item-card.card-green {
    border-color: #a7f3d0 !important;
    background-color: #f0fdf4 !important;
    color: #065f46 !important;
}
.schedule-item-card.card-green .shift-time {
    color: #047857 !important;
    opacity: 0.85;
}
.schedule-item-card.card-green .shift-employee {
    color: #065f46 !important;
}

.schedule-item-card.card-orange {
    border-color: #fde68a !important;
    background-color: #fffbeb !important;
    color: #92400e !important;
}
.schedule-item-card.card-orange .shift-time {
    color: #b45309 !important;
    opacity: 0.85;
}
.schedule-item-card.card-orange .shift-employee {
    color: #92400e !important;
}

.schedule-item-card.card-purple {
    border-color: #c7d2fe !important;
    background-color: #e0e7ff !important;
    color: #3730a3 !important;
}
.schedule-item-card.card-purple .shift-time {
    color: #4f46e5 !important;
    opacity: 0.85;
}
.schedule-item-card.card-purple .shift-employee {
    color: #3730a3 !important;
}

.schedule-item-card.card-slate {
    border-color: #e2e8f0 !important;
    background-color: #f8fafc !important;
    color: #334155 !important;
}
.schedule-item-card.card-slate .shift-time {
    color: #475569 !important;
    opacity: 0.85;
}
.schedule-item-card.card-slate .shift-employee {
    color: #334155 !important;
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
    color: #1e257c;
    padding: 10px 8px;
    text-transform: uppercase;
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

/* Style for Saturday and Sunday headers */
.day-header:nth-child(6) {
    background-color: #e0f2fe;
    color: #0284c7;
    border-color: #bae6fd;
}

.day-header:nth-child(7) {
    background-color: #ffeef1;
    color: #dc2626;
    border-color: #fecaca;
}

/* ===== MATRIX WEEK TABLE ===== */
.schedule-matrix-table {
    border-collapse: collapse;
    background: #fff;
    min-width: 900px;
    border: none;
}

.matrix-th {
    background: #ffffff !important;
    border: none !important;
    border-bottom: 2px solid #e2e8f0 !important;
    border-right: 1px solid #f1f5f9 !important;
    padding: 14px 8px !important;
    font-size: 12px !important;
    font-weight: 700 !important;
    color: #1e257c !important;
    text-align: center;
    white-space: nowrap;
}

.matrix-th:last-child {
    border-right: none !important;
}

.day-col-header {
    min-width: 120px;
}

.today-col-header {
    background: #f8fafc !important;
}

.day-col-label {
    font-size: 12px !important;
    font-weight: 700 !important;
    color: #1e257c !important;
}

.today-col-header .day-col-label {
    color: #1e257c !important;
}

.day-col-date {
    font-size: 11px;
    color: #64748b;
    margin-top: 2px;
}

.today-col-header .day-col-date {
    color: #1e257c !important;
    font-weight: 600;
}

.matrix-row:hover {
    background: #f8fafc;
}

.matrix-td {
    border: none !important;
    border-bottom: 1px solid #f1f5f9 !important;
    border-right: 1px solid #f1f5f9 !important;
    padding: 8px 6px;
    vertical-align: middle;
    font-size: 13px;
}

.matrix-td:last-child {
    border-right: none !important;
}

.matrix-emp-code {
    display: inline-block;
    background: #ffffff;
    color: #475569;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 6px;
    border: 1px solid #e2e8f0;
}

.matrix-day-cell {
    cursor: pointer;
    min-width: 120px;
}

.matrix-day-cell:hover {
    background: #f0f9ff;
}

.matrix-today-cell {
    background: #eff6ff !important;
}

.matrix-shifts {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

/* .matrix-shift-chip + .shift-morning/afternoon/night/default đã chuyển vào
   file chung src/scss/_admin-chips.scss để quản lý tập trung. */
.matrix-shift-name {
    font-size: 11px;
    font-weight: 700;
}

.matrix-shift-time {
    font-size: 10px;
    font-weight: 500;
    opacity: 0.75;
    margin-top: 1px;
}

.matrix-empty-cell {
    text-align: center;
    color: #cbd5e1;
    font-size: 16px;
    font-weight: 300;
    padding: 4px;
}

.pagination-footer {
    background: #ffffff !important;
}

/* Filter Custom Layout Styles */
.filter-item-wrap {
    display: flex;
    align-items: center;
    gap: 8px;
}

.filter-inline-label {
    font-size: 13px;
    font-weight: 700;
    color: #334155;
    white-space: nowrap;
}

.flex-shrink-0 {
    flex-shrink: 0 !important;
}

.premium-card :deep(.v-field),
.premium-card :deep(.v-field__outline) {
    border-radius: 8px !important;
}

.premium-card :deep(.v-field__input),
.premium-card :deep(input),
.premium-card :deep(.v-select__selection-text),
.premium-card :deep(.v-field__placeholder),
.premium-card :deep(input::placeholder) {
    font-size: 13px !important;
}

.filter-action-btn {
    font-size: 13px !important;
    height: 38px !important;
    font-weight: 700 !important;
    letter-spacing: normal !important;
    text-transform: none !important;
    border-radius: 8px !important;
}

.btn-primary-theme {
    background-color: #1e257c !important;
    color: #ffffff !important;
}

.btn-dark {
    background-color: #0f172a !important;
    color: #ffffff !important;
}

.btn-orange-theme {
    background-color: #ea580c !important;
    color: #ffffff !important;
    box-shadow: 0 4px 14px rgba(234, 88, 12, 0.3) !important;
}
.btn-orange-theme:hover {
    filter: brightness(1.1) !important;
}

.btn-green-theme {
    background-color: #107c41 !important;
    color: #ffffff !important;
    box-shadow: 0 4px 14px rgba(16, 124, 65, 0.3) !important;
}
.btn-green-theme:hover {
    filter: brightness(1.1) !important;
}

/* Calendar Sub-tabs style (Ngày / Tuần / Tháng) */
.calendar-tabs {
    display: flex;
    align-items: center;
    background: #f1f5f9;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 3px;
    gap: 2px;
    height: 38px;
}

.calendar-tab-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    padding: 0 16px;
    border-radius: 6px;
    border: none;
    background: transparent;
    color: #64748b;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.15s ease;
    white-space: nowrap;
}

.calendar-tab-btn:hover:not(:disabled) {
    background: #e2e8f0;
    color: #0f172a;
}

.calendar-tab-btn--active {
    background: #ffffff !important;
    color: #0f172a !important;
    font-weight: 700 !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1) !important;
}

.calendar-tab-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

/* Weekly grid styles */
.cal-grid-table {
    border-collapse: collapse;
    width: 100%;
    background-color: #ffffff;
}

.cal-grid-th {
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    padding: 12px 8px;
    text-align: center;
    vertical-align: middle;
}

.cal-grid-th-today {
    background-color: #eff6ff;
    border-bottom: 2px solid #1e257c;
}

.cal-grid-day-name {
    font-size: 13px;
    font-weight: 700;
    color: #475569;
}

.cal-grid-day-date {
    font-size: 11px;
    font-weight: 500;
    color: #64748b;
    margin-top: 2px;
}

.cal-grid-row {
    border-bottom: 1px solid #e2e8f0;
}

.cal-grid-shift-cell {
    border: 1px solid #e2e8f0;
    padding: 16px;
    text-align: center;
    vertical-align: middle;
    background-color: #f8fafc;
}

.cal-grid-shift-name {
    font-size: 13px;
    font-weight: 700;
    margin-bottom: 4px;
}

.cal-grid-shift-time {
    font-size: 11px;
    font-weight: 500;
    color: #64748b;
}

.cal-grid-cell {
    border: 1px solid #e2e8f0;
    padding: 10px;
    vertical-align: middle;
    height: 160px; /* fixed height for alignment */
    min-width: 160px;
}

/* Card Cell Styles */
.cal-grid-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 12px 8px;
    border-radius: 10px;
    text-align: center;
    height: 100%;
    transition: all 0.2s ease;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.cal-grid-card-title {
    font-size: 13px;
    font-weight: 700;
    margin-bottom: 2px;
}

.cal-grid-card-time {
    font-size: 11px;
    font-weight: 500;
    opacity: 0.8;
    margin-bottom: 4px;
}

.cal-grid-card-ratio {
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 8px;
}

.cal-grid-btn-more {
    background-color: #ffffff !important;
    border-radius: 20px !important;
    padding: 4px 14px !important;
    font-size: 11px !important;
    font-weight: 700 !important;
    cursor: pointer;
    transition: all 0.15s ease;
    outline: none;
}

/* Card styles by shifts */
.card-green {
    border: 1px solid #a7f3d0 !important;
    background-color: #f0fdf4 !important;
    color: #065f46 !important;
}
.card-green .cal-grid-btn-more {
    border: 1px solid #10b981 !important;
    color: #047857 !important;
}
.card-green .cal-grid-btn-more:hover {
    background-color: #d1fae5 !important;
}

.card-orange {
    border: 1px solid #fde68a !important;
    background-color: #fffbeb !important;
    color: #92400e !important;
}
.card-orange .cal-grid-btn-more {
    border: 1px solid #f59e0b !important;
    color: #b45309 !important;
}
.card-orange .cal-grid-btn-more:hover {
    background-color: #fef3c7 !important;
}

.card-purple {
    border: 1px solid #c7d2fe !important;
    background-color: #e0e7ff !important;
    color: #3730a3 !important;
}
.card-purple .cal-grid-btn-more {
    border: 1px solid #6366f1 !important;
    color: #4f46e5 !important;
}
.card-purple .cal-grid-btn-more:hover {
    background-color: #c7d2fe !important;
}

.card-slate {
    border: 1px solid #e2e8f0 !important;
    background-color: #f8fafc !important;
    color: #334155 !important;
}
.card-slate .cal-grid-btn-more {
    border: 1px solid #94a3b8 !important;
    color: #475569 !important;
}
.card-slate .cal-grid-btn-more:hover {
    background-color: #f1f5f9 !important;
}

/* Empty Cell Styles */
.cal-grid-cell-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 1px dashed #cbd5e1;
    border-radius: 10px;
    height: 100%;
    cursor: pointer;
    transition: all 0.2s ease;
    color: #94a3b8;
}

.cal-grid-cell-empty:hover {
    background-color: #f8fafc;
    border-color: #94a3b8;
    color: #64748b;
}

/* Shift Text Color Variations */
.color-green {
    color: #10b981 !important;
}
.color-orange {
    color: #f97316 !important;
}
.color-purple {
    color: #6366f1 !important;
}
.color-slate {
    color: #64748b !important;
}

/* Custom Swap Autocomplete styling */
.swap-autocomplete :deep(.v-field__input),
.swap-autocomplete :deep(.v-field),
.swap-autocomplete :deep(input),
.swap-autocomplete :deep(.v-autocomplete__selection-text) {
    font-size: 13px !important;
}

:global(.swap-autocomplete-menu .v-list-item-title) {
    font-size: 13px !important;
    font-weight: 600 !important;
    color: #1e293b !important;
}

:global(.swap-autocomplete-menu .v-list-item-subtitle) {
    font-size: 11px !important;
    color: #64748b !important;
}

:global(.swap-autocomplete-menu .v-list-item) {
    min-height: 40px !important;
    padding-top: 4px !important;
    padding-bottom: 4px !important;
}

:deep(.v-field__input),
:deep(.v-field__input input),
:deep(.v-field__input input::placeholder),
:deep(.v-select__selection),
:deep(.v-select__selection-text),
:deep(.v-autocomplete__selection),
:deep(.v-autocomplete__selection-text) {
    font-size: 13px !important;
}
</style>
