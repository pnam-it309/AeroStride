<script setup>
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuLanding } from '@/services/public/dichVuLanding';

const props = defineProps({ active: Boolean, warm: Boolean });
const router = useRouter();
const dynamicColumns = ref([]);

const fetchFeatures = async () => {
    try {
        dynamicColumns.value = await dichVuLanding.layDanhSachTinhNang();
    } catch (error) {
        console.error('Lỗi khi lấy danh sách tính năng:', error);
    }
};

watch(() => props.active || props.warm, (isWarm) => {
    if (isWarm && dynamicColumns.value.length === 0) {
        fetchFeatures();
    }
}, { immediate: true });

const onCardClick = (item) => {
    if (item.queryParam && item.queryValue) {
        router.push({ path: '/shoes', query: { [item.queryParam]: item.queryValue } });
    } else {
        router.push('/shoes');
    }
};
</script>

<template>
    <section class="snap-section features-section overflow-hidden" :class="{ 'is-active': props.active }">
        <template v-if="props.active || props.warm">
            <div class="light-decor">
                <div class="soft-glow glow-1"></div>
                <div class="soft-glow glow-2"></div>
                <div class="soft-glow glow-3"></div>
            </div>

            <div class="diagonal-bento-wall">
                <div
                    v-for="(column, columnIndex) in dynamicColumns"
                    :key="`col-${columnIndex}`"
                    class="bento-col"
                    :class="`col-${columnIndex + 1}`"
                >
                    <div
                        v-for="(item, itemIndex) in column"
                        :key="`c${columnIndex}-${itemIndex}`"
                        class="bento-card glass-card"
                        :class="item.size"
                        @click="onCardClick(item)"
                    >
                        <div class="card-inner">
                            <div class="icon-wrap" :style="{ background: `${item.color}15` }">
                                <v-icon :color="item.color" size="24">{{ item.icon }}</v-icon>
                            </div>
                            <div class="text-wrap">
                                <h4>{{ item.title }}</h4>
                                <p>{{ item.desc }}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </template>
    </section>
</template>

<style scoped lang="scss">
@import '@/scss/pages/landing/_features-section.scss';
</style>
