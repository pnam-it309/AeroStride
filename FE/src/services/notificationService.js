import { ref } from 'vue';

const notifications = ref([]);
const toast = ref({
  id: 0,
  show: false,
  title: '',
  subtitle: '',
  color: 'success',
  icon: 'CircleCheckIcon'
});

const resolveDefaultIcon = (color) => {
  if (color === 'error') return 'AlertTriangleIcon';
  if (color === 'warning') return 'AlertTriangleIcon';
  if (color === 'info') return 'InfoCircleIcon';
  return 'CircleCheckIcon';
};

export const useNotifications = () => {
  const addNotification = (notif) => {
    const normalized = {
      id: Date.now(),
      title: notif.title || 'Thông báo hệ thống',
      subtitle: notif.subtitle || '',
      time: 'Vừa xong',
      icon: notif.icon || resolveDefaultIcon(notif.color),
      color: notif.color || 'success',
      isNew: true
    };

    notifications.value.unshift(normalized);

    toast.value = {
      id: normalized.id,
      show: true,
      title: normalized.title,
      subtitle: normalized.subtitle,
      color: normalized.color,
      icon: normalized.icon
    };
  };

  const markAllAsRead = () => {
    notifications.value.forEach(n => n.isNew = false);
  };

  const hideToast = () => {
    toast.value = {
      ...toast.value,
      show: false
    };
  };

  return {
    notifications,
    toast,
    addNotification,
    markAllAsRead,
    hideToast
  };
};
