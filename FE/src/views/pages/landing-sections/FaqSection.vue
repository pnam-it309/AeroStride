<script setup>
import { defineProps } from 'vue';
const props = defineProps({ active: Boolean });
</script>

<template>
    <section class="snap-section faq-section bg-blue-accent-1 d-flex align-center">
        <v-container>
            <v-row justify="center" :class="{ 'animate-in-scale': props.active }">
                <v-col cols="12" md="8">
                    <h2 class="text-h3 font-weight-black text-center mb-12">Hỏi đáp nhanh</h2>
                    <v-expansion-panels variant="popout">
                        <v-expansion-panel title="Hỗ trợ đổi trả trong bao lâu?" text="Bạn có thể đổi trả miễn phí trong vòng 30 ngày nếu không hài lòng." class="rounded-xl mb-4 elevation-0"></v-expansion-panel>
                        <v-expansion-panel title="Có ship COD không?" text="Chúng tôi hỗ trợ ship COD toàn quốc với thời gian từ 2-3 ngày." class="rounded-xl mb-4 elevation-0"></v-expansion-panel>
                        <v-expansion-panel title="Bảo hành như thế nào?" text="Sản phẩm được bảo hành 12 tháng cho toàn bộ lỗi từ nhà sản xuất." class="rounded-xl elevation-0"></v-expansion-panel>
                    </v-expansion-panels>
                </v-col>
            </v-row>
        </v-container>
    </section>
</template>

<style scoped lang="scss">
.snap-section { height: 100vh; width: 100%; scroll-snap-align: start; scroll-snap-stop: always; display: flex; align-items: center; }
.animate-in-scale { animation: scaleIn 0.8s ease-out both; }
@keyframes scaleIn { from { opacity: 0; transform: scale(0.9); } to { opacity: 1; transform: scale(1); } }
</style>
