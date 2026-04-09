<script setup>
import { ref, shallowRef } from 'vue';
import sidebarItems from './vertical-sidebar/sidebarItem';
import NavGroup from './vertical-sidebar/NavGroup/index.vue';
import NavItem from './vertical-sidebar/NavItem/index.vue';
import NavCollapse from './vertical-sidebar/NavCollapse/index.vue';
import Logo from './logo/Logo.vue';
import { Menu2Icon } from 'vue-tabler-icons';
import NotificationDD from './vertical-header/NotificationDD.vue';
import ProfileDD from './vertical-header/ProfileDD.vue';

const sidebarMenu = shallowRef(sidebarItems);
const sDrawer = ref(true); // Trạng thái mở rộng (true) hoặc mini (false)
</script>

<template>
    <!-- BIẾN SIDEBAR THÀNH CHẾ ĐỘ RAIL (MINI) KHI TẮT -->
    <v-navigation-drawer 
        left 
        :rail="!sDrawer"
        permanent
        app 
        class="leftSidebar bg-containerBg border-r" 
        elevation="0"
        :width="270"
        rail-width="75"
        style="overflow: hidden !important;"
    >
        <div class="pa-4 d-flex justify-center border-b" v-if="sDrawer">
            <Logo />
        </div>
        <div class="pa-4 d-flex justify-center border-b" v-else>
            <v-avatar size="32" color="primary" class="rounded-0 text-white font-weight-black">A</v-avatar>
        </div>

        <perfect-scrollbar class="scrollnavbar bg-containerBg" :style="{ height: 'calc(100vh - 120px)' }">
            <v-list class="py-4 px-2 bg-containerBg">
                <template v-for="(item, i) in sidebarMenu">
                    <NavGroup :item="item" v-if="item.header && sDrawer" :key="item.title" />
                    <NavCollapse :item="item" v-else-if="item.children" :hide-title="!sDrawer" />
                    <NavItem :item="item" v-else class="leftPadding" :hide-title="!sDrawer" />
                </template>
            </v-list>
        </perfect-scrollbar>
    </v-navigation-drawer>

    <!-- HEADER: TỰ ĐỘNG CĂN THEO SIDEBAR VÀ NỘI DUNG -->
    <v-app-bar flat color="transparent" height="70" app class="px-4" style="box-shadow: none !important; border: none !important; background: transparent !important;">
        <div class="maxWidth mx-auto w-100 bg-white rounded-xl shadow-sm d-flex align-center px-4 py-2" style="height: 60px; border: 1px solid rgba(0,0,0,0.05);">
            <div class="d-flex align-center justify-space-between w-100">
                <div class="d-flex align-center">
                    <v-btn class="text-muted mr-1" @click="sDrawer = !sDrawer" icon variant="flat">
                        <Menu2Icon size="22" stroke-width="2" />
                    </v-btn>
                    <NotificationDD />
                </div>
                <div>
                    <ProfileDD />
                </div>
            </div>
        </div>
    </v-app-bar>
</template>

<style scoped>
.maxWidth {
    max-width: 1600px;
}
.shadow-sm {
    box-shadow: 0 2px 15px -3px rgba(0,0,0,0.07),0 4px 6px -2px rgba(0,0,0,0.05) !important;
}

/* TRIỆT TIÊU THANH CUỘN THỨ 2 CỦA VUETIFY */
:deep(.v-navigation-drawer__content) {
    overflow: hidden !important;
}

:deep(.v-navigation-drawer) {
    overflow: hidden !important;
}
</style>
