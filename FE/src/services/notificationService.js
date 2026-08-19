import { ref } from 'vue';
import { useToastStore } from '@/stores/toastStore';
import { formatUserErrorMessage, getBackendErrorMessage } from '@/utils/errorUtils';

const notifications = ref([]);

export const useNotifications = () => {
    const addNotification = (notif) => {
        const toast = useToastStore();

        const rawSubtitle = notif.subtitle || notif.message || '';
        const userSubtitle = typeof rawSubtitle === 'object' && rawSubtitle !== null
            ? getBackendErrorMessage(rawSubtitle, 'Thao tác không thành công', 'Notification')
            : formatUserErrorMessage(rawSubtitle, 'Thao tác không thành công');

        const userTitle = formatUserErrorMessage(notif.title || 'Thông báo hệ thống');

        // Add to notification list (for the bell icon)
        notifications.value.unshift({
            id: Date.now(),
            title: userTitle,
            subtitle: userSubtitle,
            time: 'Vừa xong',
            icon: notif.icon || 'CircleCheckIcon',
            color: notif.color || 'success',
            isNew: true
        });

        // Use the passed color or default to primary
        let displayColor = notif.color || 'primary';
        const defaultTimeout = displayColor === 'error' ? 3000 : displayColor === 'warning' ? 2500 : 2000;

        // Show toast (snackbar) with default timeout
        toast.showToast(
            userSubtitle || userTitle || 'Thông báo mới',
            displayColor,
            notif.icon || (displayColor === 'error' ? 'mdi-alert-circle' : displayColor === 'warning' ? 'mdi-alert' : 'mdi-check-circle'),
            notif.timeout || defaultTimeout
        );
    };

    const markAllAsRead = () => {
        notifications.value.forEach((n) => (n.isNew = false));
    };

    return {
        notifications,
        addNotification,
        markAllAsRead
    };
};
