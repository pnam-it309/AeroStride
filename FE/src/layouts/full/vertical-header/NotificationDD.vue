<script setup>
import { BellRingingIcon, CircleCheckIcon, InfoCircleIcon, AlertTriangleIcon, TrashIcon, EditIcon } from 'vue-tabler-icons';
import { useNotifications } from '@/services/notificationService';

const { notifications, markAllAsRead } = useNotifications();

// Map string icons to actual components
const iconMap = {
  CircleCheckIcon,
  InfoCircleIcon,
  AlertTriangleIcon,
  TrashIcon,
  EditIcon
};
</script>

<template>
  <v-menu :close-on-content-click="false" location="bottom end" transition="slide-y-transition">
    <template v-slot:activator="{ props }">
      <v-btn icon variant="text" class="custom-hover-primary text-muted ml-2" v-bind="props" @click="markAllAsRead">
        <v-badge :content="notifications.filter(n => n.isNew).length" :model-value="notifications.filter(n => n.isNew).length > 0" color="primary" offset-x="-5" offset-y="-3">
          <BellRingingIcon stroke-width="1.8" size="22" />
        </v-badge>
      </v-btn>
    </template>

    <v-card width="380" class="premium-notification-card">
      <!-- Header -->
      <div class="pa-5 header-navy d-flex align-center justify-space-between">
        <h3 class="text-subtitle-1 text-uppercase tracking-wider" style="color: #ffffff !important; font-size: 16px !important; font-weight: 600 !important;">Hoạt động gần đây</h3>
        <v-chip v-if="notifications.length > 0" size="x-small" class="notification-count-chip">
          {{ notifications.length }} THÔNG BÁO
        </v-chip>
      </div>

      <!-- Empty State -->
      <div v-if="notifications.length === 0" class="pa-10 text-center">
        <v-icon size="48" color="grey-lighten-2" class="mb-3">mdi-bell-outline</v-icon>
        <div class="text-subtitle-2 font-weight-bold text-slate-700">Chưa có thông báo nào</div>
        <div class="text-caption text-slate-400 mt-1">Các hoạt động của bạn sẽ xuất hiện tại đây</div>
      </div>

      <!-- List -->
      <v-list v-else class="pa-0 notification-list" lines="two">
        <v-list-item
          v-for="(item, i) in notifications"
          :key="item.id"
          class="notification-item pa-5"
          :active="item.isNew"
        >
          <template v-slot:prepend>
            <div class="icon-wrapper mr-4" :style="`background-color: var(--v-${item.color}-lighten-5)`">
              <div class="icon-inner" :style="`background-color: var(--v-${item.color}-lighten-4)`">
                <component :is="iconMap[item.icon] || CircleCheckIcon" size="20" :style="`color: var(--v-${item.color}-base)`" />
              </div>
            </div>
          </template>

          <template v-slot:title>
            <div class="text-subtitle-2 font-weight-bold text-slate-800 mb-1">{{ item.title }}</div>
          </template>

          <template v-slot:subtitle>
            <div class="text-caption text-slate-500 line-clamp-2 leading-relaxed">
              {{ item.subtitle }}
            </div>
            <div class="text-overline font-weight-bold text-slate-400 mt-1 d-block">{{ item.time }}</div>
          </template>
        </v-list-item>
      </v-list>

      <!-- Footer -->
      <div class="pa-3 bg-slate-50">
        <v-btn block variant="text" color="primary" class="text-none font-weight-bold py-2 footer-btn" size="large">
          Xem tất cả nhật ký
        </v-btn>
      </div>
    </v-card>
  </v-menu>
</template>

<style scoped>
.premium-notification-card {
  border-radius: 20px !important;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15) !important;
  overflow: hidden;
  background: white;
  font-family: inherit;
}

.header-navy {
  background-color: #1e257c !important;
}

.notification-count-chip {
  background-color: rgba(255, 255, 255, 0.2) !important;
  color: white !important;
  font-weight: 700 !important;
  font-size: 10px !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
}

.notification-list {
  max-height: 480px;
  overflow-y: auto;
}

.notification-item {
  border-bottom: 1px solid #f1f5f9;
  transition: all 0.2s ease;
}

.notification-item:hover {
  background-color: #f8fafc;
}

.icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-inner {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.footer-btn {
  color: #1e257c !important;
  font-size: 14px;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.leading-relaxed {
  line-height: 1.5;
}

:deep(.v-badge__wrapper .v-badge__badge) {
  color: white !important;
  font-weight: 700 !important;
}
</style>
