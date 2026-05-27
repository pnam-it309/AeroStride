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
                        <v-expansion-panel
                            title="Làm sao để tôi chọn đúng size giày?"
                            text="Bạn có thể tham khảo bảng hướng dẫn đo chân (cm) tại trang chi tiết sản phẩm. Nếu vẫn phân vân, đừng ngần ngại nhắn tin cho Trợ lý AI hoặc nhân viên để được tư vấn size chuẩn nhất nhé."
                            class="rounded-xl mb-4 elevation-0"
                        ></v-expansion-panel>
                        <v-expansion-panel
                            title="Chính sách đổi trả khi không vừa size?"
                            text="AeroStride hỗ trợ đổi size miễn phí trong vòng 7 ngày kể từ khi nhận hàng. Sản phẩm đổi trả cần giữ nguyên tem mác, hộp và chưa qua sử dụng."
                            class="rounded-xl mb-4 elevation-0"
                        ></v-expansion-panel>
                        <v-expansion-panel
                            title="Giày AeroStride được bảo hành bao lâu?"
                            text="Mọi đôi giày mua tại AeroStride đều được bảo hành keo, đế và chỉ khâu hoàn toàn miễn phí trong vòng 6 tháng."
                            class="rounded-xl elevation-0"
                        ></v-expansion-panel>
                    </v-expansion-panels>
                </v-col>
            </v-row>
        </v-container>
    </section>
</template>

<style scoped lang="scss">
.snap-section {
    height: 100vh;
    width: 100%;
    scroll-snap-align: start;
    scroll-snap-stop: always;
    display: flex;
    align-items: center;
}
.animate-in-scale {
    animation: scaleIn 0.8s ease-out both;
}
@keyframes scaleIn {
    from {
        opacity: 0;
        transform: scale(0.9);
    }
    to {
        opacity: 1;
        transform: scale(1);
    }
}
</style>
