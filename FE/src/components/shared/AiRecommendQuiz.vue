<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import { formatCurrency } from '@/utils/formatters';
import { useToastStore } from '@/stores/toastStore';

const router = useRouter();
const toastStore = useToastStore();

const showDialog = ref(false);
const loading = ref(false);
const currentQuestion = ref(null);
const recommendedProducts = ref([]);
const answers = ref({});
const history = ref([]); // To allow backing out of questions

const openQuiz = async () => {
    showDialog.value = true;
    resetQuiz();
    await fetchNextQuestion();
};

const resetQuiz = () => {
    answers.value = {};
    history.value = [];
    currentQuestion.value = null;
    recommendedProducts.value = [];
};

const fetchNextQuestion = async () => {
    loading.value = true;
    try {
        const res = await dichVuSanPhamPublic.layGoiYQuiz(answers.value);
        if (res.nextQuestion) {
            currentQuestion.value = res.nextQuestion;
            recommendedProducts.value = [];
        } else {
            currentQuestion.value = null;
            recommendedProducts.value = res.recommendedProducts || [];
        }
    } catch (e) {
        console.error(e);
        toastStore.showToast('Không thể kết nối dịch vụ gợi ý. Vui lòng thử lại sau.', 'error');
    } finally {
        loading.value = false;
    }
};

const selectOption = async (optionValue) => {
    if (!currentQuestion.value) return;

    // Save current state to history
    history.value.push({
        answers: { ...answers.value },
        question: { ...currentQuestion.value }
    });

    answers.value[currentQuestion.value.key] = optionValue;
    await fetchNextQuestion();
};

const goBack = () => {
    if (history.value.length === 0) return;
    const previous = history.value.pop();
    answers.value = previous.answers;
    currentQuestion.value = previous.question;
    recommendedProducts.value = [];
};

const viewProductDetail = (productId) => {
    showDialog.value = false;
    router.push(`/product/${productId}`);
};

const formatProductPrice = (prod) => {
    const min = prod?.giaBanThapNhat ?? prod?.giaBan ?? prod?.giaBanMin ?? prod?.gia ?? 0;
    const max = prod?.giaBanCaoNhat;
    if (min > 0 && max && max > min) {
        return `${formatCurrency(min)} - ${formatCurrency(max)}`;
    }
    return formatCurrency(min);
};

const totalSteps = 5;
const isFinished = computed(() => !currentQuestion.value && !loading.value && (history.value.length > 0 || recommendedProducts.value.length > 0));
const currentStep = computed(() => {
    if (isFinished.value) return totalSteps;
    return Math.min(history.value.length + 1, totalSteps);
});

const progressPercentage = computed(() => {
    if (isFinished.value) return 100;
    return Math.min(100, Math.round((currentStep.value / totalSteps) * 100));
});
</script>

