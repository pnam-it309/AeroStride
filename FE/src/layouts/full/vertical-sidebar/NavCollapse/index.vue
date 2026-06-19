<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { ChevronDownIcon } from 'vue-tabler-icons';
import NavItem from '../NavItem/index.vue';
import Icon from '../Icon.vue';
import { useUIStore } from '@/stores/ui';

const props = defineProps({ item: Object, level: { type: Number, default: 1 }, hideTitle: Boolean });
const route = useRoute();
const uiStore = useUIStore();

const isActive = computed(() => {
    if (!props.item?.children) return false;
    const path = route.path;
    return props.item.children.some(child => {
        if (!child.to) return false;
        if (path === child.to) return true;
        if (child.to === '/' || child.to === '/admin/thong-ke') return false;
        if (child.to === '/admin/san-pham' && path.startsWith('/admin/san-pham/bien-the')) return false;
        if (path.startsWith(child.to + '/')) return true;
        return false;
    });
});

const handleActivatorClick = () => {
    if (props.hideTitle) {
        uiStore.setSidebarCollapsed(false);
    }
};
</script>

<template>
    <!-- NORMAL MODE: Sidebar Expanded -->
    <v-list-group v-if="!hideTitle" :value="item.title" class="mb-1 nav-collapse-group">
        <template v-slot:activator="{ props: activatorProps, isOpen }">
            <v-list-item v-bind="activatorProps" rounded class="leftPadding sidebar-link transition-item"
                :class="{ 'v-list-item--active text-primary bg-lightprimary': isActive && !isOpen }"
                :ripple="false">
                <template v-slot:prepend>
                    <div class="navbox">
                        <span class="icon-box">
                            <Icon :item="item.icon" :level="level" class="position-relative z-index-2" />
                        </span>
                    </div>
                </template>
                <v-list-item-title class="sidebar-item-title font-weight-medium">{{ item.title }}</v-list-item-title>

                <template v-slot:append>
                    <ChevronDownIcon size="16" class="rotate-icon transition-300" :class="{ rotated: isOpen }" />
                </template>
            </v-list-item>
        </template>

        <!---Nested Items Wrapper with curtain effect-->
        <div class="nested-items-container">
            <NavItem v-for="(subitem, i) in item.children" :key="i" :item="subitem" :level="level + 1"
                :hide-title="false" class="leftPadding" />
        </div>
    </v-list-group>

    <!-- RAIL MODE: Sidebar Collapsed (Flyout Menu) -->
    <v-menu v-else open-on-hover location="end" offset="15" transition="scale-transition">
        <template v-slot:activator="{ props: menuProps }">
            <v-list-item v-bind="menuProps" rounded class="leftPadding sidebar-link transition-item"
                :class="{ 'v-list-item--active text-primary bg-lightprimary': isActive }"
                :ripple="false" @click="handleActivatorClick">
                <template v-slot:prepend>
                    <div class="navbox">
                        <span class="icon-box">
                            <Icon :item="item.icon" :level="level" class="position-relative z-index-2" />
                        </span>
                    </div>
                </template>
            </v-list-item>
        </template>

        <v-list class="flyout-menu bg-white rounded-lg elevation-3" width="220">
            <v-list-item class="flyout-header border-b mb-2">
                <v-list-item-title class="font-weight-bold text-subtitle-2 text-primary">{{ item.title }}</v-list-item-title>
            </v-list-item>
            <!-- Render NavItems inside flyout, but force level=1 so they don't have deep indents -->
            <NavItem v-for="(subitem, i) in item.children" :key="i" :item="subitem" :level="1"
                :hide-title="false" class="px-2" />
        </v-list>
    </v-menu>
</template>

<style scoped>
.sidebar-item-title {
    font-size: 15px !important;
}

.rotate-icon {
    opacity: 0.7;
}

.rotate-icon.rotated {
    transform: rotate(180deg);
    color: rgb(var(--v-theme-primary));
    opacity: 1;
}

.transition-300 {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-collapse-group :deep(.v-list-group__items) {
    --v-list-group-items-padding: 0;
    border-left: 2px solid rgba(var(--v-theme-primary), 0.1);
    margin-left: 20px;
    margin-top: 2px;
    margin-bottom: 8px;
    border-radius: 0 0 12px 12px;
}
</style>
