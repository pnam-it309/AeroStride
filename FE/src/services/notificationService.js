import { ref } from 'vue';
import { useToastStore } from '@/stores/toastStore';

const notifications = ref([]);

export const useNotifications = () => {
  const addNotification = (notif) => {
    const toast = useToastStore();
    
    // Add to notification list (for the bell icon)
    notifications.value.unshift({
      id: Date.now(),
      title: notif.title || 'Thông báo hệ thống',
      subtitle: notif.subtitle || '',
      time: 'Vừa xong',
      icon: notif.icon || 'CircleCheckIcon',
      color: notif.color || 'success',
      isNew: true
    });

    // Show toast (snackbar)
    const toastType = notif.color === 'error' ? 'error' : (notif.color === 'warning' ? 'warning' : 'success');
    toast.showToast(
      notif.subtitle || notif.title || 'Thông báo mới',
      notif.color || 'success',
      notif.icon || (notif.color === 'error' ? 'mdi-alert-circle' : 'mdi-check-circle'),
      3000
    );
  };

  const markAllAsRead = () => {
    notifications.value.forEach(n => n.isNew = false);
  };

  return {
    notifications,
    addNotification,
    markAllAsRead
  };
};
