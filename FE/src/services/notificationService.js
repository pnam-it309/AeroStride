import { ref } from 'vue';
import { useToastStore } from '@/stores/toastStore';

const notifications = ref([]);

export const useNotifications = () => {
    const addNotification = (notif) => {
        const toast = useToastStore();

        // Change default success color (green) to primary (blue)
        let displayColor = notif.color || 'primary';
        if (displayColor === 'success') {
            displayColor = 'primary';
        }

        // Add to notification list (for the bell icon)
        notifications.value.unshift({
            id: Date.now(),
            title: notif.title || 'Thông báo hệ thống',
            subtitle: notif.subtitle || '',
            time: 'Vừa xong',
            icon: notif.icon || 'CircleCheckIcon',
            color: displayColor,
            isNew: true
        });

        // Show toast (snackbar)
        toast.showToast(
            notif.subtitle || notif.title || 'Thông báo mới',
            displayColor,
            notif.icon || (displayColor === 'error' ? 'mdi-alert-circle' : 'mdi-check-circle'),
            3000
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
