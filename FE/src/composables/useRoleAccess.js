import { computed } from 'vue';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { APP_ROLES } from '@/constants/appConstants';

/**
 * Composable dùng chung quản lý và phân quyền vai trò người dùng (Admin, Staff, Customer).
 * Sử dụng chuẩn hằng số APP_ROLES từ appConstants.js.
 */
export function useRoleAccess() {
    const currentUser = computed(() => dichVuXacThuc.layUserHienTai());

    const userRole = computed(() => {
        return currentUser.value?.role || '';
    });

    const isAdmin = computed(() => userRole.value === APP_ROLES.ADMIN);
    const isStaff = computed(() => userRole.value === APP_ROLES.STAFF);
    const isCustomer = computed(() => userRole.value === APP_ROLES.CUSTOMER);

    // Quyền quản lý lịch làm việc (Chỉ dành cho Admin)
    const canManageSchedule = computed(() => isAdmin.value);

    // Quyền quản trị hệ thống (Chỉ dành cho Admin)
    const canManageAdmin = computed(() => isAdmin.value);

    return {
        currentUser,
        userRole,
        isAdmin,
        isStaff,
        isCustomer,
        canManageSchedule,
        canManageAdmin
    };
}
