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
const { sidebarCollapsed, breadcrumbs } = storeToRefs(uiStore);
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
        class="leftSidebar bg-containerBg"
        elevation="0"
        :width="300"
        rail-width="68"
        style="
            overflow: hidden !important;
            margin: 0;
            height: 100vh !important;
            border-radius: 0 !important;
            border-right: 1px solid rgba(0, 0, 0, 0.08) !important;
        "
    >
        <div class="sidebar-logo-wrap d-flex justify-center border-b" v-if="!sidebarCollapsed">
            <Logo />
        </div>
        <div class="sidebar-logo-wrap d-flex justify-center border-b" v-else>
            <v-avatar size="32" color="primary" class="rounded-0 text-white font-weight-black">A</v-avatar>
        </div>

        <perfect-scrollbar class="scrollnavbar bg-containerBg" :style="{ height: 'calc(100vh - 136px)' }">
            <v-list class="py-4 px-2 bg-containerBg">
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
        height="64"
        app
        class="px-sm-4 px-3"
        style="box-shadow: none !important; border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important; background: white !important"
    >
        <div class="maxWidth mx-auto w-100 d-flex align-center px-1 py-0" style="height: 56px">
            <div class="d-flex align-center justify-space-between w-100">
                <div class="d-flex align-center">
                    <v-btn class="mr-3 header-toggle-btn" @click="toggleSidebar" icon variant="text" color="slate-600">
                        <Menu2Icon size="22" stroke-width="2" />
                    </v-btn>
                    
                    <v-breadcrumbs v-if="breadcrumbs.length" :items="breadcrumbs" class="pa-0 breadcrumbs-header">
                        <template v-slot:divider>
                            <v-icon size="small" color="grey-lighten-1">mdi-chevron-right</v-icon>
                        </template>
                    </v-breadcrumbs>
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

.header-toggle-btn {
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.header-toggle-btn:hover {
    color: #475569 !important;
    background: transparent !important;
    transform: rotate(90deg) scale(1.1);
}

.header-toggle-btn :deep(.v-btn__overlay),
.header-toggle-btn :deep(.v-btn__underlay),
.header-toggle-btn :deep(.v-ripple__container) {
    display: none !important;
}

.header-toggle-btn:focus, 
.header-toggle-btn:active, 
.header-toggle-btn:focus-visible {
    background: transparent !important;
    background-color: transparent !important;
    box-shadow: none !important;
    outline: none !important;
}

.sidebar-logo-wrap {
    padding: 12px 8px;
    min-height: 128px;
    align-items: center;
}

/* SIDEBAR STATIC DESIGN - MATCHING IMAGE EXACTLY */
/* Ép màu nền và chữ cho TẤT CẢ các mục đang active (kể cả menu cha và menu con) */
:deep(.v-list-item.sidebar-link.v-list-item--active),
:deep(.v-list-group--active > .v-list-item),
:deep(.v-list-item--active) {
    background-color: #eef2ff !important; 
    color: #1e257c !important;
    transition: none !important;
}

:deep(.v-list-item.sidebar-link) {
    transition: none !important;
    margin-bottom: 4px !important;
    margin-left: 8px !important; 
    margin-right: 8px !important;
    border-radius: 12px !important;
    min-height: 44px !important;
}

/* Ép tất cả thành phần con không được có chuyển động */
:deep(.v-list-item.sidebar-link *),
:deep(.v-list-item.sidebar-link::before),
:deep(.v-list-item.sidebar-link::after) {
    transition: none !important;
    animation: none !important;
}

/* Chặn đứng Ripple và các lớp phủ màu rực rỡ */
:deep(.v-list-item .v-ripple__container),
:deep(.v-list-item__overlay),
:deep(.v-list-item__underlay) {
    display: none !important;
}

/* HOVER: Chỉ đổi màu chữ, KHÔNG hiện nền để tránh cảm giác xanh toàn bộ */
:deep(.v-list-item.sidebar-link:hover) {
    background-color: transparent !important; 
    color: #1e257c !important;
}

/* Đảm bảo chữ và icon LUÔN là màu xanh thương hiệu khi active hoặc hover */
:deep(.v-list-item--active .v-list-item-title),
:deep(.v-list-item--active .v-icon),
:deep(.v-list-item--active .icon-box),
:deep(.v-list-item:hover .v-list-item-title),
:deep(.v-list-item:hover .v-icon) {
    color: #1e257c !important;
    opacity: 1 !important;
}

/* Animation cho text menu khi rail đổi trạng thái */
:deep(.v-list-item-title) {
    transition: opacity 0.3s ease;
}
/* Breadcrumbs in Header */
:deep(.breadcrumbs-header) {
    font-size: 13px;
    font-weight: 600;
}

:deep(.breadcrumbs-header .v-breadcrumbs-item) {
    padding: 0;
    color: #475569 !important; /* Slate 600 */
    opacity: 1 !important;
}

:deep(.breadcrumbs-header .v-breadcrumbs-item--disabled) {
    opacity: 1 !important;
    color: #0f172a !important; /* Slate 900 for current page */
}

:deep(.breadcrumbs-header .v-breadcrumbs-divider) {
    padding: 0 4px;
    color: #94a3b8;
}
</style>
