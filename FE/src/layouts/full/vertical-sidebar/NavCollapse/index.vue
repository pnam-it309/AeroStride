<script setup>
import { ChevronDownIcon } from 'vue-tabler-icons';
import NavItem from '../NavItem/index.vue';
import Icon from '../Icon.vue';

const props = defineProps({ item: Object, level: { type: Number, default: 1 }, hideTitle: Boolean });
</script>

<template>
    <v-list-group :value="item.title" class="mb-1 nav-collapse-group" :disabled="hideTitle">
        <template v-slot:activator="{ props: activatorProps, isOpen }">
            <v-list-item v-bind="activatorProps" rounded class="leftPadding sidebar-link transition-item" :ripple="false">
                <template v-slot:prepend>
                    <div class="navbox">
                        <span class="icon-box">
                            <Icon :item="item.icon" :level="level" class="position-relative z-index-2" />
                        </span>
                    </div>
                </template>
                <v-list-item-title v-if="!hideTitle" class="sidebar-item-title font-weight-medium">{{ item.title }}</v-list-item-title>
                
                <template v-slot:append v-if="!hideTitle">
                    <ChevronDownIcon 
                        size="16" 
                        class="rotate-icon transition-300" 
                        :class="{ 'rotated': isOpen }"
                    />
                </template>
            </v-list-item>
        </template>

        <!---Nested Items Wrapper with curtain effect-->
        <div class="nested-items-container">
            <NavItem v-for="(subitem, i) in item.children" :key="i" :item="subitem" :level="level + 1" :hide-title="hideTitle" class="leftPadding" />
        </div>
    </v-list-group>
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
}

.nested-items-container {
    border-left: 2px solid rgba(var(--v-theme-primary), 0.1);
    margin-left: 24px;
    margin-top: 2px;
    margin-bottom: 8px;
    border-radius: 0 0 12px 12px;
}
</style>