<template>
    <div>
        <!-- Floating Action Button for Quiz Recommendation -->
        <v-btn color="primary" class="recommend-floating-btn font-weight-bold" elevation="6" @click="openQuiz">
            <v-icon start size="20">mdi-shoe-sneaker</v-icon>
            Tìm Giày Phù Hợp
        </v-btn>

        <!-- Dynamic Recommendation Dialog -->
        <v-dialog v-model="showDialog" max-width="880" class="quiz-recommend-dialog" scrollable>
            <v-card class="quiz-card overflow-hidden">
                <v-card-title class="quiz-header d-flex justify-space-between align-center px-6 py-5">
                    <div class="d-flex align-center">
                        <div class="quiz-header-icon-wrap mr-3">
                            <v-icon color="#ffffff" size="24" class="animate-bounce">mdi-wizard-hat</v-icon>
                        </div>
                        <div>
                            <div class="font-weight-black text-h6 text-white leading-tight">Chọn Giày Thông Minh Cùng AeroStride AI</div>
                            <div class="text-caption text-slate-200 mt-1">Trắc nghiệm tư vấn phong cách & công năng</div>
                        </div>
                    </div>
                    <v-btn icon variant="text" size="small" class="quiz-close-btn rounded-circle" @click="showDialog = false">
                        <v-icon color="#ffffff" size="20">mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <!-- Progress Tracker -->
                <div class="progress-bar-container px-6 pt-2">
                    <v-progress-linear
                        :model-value="progressPercentage"
                        color="primary"
                        height="6"
                        rounded
                        striped
                        class="quiz-progress"
                    ></v-progress-linear>
                    <div class="d-flex justify-space-between text-caption text-medium-emphasis mt-1">
                        <span>Bước {{ currentStep }} / {{ totalSteps }}</span>
                        <span>{{ Math.round(progressPercentage) }}% Hoàn thành</span>
                    </div>
                </div>

                <v-card-text class="quiz-content px-6 px-md-8 py-6 overflow-y-auto">
                    <!-- Loading state -->
                    <div v-if="loading" class="d-flex flex-column align-center justify-center py-10">
                        <v-progress-circular indeterminate color="primary" size="56" width="4"></v-progress-circular>
                        <div class="mt-4 text-h6 font-weight-bold text-slate-700 animate-pulse">AeroStride AI đang xử lý...</div>
                    </div>

                    <div v-else>
                        <!-- Question Step -->
                        <div v-if="currentQuestion" class="animate-fade-in">
                            <h3 class="text-h5 font-weight-black mb-6 text-slate-900 text-center">
                                {{ currentQuestion.questionText }}
                            </h3>
                            <v-row class="ga-3 flex-column align-center px-2">
                                <v-card
                                    v-for="opt in currentQuestion.options"
                                    :key="opt.value"
                                    class="option-card w-100 pa-5 d-flex align-center justify-space-between border cursor-pointer transition elevation-1"
                                    variant="outlined"
                                    @click="selectOption(opt.value)"
                                >
                                    <div class="font-weight-bold text-h6 text-slate-800">{{ opt.label }}</div>
                                    <v-icon color="primary" size="24" class="option-arrow">mdi-chevron-right</v-icon>
                                </v-card>
                            </v-row>
                        </div>

                        <!-- Quiz Recommendations Results -->
                        <div v-else-if="recommendedProducts.length > 0" class="animate-fade-in">
                            <div class="text-center mb-6">
                                <v-icon color="success" size="48" class="mb-2">mdi-check-circle-outline</v-icon>
                                <h3 class="text-h5 font-weight-bold text-slate-800">Sản Phẩm Phù Hợp Với Bạn!</h3>
                                <p class="text-subtitle-1 text-medium-emphasis">
                                    Dưới đây là các đôi giày được gợi ý dựa trên lựa chọn của bạn:
                                </p>
                            </div>

                            <v-row>
                                <v-col v-for="prod in recommendedProducts" :key="prod.id" cols="12" sm="6" class="pa-2">
                                    <v-card class="result-product-card h-100 d-flex flex-column" @click="viewProductDetail(prod.id)">
                                        <div class="product-img-wrapper position-relative">
                                            <v-img
                                                :src="prod.hinhAnh || '/assets/images/products/s4.jpg'"
                                                height="180"
                                                cover
                                                class="product-img"
                                            >
                                                <template #placeholder>
                                                    <div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
                                                        <v-progress-circular indeterminate color="grey"></v-progress-circular>
                                                    </div>
                                                </template>
                                            </v-img>
                                            <v-chip
                                                color="primary"
                                                size="x-small"
                                                class="brand-badge position-absolute font-weight-bold"
                                                style="top: 8px; left: 8px"
                                            >
                                                {{ prod.tenThuongHieu }}
                                            </v-chip>
                                        </div>
                                        <div class="pa-3 d-flex flex-column justify-space-between flex-grow-1">
                                            <div>
                                                <h4 class="font-weight-bold text-body-1 text-slate-800 text-truncate">
                                                    {{ prod.tenSanPham }}
                                                </h4>
                                                <div class="text-caption text-medium-emphasis mt-1">
                                                    {{ prod.tenMucDichChay }} | {{ prod.tenChatLieu }}
                                                </div>
                                            </div>
                                            <div class="d-flex align-center justify-space-between mt-3">
                                                <div class="price-text text-h6 font-weight-bold text-primary">
                                                    {{ formatProductPrice(prod) }}
                                                </div>
                                                <v-btn color="primary" variant="text" size="small" class="text-none font-weight-bold">
                                                    Xem ngay
                                                </v-btn>
                                            </div>
                                        </div>
                                    </v-card>
                                </v-col>
                            </v-row>
                        </div>

                        <!-- No matching products -->
                        <div v-else class="text-center py-8 animate-fade-in">
                            <v-icon color="warning" size="48" class="mb-2">mdi-alert-circle-outline</v-icon>
                            <h3 class="text-h6 font-weight-bold text-slate-800">Không tìm thấy sản phẩm phù hợp</h3>
                            <p class="text-body-1 text-medium-emphasis mt-2 px-6">
                                Rất tiếc, hiện tại không có đôi giày nào trong CSDL đáp ứng đầy đủ tất cả các lựa chọn của bạn. Bạn hãy thử
                                làm lại quiz với lựa chọn rộng hơn nhé!
                            </p>
                            <v-btn
                                color="primary"
                                class="mt-6 font-weight-bold text-none px-6"
                                @click="
                                    resetQuiz();
                                    fetchNextQuestion();
                                "
                            >
                                Thử Lại Quiz
                            </v-btn>
                        </div>
                    </div>
                </v-card-text>

                <!-- Footer Navigation -->
                <v-card-actions class="quiz-footer px-6 py-4 border-t bg-grey-lighten-4 d-flex justify-space-between">
                    <v-btn
                        variant="outlined"
                        color="secondary"
                        class="text-none font-weight-bold px-4"
                        :disabled="history.length === 0 || loading"
                        @click="goBack"
                    >
                        <v-icon start>mdi-arrow-left</v-icon>
                        Quay lại
                    </v-btn>

                    <v-btn
                        variant="text"
                        color="error"
                        class="text-none font-weight-bold px-4"
                        :disabled="loading"
                        @click="
                            resetQuiz();
                            fetchNextQuestion();
                        "
                    >
                        Làm mới quiz
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </div>
</template>

