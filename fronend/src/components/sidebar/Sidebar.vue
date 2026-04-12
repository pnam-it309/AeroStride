<template>
  <aside class="sidebar" :class="{ open: open, collapsed: collapsed }">
    <div class="brand">
      <img class="logoMark" :src="logoSrc" alt="AeroStride logo" />
    </div>

    <nav class="menu">
      <template v-for="item in sidebarItems" :key="item.label">
        <SidebarItem
          v-if="!item.children"
          :to="item.to"
          :label="item.label"
          :icon="item.icon"
          :collapsed="collapsed"
        />

        <div v-else class="group">
          <button
            class="groupBtn"
            :class="{ active: isGroupActive(item) }"
            :title="item.label"
            @click="onGroupClick(item)"
          >
            <span class="groupMain">
              <Icon :name="item.icon" />
              <span v-show="!collapsed">{{ item.label }}</span>
            </span>
            <Icon
              v-show="!collapsed"
              name="chevron"
              class="groupArrow"
              :class="{ open: isExpanded(item.label) }"
            />
          </button>

          <div v-show="!collapsed && isExpanded(item.label)" class="children">
            <SidebarItem
              v-for="child in item.children"
              :key="child.label"
              :to="child.to"
              :label="child.label"
              :icon="child.icon"
              :collapsed="collapsed"
            />
          </div>
        </div>
      </template>
    </nav>

    <button class="logout" title="Đăng xuất" @click="logout">
      <Icon name="logout" />
      <span v-show="!collapsed">Đăng xuất</span>
    </button>
  </aside>
</template>

<script setup>
import { reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import { sidebarItems } from "@/data/sidebar";
import { useAuthStore } from "@/stores/auth.store";
import SidebarItem from "@/components/sidebar/SidebarItem.vue";
import Icon from "@/components/common/Icon.vue";
import logoSrc from "@/assets/logo.jpg";

const props = defineProps({
  open: { type: Boolean, default: true },
  collapsed: { type: Boolean, default: false },
});

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const expanded = reactive({
  "Thuộc tính": true,
});

function isExpanded(label) {
  return Boolean(expanded[label]);
}

function toggleGroup(label) {
  expanded[label] = !expanded[label];
}

function onGroupClick(item) {
  if (!item.children?.length) return;
  if (props.collapsed) {
    router.push(item.children[0].to);
    return;
  }
  toggleGroup(item.label);
}

function isToActive(to) {
  if (typeof to === "string") {
    if (to === "/") return route.path === "/";
    return route.path.startsWith(to);
  }

  if (to?.name && route.name !== to.name) return false;
  const query = to?.query || {};
  return Object.keys(query).every(
    (key) => String(route.query[key] || "") === String(query[key]),
  );
}

function isGroupActive(item) {
  return item.children?.some((child) => isToActive(child.to));
}

function logout() {
  auth.logout();
  router.push({ name: "login" });
}
</script>

<style scoped>
.sidebar {
  width: 250px;
  height: 100vh;
  position: sticky;
  top: 0;
  background: #ffffff;
  border-right: 1px solid var(--border);
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 12px;
  padding: 16px 12px;
  transition:
    width 0.2s ease,
    padding 0.2s ease;
}

.sidebar.collapsed {
  width: 84px;
  padding: 14px 8px;
}

.brand {
  display: grid;
  justify-items: center;
  padding: 6px 6px 12px;
}

.logoMark {
  width: 132px;
  height: 132px;
  object-fit: contain;
}

.sidebar.collapsed .logoMark {
  width: 62px;
  height: 62px;
}

.menu {
  display: grid;
  align-content: start;
  gap: 7px;
  overflow: auto;
  padding-right: 4px;
}

.sidebar.collapsed .menu {
  justify-items: center;
}

.group {
  display: grid;
  gap: 7px;
}

.groupBtn {
  border: 0;
  background: transparent;
  width: 100%;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #334155;
  padding: 9px 14px;
  font-size: 15px;
  letter-spacing: 0.2px;
  line-height: 1.35;
  font-weight: 600;
  cursor: pointer;
}

.sidebar.collapsed .groupBtn {
  width: 52px;
  justify-content: center;
  padding: 9px;
}

.groupBtn:hover {
  background: #f2f6ff;
}

.groupBtn.active {
  color: var(--primary);
  background: var(--primary-soft);
}

.groupMain {
  display: inline-flex;
  align-items: center;
  gap: 13px;
}

.groupArrow {
  transition: transform 0.2s ease;
}

.groupArrow.open {
  transform: rotate(90deg);
}

.children {
  display: grid;
  gap: 4px;
  padding-left: 12px;
}

.children :deep(.item) {
  padding-left: 10px;
  border-radius: 12px;
}

.children :deep(.item span) {
  font-size: 15px;
}

.logout {
  height: 42px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: #ffffff;
  color: #334155;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 0.2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
}

.sidebar.collapsed .logout {
  width: 52px;
  justify-self: center;
  padding: 0;
}

.logout:hover {
  border-color: #c8d5ec;
  background: #f6f9ff;
}

@media (max-width: 1024px) {
  .sidebar {
    position: fixed;
    left: -260px;
    z-index: 50;
    box-shadow: var(--shadow-md);
    transition: left 0.22s ease;
  }

  .sidebar.open {
    left: 0;
  }
}
</style>
