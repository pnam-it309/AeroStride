<script setup>
import Icon from '../Icon.vue';
const props = defineProps({ item: Object, level: { type: Number, default: 1 }, hideTitle: Boolean });
</script>

<template>
    <!---Single Item-->
    <div class="mb-2" :style="{ paddingLeft: level > 1 ? (level - 1) * 20 + 'px' : '0px' }">
        <v-list-item  :to="item.type === 'external' ? '' : item.to" :href="item.type === 'external' ? item.to : ''" rounded
            class="sidebar-link" :ripple="false" :disabled="item.disabled"
            :target="item.type === 'external' ? '_blank' : ''">
            <!---If icon-->
            <template v-slot:prepend>
                <div class="navbox">
                    <span class="icon-box">
                        <Icon :item="item.icon" :level="level" class="position-relative z-index-2" />
                    </span>
                </div>
            </template>
            <v-list-item-title v-if="!hideTitle" class="sidebar-item-title font-weight-medium">{{ item.title}}</v-list-item-title>
            <!---If Caption-->
            <v-list-item-subtitle v-if="item.subCaption && !hideTitle" class="text-caption mt-n1 hide-menu">
                {{ item.subCaption }}
            </v-list-item-subtitle>
            <!---If any chip or label-->
            <template v-slot:append v-if="item.chip && !hideTitle">
                <v-chip :color="item.chipColor" class="sidebarchip hide-menu"
                    :size="item.chipIcon ? 'x-small' : 'x-small'" :variant="item.chipVariant" :prepend-icon="item.chipIcon">
                    {{ item.chip }}
                </v-chip>
            </template>
        </v-list-item>
    </div>
</template>

<style scoped>
.sidebar-item-title {
    font-size: 15px !important;
}
</style>