<style scoped>
.recommend-floating-btn {
    position: fixed;
    bottom: 96px;
    right: 24px;
    z-index: 99;
    border-radius: 30px;
    padding: 0 20px;
    height: 48px;
    background: linear-gradient(135deg, #1890ff 0%, #001529 100%) !important;
    color: #fff !important;
    text-transform: none;
    box-shadow: 0 4px 15px rgba(24, 144, 255, 0.4) !important;
    transition: all 0.3s ease;
}

.recommend-floating-btn:hover {
    transform: translateY(-3px) scale(1.03);
    box-shadow: 0 6px 20px rgba(24, 144, 255, 0.6) !important;
}

.quiz-card {
    border-radius: 20px !important;
    box-shadow: 0 16px 48px rgba(30, 37, 124, 0.18) !important;
}

.quiz-header {
    background: linear-gradient(135deg, #1e257c 0%, #23318c 50%, #1d4ed8 100%) !important;
    color: #ffffff !important;
    box-shadow: 0 4px 16px rgba(30, 37, 124, 0.2);
}

.quiz-header-icon-wrap {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid rgba(255, 255, 255, 0.35);
}

.quiz-close-btn {
    background: rgba(255, 255, 255, 0.18) !important;
    color: #ffffff !important;
    transition: all 0.2s ease;
}

.quiz-close-btn:hover {
    background: rgba(255, 255, 255, 0.35) !important;
    transform: rotate(90deg);
}

.progress-bar-container {
    background-color: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
}

.quiz-progress {
    border-radius: 6px;
}

.quiz-progress :deep(.v-progress-linear__determinate) {
    background: linear-gradient(90deg, #1e257c 0%, #2563eb 50%, #10b981 100%) !important;
}

.quiz-content {
    background-color: #ffffff;
    min-height: 280px;
}

.option-card {
    border-radius: 14px;
    border: 2px solid #e2e8f0 !important;
    background-color: #f8fafc;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.option-card:hover {
    border-color: #1e257c !important;
    background-color: #f0f4ff;
    transform: translateY(-2px);
    box-shadow: 0 6px 18px rgba(30, 37, 124, 0.14) !important;
}

.option-card:hover .option-arrow {
    transform: translateX(4px);
}

.option-arrow {
    transition: transform 0.2s ease;
}

.result-product-card {
    border-radius: 14px !important;
    border: 1.5px solid #e2e8f0;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.3s ease;
}

.result-product-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 10px 24px rgba(30, 37, 124, 0.14);
    border-color: #1e257c;
}

.product-img-wrapper {
    overflow: hidden;
}

.product-img {
    transition: transform 0.5s ease;
}

.result-product-card:hover .product-img {
    transform: scale(1.05);
}

.brand-badge {
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.animate-fade-in {
    animation: fadeIn 0.4s ease-out forwards;
}

.animate-bounce {
    animation: bounce 2s infinite;
}

.animate-pulse {
    animation: pulse 1.5s infinite;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@keyframes bounce {
    0%,
    100% {
        transform: translateY(0);
    }
    50% {
        transform: translateY(-5px);
    }
}

@keyframes pulse {
    0%,
    100% {
        opacity: 1;
    }
    50% {
        opacity: 0.6;
    }
}
</style>
