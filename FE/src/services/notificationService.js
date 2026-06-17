import { ref } from 'vue';
import { useToastStore } from '@/stores/toastStore';

const notifications = ref([]);

export const useNotifications = () => {
  const addNotification = (notif) => {
    const toast = useToastStore();

    // Add to notification list (for the bell icon)
    notifications.value.unshift({
      id: Date.now(),
      title: notif.title || 'Thông báo h? th?ng',
      subtitle: notif.subtitle || '',
      time: 'V?a xong',
      icon: notif.icon || 'CircleCheckIcon',
      color: notif.color || 'success',
      isNew: true
    });

    // Use the passed color or default to primary
    let displayColor = notif.color || 'primary';

    // Show toast (snackbar) with 5s timeout
    toast.showToast(
      notif.subtitle || notif.title || 'Thông báo m?i',
      displayColor,
      notif.icon || (displayColor === 'error' ? 'mdi-alert-circle' : (displayColor === 'warning' ? 'mdi-alert' : 'mdi-check-circle')),
      notif.timeout || 2000
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
