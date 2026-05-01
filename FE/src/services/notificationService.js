import { ref } from 'vue';

const notifications = ref([]);

export const useNotifications = () => {
  const addNotification = (notif) => {
    notifications.value.unshift({
      id: Date.now(),
      title: notif.title || 'Thông báo hệ thống',
      subtitle: notif.subtitle || '',
      time: 'Vừa xong',
      icon: notif.icon || 'CircleCheckIcon',
      color: notif.color || 'success',
      isNew: true
    });
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
