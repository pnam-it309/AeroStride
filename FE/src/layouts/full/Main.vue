<script setup>
import { ref, shallowRef } from 'vue';
import { useUIStore } from '@/stores/ui';
import { storeToRefs } from 'pinia';

// Menu & Header Components
import sidebarItems from './vertical-sidebar/sidebarItem';
import NavGroup from './vertical-sidebar/NavGroup/index.vue';
import NavItem from './vertical-sidebar/NavItem/index.vue';
import NavCollapse from './vertical-sidebar/NavCollapse/index.vue';
import Logo from './logo/Logo.vue';
import NotificationDD from './vertical-header/NotificationDD.vue';
import ProfileDD from './vertical-header/ProfileDD.vue';
import { Menu2Icon } from 'vue-tabler-icons';

const uiStore = useUIStore();
const { sidebarCollapsed } = storeToRefs(uiStore);
const { toggleSidebar } = uiStore;

const sidebarMenu = shallowRef(sidebarItems);
</script>

<template>
    <!-- BIẾN SIDEBAR THÀNH CHẾ ĐỘ RAIL (MINI) KHI TẮT -->
    <v-navigation-drawer
        left
        :rail="sidebarCollapsed"
        permanent
        app
        class="leftSidebar admin-sidebar"
        elevation="0"
        :width="292"
        rail-width="84"
        style="
            overflow: hidden !important;
            margin: 0;
            height: 100vh !important;
            border-radius: 0 !important;
            border-right: 1px solid #e2e8f0 !important;
        "
    >
        <div class="sidebar-logo-wrap border-b" v-if="!sidebarCollapsed">
            <div class="sidebar-brand-card">
                <Logo />
                <div class="sidebar-brand-meta">
                    <div class="sidebar-brand-title">AeroStride</div>
                    <div class="sidebar-brand-subtitle">Bảng điều khiển quản trị</div>
                </div>
            </div>
        </div>
        <div class="sidebar-logo-wrap d-flex justify-center border-b" v-else>
            <v-avatar size="42" color="primary" class="sidebar-rail-avatar text-white font-weight-black">A</v-avatar>
        </div>

        <perfect-scrollbar class="scrollnavbar admin-sidebar-scroll" :style="{ height: 'calc(100vh - 148px)' }">
            <v-list class="py-4 px-3 admin-sidebar-list">
                <template v-for="(item, i) in sidebarMenu">
                    <NavGroup :item="item" v-if="item.header && !sidebarCollapsed" :key="item.title" />
                    <NavCollapse :item="item" v-else-if="item.children" :hide-title="sidebarCollapsed" />
                    <NavItem :item="item" v-else class="leftPadding" :hide-title="sidebarCollapsed" />
                </template>
            </v-list>
        </perfect-scrollbar>
    </v-navigation-drawer>

    <!-- HEADER: TỰ ĐỘNG CĂN THEO SIDEBAR VÀ NỘI DUNG -->
    <v-app-bar
        flat
        color="white"
        height="70"
        app
        class="px-sm-4 px-3 admin-topbar"
        style="box-shadow: none !important; border-bottom: 1px solid #e2e8f0 !important; background: white !important"
    >
        <div class="maxWidth mx-auto w-100 d-flex align-center px-1 py-0" style="height: 56px">
            <div class="d-flex align-center justify-space-between w-100">
                <div class="d-flex align-center">
                    <v-btn class="text-muted mr-1" @click="toggleSidebar" icon variant="flat">
                        <Menu2Icon size="22" stroke-width="2" />
                    </v-btn>
                </div>
                <div class="d-flex align-center">
                    <NotificationDD />
                    <ProfileDD />
                </div>
            </div>
        </div>
    </v-app-bar>
</template>

<style scoped>
/* TRIỆT TIÊU THANH CUỘN THỨ 2 CỦA VUETIFY */
:deep(.v-navigation-drawer__content) {
    overflow: hidden !important;
}

:deep(.v-navigation-drawer) {
    overflow: hidden !important;
}

.sidebar-logo-wrap {
    padding: 16px 14px;
    min-height: 132px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #ffffff;
}

.sidebar-brand-card {
    width: 100%;
    border: 1px solid #dce7fb;
    border-radius: 28px;
    background: linear-gradient(180deg, #f9fbff 0%, #f4f8ff 100%);
    padding: 18px 16px 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    box-shadow: 0 8px 22px rgba(15, 23, 42, 0.04);
}

.sidebar-brand-meta {
    text-align: center;
}

.sidebar-brand-title {
    font-size: 16px;
    font-weight: 800;
    color: #0f172a;
}

.sidebar-brand-subtitle {
    margin-top: 2px;
    font-size: 12px;
    font-weight: 600;
    color: #64748b;
}

.sidebar-rail-avatar {
    border-radius: 16px !important;
    box-shadow: 0 8px 18px rgba(30, 58, 138, 0.18);
}

:deep(.admin-sidebar) {
    background: #ffffff !important;
}

:deep(.admin-sidebar-list) {
    background: #ffffff !important;
}

:deep(.sidebar-link) {
    min-height: 58px !important;
    border-radius: 22px !important;
    margin-bottom: 6px !important;
    padding-inline: 12px !important;
    color: #334155 !important;
    transition: all 0.18s ease !important;
}

:deep(.sidebar-link:hover) {
    background: #f8fafc !important;
    color: #0f172a !important;
}

:deep(.sidebar-link .navbox) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
}

:deep(.sidebar-link .icon-box) {
    width: 42px;
    height: 42px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    background: #f8fafc;
    color: #64748b;
    transition: all 0.18s ease;
}

:deep(.sidebar-link .v-list-item-title) {
    font-size: 15px !important;
    font-weight: 700 !important;
}

:deep(.sidebar-link .v-list-item-subtitle) {
    margin-top: 2px !important;
    color: #94a3b8 !important;
    font-size: 12px !important;
    font-weight: 600 !important;
}

:deep(.sidebar-link.v-list-item--active),
:deep(.v-list-group--open > .v-list-group__items + .v-list-group__header .sidebar-link),
:deep(.v-list-group--open > .sidebar-link),
:deep(.v-list-group--open > .v-list-group__header .sidebar-link) {
    background: #edf4ff !important;
    color: #1f5fbf !important;
    box-shadow: 0 4px 12px rgba(31, 95, 191, 0.08);
}

:deep(.sidebar-link.v-list-item--active .icon-box),
:deep(.v-list-group--open > .v-list-group__header .sidebar-link .icon-box) {
    background: #ffffff !important;
    border-color: #d9e7ff !important;
    color: #1f5fbf !important;
}

:deep(.v-list-group__items .sidebar-link) {
    min-height: 46px !important;
    border-radius: 16px !important;
    padding-inline-start: 10px !important;
}

:deep(.v-list-group__items .sidebar-link .icon-box) {
    width: 32px;
    height: 32px;
    border-radius: 12px;
}

:deep(.v-list-group__items .sidebar-link .v-list-item-title) {
    font-size: 14px !important;
    font-weight: 600 !important;
}
</style>
