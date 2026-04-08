<script setup>
const profileDD = [
  {
    avatar: 'mdi-account-outline',
    title: 'Hồ sơ của tôi',
    subtitle: 'Cài đặt tài khoản',
  },
  {
    avatar: 'mdi-email-outline',
    title: 'Tài khoản của tôi',
    subtitle: 'Tin nhắn & Email',
  },
  {
    avatar: 'mdi-format-list-bulleted',
    title: 'Nhiệm vụ của tôi',
    subtitle: 'Việc cần làm',
  },
];

const handleLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  window.location.href = '/auth/login';
};
</script>

<template>
  <v-menu :close-on-content-click="false">
    <template v-slot:activator="{ props }">
      <v-btn class="profileBtn" variant="text" v-bind="props" icon>
        <v-avatar size="35">
          <img src="@/assets/images/profile/user-1.jpg" height="35" alt="user" />
        </v-avatar>
      </v-btn>
    </template>
    <v-sheet rounded="md" width="200" elevation="10" class="mt-2">
      <v-list class="py-0" lines="one" density="compact">
        <v-list-item v-for="(item, i) in profileDD" :key="i" :value="item" color="primary" class="py-2 px-4 shadow-none">
          <template v-slot:prepend>
            <v-icon :icon="item.avatar" size="20" class="mr-3"></v-icon>
          </template>
          <v-list-item-title class="text-subtitle-2 font-weight-bold">{{ item.title }}</v-list-item-title>
        </v-list-item>
      </v-list>
      <div class="pt-4 pb-4 px-5 text-center">
        <v-btn color="primary" variant="outlined" block @click="handleLogout" class="rounded-pill font-weight-bold">Đăng xuất</v-btn>
      </div>
    </v-sheet>
  </v-menu>
</template>
