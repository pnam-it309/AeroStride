<script setup>
import NavItem from '../NavItem/index.vue';
import Icon from '../Icon.vue';

const props = defineProps({ item: Object, level: { type: Number, default: 1 }, hideTitle: Boolean });
</script>

<template>
    <v-list-group :value="item.title" class="mb-1" :disabled="hideTitle">
        <template v-slot:activator="{ props }">
            <v-list-item v-bind="props" rounded class="leftPadding sidebar-link" :ripple="false">
                <template v-slot:prepend>
                    <div class="navbox">
                        <span class="icon-box">
                            <Icon :item="item.icon" :level="level" class="position-relative z-index-2" />
                        </span>
                    </div>
                </template>
                <v-list-item-title v-if="!hideTitle" class="sidebar-item-title font-weight-medium">{{ item.title }}</v-list-item-title>
            </v-list-item>
        </template>

        <!---Nested Items-->
        <NavItem v-for="(subitem, i) in item.children" :key="i" :item="subitem" :level="level + 1" :hide-title="hideTitle" class="leftPadding" />
    </v-list-group>
</template>

<style scoped>
.sidebar-item-title {
    font-size: 15px !important;
}
</style>
