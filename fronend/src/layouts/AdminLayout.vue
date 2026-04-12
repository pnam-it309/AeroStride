<template>
  <div class="layout">
    <Sidebar :open="sidebarOpen" :collapsed="sidebarCollapsed" />
    <div
      class="overlay"
      :class="{ show: sidebarOpen }"
      @click="sidebarOpen = false"
    ></div>

    <div class="main">
      <header class="topbar">
        <div class="leftActions">
          <button class="menuBtn mobile" @click="sidebarOpen = !sidebarOpen">
            <Icon name="menu" />
          </button>
          <button
            class="menuBtn desktop"
            @click="sidebarCollapsed = !sidebarCollapsed"
            title="Thu gọn thanh bên"
          >
            <Icon :name="sidebarCollapsed ? 'chevron-right' : 'chevron-left'" />
          </button>
        </div>

        <div class="rightActions">
          <button class="iconBtn" title="Thông báo">
            <Icon name="bell" />
          </button>
          <div class="avatar" :title="auth.avatarTitle">
            {{ auth.avatarText }}
          </div>
        </div>
      </header>

      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import Sidebar from "@/components/sidebar/Sidebar.vue";
import Icon from "@/components/common/Icon.vue";
import { useAuthStore } from "@/stores/auth.store";

const sidebarOpen = ref(false);
const sidebarCollapsed = ref(false);
const auth = useAuthStore();
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  overflow-x: hidden;
}

.main {
  min-width: 0;
  flex: 1;
  display: grid;
  grid-template-rows: auto 1fr;
  overflow-x: hidden;
}

.topbar {
  height: 64px;
  border-bottom: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  position: sticky;
  top: 0;
  z-index: 20;
}

.menuBtn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  color: #334155;
  display: inline-grid;
  place-items: center;
}

.leftActions,
.rightActions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.menuBtn.mobile {
  display: none;
}

.iconBtn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  display: inline-grid;
  place-items: center;
}

.avatar {
  min-width: 38px;
  padding: 0 10px;
  height: 38px;
  border-radius: 999px;
  background: linear-gradient(135deg, #2c4f96, #5477c0);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.content {
  padding: 16px;
  overflow-x: hidden;
}

.overlay {
  display: none;
}

@media (max-width: 1024px) {
  .menuBtn.mobile {
    display: inline-grid;
    place-items: center;
  }

  .menuBtn.desktop {
    display: none;
  }

  .overlay {
    position: fixed;
    inset: 0;
    z-index: 40;
    background: rgba(15, 23, 42, 0.25);
    display: block;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.2s ease;
  }

  .overlay.show {
    opacity: 1;
    pointer-events: auto;
  }
}
</style>
