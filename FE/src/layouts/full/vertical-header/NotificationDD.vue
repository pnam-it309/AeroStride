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

    <v-card width="360" class="rounded-0 border shadow-2xl elevation-0 overflow-hidden">
      <!-- Header -->
      <div class="pa-4 bg-primary-lighten-5 d-flex align-center justify-space-between border-b">
        <h3 class="text-subtitle-1 font-weight-black text-primary text-uppercase tracking-wider">Hoạt động gần đây</h3>
        <v-chip v-if="notifications.length > 0" size="x-small" color="primary" variant="flat" class="font-weight-black px-2">
          {{ notifications.length }} THÔNG BÁO
        </v-chip>
      </div>

      <!-- Empty State -->
      <div v-if="notifications.length === 0" class="pa-10 text-center">
        <v-icon size="48" color="grey-lighten-2" class="mb-2">mdi-bell-outline</v-icon>
        <div class="text-subtitle-2 font-weight-black text-grey">Chưa có thông báo nào</div>
        <div class="text-caption text-grey">Các hoạt động của bạn sẽ xuất hiện tại đây</div>
      </div>

      <!-- List -->
      <v-list v-else class="pa-0 notification-list" lines="two">
        <v-list-item
          v-for="(item, i) in notifications"
          :key="item.id"
          class="border-b notification-item pa-4"
          :active="item.isNew"
          color="primary"
        >
          <template v-slot:prepend>
            <div :class="`bg-${item.color}-lighten-4 pa-2 mr-4 rounded-0 d-flex align-center justify-center`" style="width: 40px; height: 40px;">
              <component :is="iconMap[item.icon] || CircleCheckIcon" size="24" :class="`text-${item.color}`" />
            </div>
          </template>

          <template v-slot:title>
            <div class="text-subtitle-2 font-weight-black text-dark mb-1">{{ item.title }}</div>
          </template>

          <template v-slot:subtitle>
            <div class="text-caption font-weight-bold text-medium-emphasis line-clamp-2 leading-tight">
              {{ item.subtitle }}
            </div>
            <div class="text-overline font-weight-black text-grey mt-1 d-block">{{ item.time }}</div>
          </template>
        </v-list-item>
      </v-list>

      <v-divider></v-divider>
      
      <!-- Footer -->
      <v-btn block variant="flat" color="grey-lighten-4" class="text-none font-weight-black py-4 rounded-0" size="large">
        Xem tất cả nhật ký
      </v-btn>
    </v-card>
  </v-menu>
</template>

<style scoped>
.notification-list {
  max-height: 450px;
  overflow-y: auto;
}
.notification-item:hover {
  background-color: #f8fafc;
}
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-clamp: 2; /* Added for compatibility */
}
.leading-tight {
  line-height: 1.25;
}
.shadow-2xl {
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}
</style>
