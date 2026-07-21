<script setup>
import { ref, watch, computed } from 'vue';
import { useNotificationStore } from '@/stores/notificationStore';
import api from '@/services/apiService';

const props = defineProps({
    show: { type: Boolean, default: false },
    order: { type: Object, default: () => null }
});

const emit = defineEmits(['update:show', 'review-success']);
const notification = useNotificationStore();

const showModal = computed({
    get: () => props.show,
    set: (val) => emit('update:show', val)
});

const selectedItem = ref(null);
const rating = ref(5);
const comment = ref('');
const isSubmitting = ref(false);

watch(() => props.show, (newVal) => {
    if (newVal) {
        rating.value = 5;
        comment.value = '';
        selectedItem.value = props.order?.items?.[0] || null;
    }
});

const submitReview = async () => {
    if (!selectedItem.value) {
        notification.showError('Vui lòng chọn sản phẩm để đánh giá');
        return;
    }
    if (!comment.value.trim()) {
        notification.showError('Vui lòng nhập nội dung đánh giá');
        return;
    }

    isSubmitting.value = true;
    try {
        const payload = {
            idHoaDon: props.order.id,
            idChiTietSanPham: selectedItem.value.idChiTietSanPham,
            rating: rating.value,
            comment: comment.value
        };

        const response = await api.post('/api/v1/reviews/submit', payload);
        if (response.data?.success) {
            notification.showSuccess('Cảm ơn bạn đã đánh giá sản phẩm!');
            emit('review-success');
            showModal.value = false;
        } else {
            notification.showError(response.data?.message || 'Có lỗi xảy ra');
        }
    } catch (error) {
        console.error('Lỗi khi gửi đánh giá:', error);
        notification.showError(error.response?.data?.message || 'Có lỗi xảy ra khi gửi đánh giá');
    } finally {
        isSubmitting.value = false;
    }
};
</script>

<template>
    <v-dialog v-model="showModal" max-width="600" persistent>
        <v-card class="rounded-xl overflow-hidden">
            <v-card-title class="d-flex align-center py-3 bg-indigo-darken-4 text-white">
                <v-icon icon="mdi-star-circle-outline" class="mr-2"></v-icon>
                Đánh giá sản phẩm
                <v-spacer></v-spacer>
                <v-btn icon="mdi-close" variant="text" color="white" @click="showModal = false" density="compact" :disabled="isSubmitting"></v-btn>
            </v-card-title>
            
            <v-card-text class="pa-4">
                <div v-if="order" class="mb-4">
                    <p class="text-subtitle-2 mb-2">Chọn sản phẩm cần đánh giá:</p>
                    <v-item-group v-model="selectedItem" mandatory>
                        <v-row dense>
                            <v-col cols="12" v-for="item in order.items" :key="item.idChiTietSanPham">
                                <v-item v-slot="{ isSelected, toggle }" :value="item">
                                    <v-card 
                                        :color="isSelected ? 'indigo-lighten-5' : ''"
                                        :style="isSelected ? 'border: 1px solid #1e257c;' : 'border: 1px solid #e0e0e0;'"
                                        class="d-flex align-center pa-2 rounded-lg cursor-pointer"
                                        flat
                                        @click="toggle"
                                    >
                                        <v-img :src="item.hinhAnh" width="48" height="48" cover class="rounded mr-3" style="flex: none;"></v-img>
                                        <div class="flex-grow-1">
                                            <div class="text-body-2 font-weight-bold text-truncate" style="max-width: 300px;">{{ item.tenSanPham }}</div>
                                            <div class="text-caption text-grey">{{ item.tenMauSac }} / {{ item.tenKichThuoc }}</div>
                                        </div>
                                        <v-icon v-if="isSelected" color="#1e257c">mdi-check-circle</v-icon>
                                    </v-card>
                                </v-item>
                            </v-col>
                        </v-row>
                    </v-item-group>
                </div>

                <div class="text-center mb-4">
                    <p class="text-subtitle-2 mb-1">Chất lượng sản phẩm</p>
                    <v-rating
                        v-model="rating"
                        color="amber"
                        active-color="amber"
                        hover
                        size="x-large"
                    ></v-rating>
                </div>

                <v-textarea
                    v-model="comment"
                    label="Nhận xét của bạn"
                    placeholder="Hãy chia sẻ cảm nhận của bạn về sản phẩm này nhé (chất liệu, màu sắc, form dáng...)"
                    variant="outlined"
                    rows="4"
                    auto-grow
                    hide-details="auto"
                ></v-textarea>
                
                <!-- TODO: Add image upload for review if needed -->
            </v-card-text>

            <v-card-actions class="pa-4 pt-0">
                <v-spacer></v-spacer>
                <v-btn variant="text" class="text-none font-weight-bold" @click="showModal = false" :disabled="isSubmitting">Hủy</v-btn>
                <v-btn 
                    color="indigo-darken-4" 
                    variant="flat" 
                    class="text-none font-weight-bold px-6 rounded-pill" 
                    :loading="isSubmitting"
                    @click="submitReview"
                >
                    Gửi đánh giá
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>
