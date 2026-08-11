<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import { formatCurrency } from '@/utils/formatters';
import { useToastStore } from '@/stores/toastStore';
import { useSeoMeta } from '@/composables/useSeoMeta';

const router = useRouter();
const toastStore = useToastStore();
const { setSeoMeta } = useSeoMeta();

const loading = ref(false);
const currentQuestion = ref(null);
const recommendedProducts = ref([]);
const answers = ref({});
const history = ref([]); // To allow backing out of questions

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
        } else {
            currentQuestion.value = null;
        }
        if (res.recommendedProducts && res.recommendedProducts.length > 0) {
            recommendedProducts.value = res.recommendedProducts;
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

    history.value.push({
        answers: { ...answers.value },
        question: { ...currentQuestion.value }
    });

    answers.value[currentQuestion.value.key] = optionValue;
    await fetchNextQuestion();
};

const goBack = async () => {
    if (history.value.length === 0) return;
    const previous = history.value.pop();
    answers.value = previous.answers;
    currentQuestion.value = previous.question;
    await fetchNextQuestion();
};

const viewProductDetail = (productId) => {
    router.push(`/product/${productId}`);
};

const totalSteps = 5;
const currentStep = computed(() => {
    if (recommendedProducts.value.length > 0) return totalSteps;
    return history.value.length + 1;
});

const progressPercentage = computed(() => {
    return (currentStep.value / totalSteps) * 100;
});

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Gợi Ý Chọn Giày AI | AeroStride',
        description: 'Khảo sát nhanh với AeroStride AI để tìm ra đôi giày chạy bộ, giày thể thao phù hợp nhất với bạn.'
    });
    resetQuiz();
    fetchNextQuestion();
});
</script>

<template>
    <div class="app-container bg-white">
        <MainHeader />

        <main class="main-content">
            <!-- Header Section -->
            <div class="page-header py-12 bg-grey-lighten-4 border-b">
                <v-container>
                    <h1 class="text-h3 font-weight-black text-center text-grey-darken-4 mb-4">GỢI Ý CHỌN GIÀY AI</h1>
                    <p class="text-center text-grey-darken-1 text-subtitle-1 max-w-2xl mx-auto">
                        Hãy hoàn thành bài trắc nghiệm ngắn dưới đây để AeroStride AI phân tích và tìm ra những đôi giày hoàn hảo nhất cho bạn.
                    </p>
                </v-container>
            </div>

            <!-- Quiz Layout Container -->
            <v-container class="py-16">
                <v-row justify="center">
                    <v-col cols="12" md="8" lg="7">
                        <v-card class="quiz-card overflow-hidden border">
                            <!-- Progress Bar -->
                            <div class="progress-bar-container px-8 pt-6 pb-2">
                                <v-progress-linear
                                    :model-value="progressPercentage"
                                    color="primary"
                                    height="8"
                                    rounded
                                    striped
                                    class="quiz-progress"
                                ></v-progress-linear>
                                <div class="d-flex justify-space-between text-caption text-medium-emphasis mt-2">
                                    <span class="font-weight-bold text-slate-600">Bước {{ currentStep }} / {{ totalSteps }}</span>
                                    <span class="font-weight-bold text-primary">{{ Math.round(progressPercentage) }}% Hoàn thành</span>
                                </div>
                            </div>

                            <!-- Divider -->
                            <v-divider></v-divider>

                            <v-card-text class="quiz-content px-8 py-8">
                                <!-- Loading state -->
                                <div v-if="loading" class="d-flex flex-column align-center justify-center py-12">
                                    <v-progress-circular indeterminate color="primary" size="64" width="5"></v-progress-circular>
                                    <div class="mt-6 text-h6 font-weight-medium text-slate-600 animate-pulse">
                                        AeroStride AI đang phân tích dữ liệu...
                                    </div>
                                </div>

                                <div v-else>
                                    <!-- Question Step -->
                                    <div v-if="currentQuestion" class="animate-fade-in">
                                        <div class="d-flex align-center justify-center mb-6">
                                            <v-icon color="primary" size="32" class="mr-2 animate-bounce">mdi-wizard-hat</v-icon>
                                            <span class="text-overline font-weight-bold text-primary tracking-wider">AEROSTRIDE AI QUIZ</span>
                                        </div>
                                        <h2 class="text-h5 font-weight-bold mb-8 text-slate-800 text-center question-text">
                                            {{ currentQuestion.questionText }}
                                        </h2>
                                        <v-row class="ga-4 flex-column align-center px-4">
                                            <v-card
                                                v-for="opt in currentQuestion.options"
                                                :key="opt.value"
                                                class="option-card w-100 pa-5 d-flex align-center justify-space-between border cursor-pointer transition elevation-1"
                                                variant="outlined"
                                                @click="selectOption(opt.value)"
                                            >
                                                <div class="font-weight-bold text-body-1 text-slate-700">{{ opt.label }}</div>
                                                <v-icon color="primary" class="option-arrow">mdi-chevron-right</v-icon>
                                            </v-card>
                                        </v-row>
                                    </div>

                                    <!-- Quiz Recommendations Results -->
                                    <div v-else-if="recommendedProducts.length > 0" class="animate-fade-in">
                                        <div class="text-center mb-8">
                                            <v-icon color="success" size="56" class="mb-3 animate-bounce">mdi-check-circle-outline</v-icon>
                                            <h2 class="text-h4 font-weight-black text-slate-800">Sản Phẩm Phù Hợp Với Bạn!</h2>
                                            <p class="text-subtitle-1 text-medium-emphasis mt-2">
                                                Dưới đây là danh sách sản phẩm được gợi ý riêng dựa trên các câu trả lời của bạn:
                                            </p>
                                        </div>

                                        <v-row class="mt-4">
                                            <v-col v-for="prod in recommendedProducts" :key="prod.id" cols="12" sm="6" class="pa-3">
                                                <v-card class="result-product-card h-100 d-flex flex-column elevation-2" @click="viewProductDetail(prod.id)">
                                                    <div class="product-img-wrapper position-relative">
                                                        <v-img
                                                            :src="prod.hinhAnh || '/assets/images/products/s4.jpg'"
                                                            height="220"
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
                                                            size="small"
                                                            class="brand-badge position-absolute font-weight-bold"
                                                            style="top: 12px; left: 12px"
                                                        >
                                                            {{ prod.tenThuongHieu }}
                                                        </v-chip>
                                                    </div>
                                                    <div class="pa-4 d-flex flex-column justify-space-between flex-grow-1">
                                                        <div>
                                                            <h3 class="font-weight-bold text-body-1 text-slate-800 text-truncate">
                                                                {{ prod.tenSanPham }}
                                                            </h3>
                                                            <div class="text-caption text-medium-emphasis mt-1">
                                                                {{ prod.tenMucDichChay }} | {{ prod.tenChatLieu }}
                                                            </div>
                                                        </div>
                                                        <div class="d-flex align-center justify-space-between mt-4 pt-2 border-t">
                                                            <div class="price-text text-h6 font-weight-black text-primary">
                                                                {{ formatCurrency(prod.giaBanMin || prod.giaBan) }}
                                                            </div>
                                                            <v-btn color="primary" variant="flat" size="small" class="text-none font-weight-bold px-4 rounded-lg">
                                                                Xem chi tiết
                                                            </v-btn>
                                                        </div>
                                                    </div>
                                                </v-card>
                                            </v-col>
                                        </v-row>
                                    </div>

                                    <!-- No matching products -->
                                    <div v-else class="text-center py-12 animate-fade-in">
                                        <v-icon color="warning" size="64" class="mb-4">mdi-alert-circle-outline</v-icon>
                                        <h2 class="text-h5 font-weight-bold text-slate-800">Không tìm thấy sản phẩm phù hợp</h2>
                                        <p class="text-body-1 text-medium-emphasis mt-3 px-8 mx-auto" style="max-width: 500px">
                                            Rất tiếc, hiện tại không có đôi giày nào trong cửa hàng đáp ứng đầy đủ tất cả các lựa chọn của bạn. Bạn hãy thử làm lại khảo sát với các lựa chọn rộng hơn nhé!
                                        </p>
                                        <v-btn
                                            color="primary"
                                            size="large"
                                            class="mt-8 font-weight-bold text-none px-8 rounded-lg"
                                            @click="
                                                resetQuiz();
                                                fetchNextQuestion();
                                            "
                                        >
                                            <v-icon start class="mr-2">mdi-refresh</v-icon>
                                            Thử Lại Khảo Sát
                                        </v-btn>
                                    </div>
                                </div>
                            </v-card-text>

                            <!-- Footer Navigation -->
                            <v-card-actions class="quiz-footer px-8 py-5 border-t bg-grey-lighten-4 d-flex justify-space-between">
                                <v-btn
                                    variant="outlined"
                                    color="secondary"
                                    class="text-none font-weight-bold px-5 py-2 rounded-lg"
                                    :disabled="history.length === 0 || loading"
                                    @click="goBack"
                                >
                                    <v-icon start class="mr-1">mdi-arrow-left</v-icon>
                                    Quay lại
                                </v-btn>

                                <v-btn
                                    variant="text"
                                    color="error"
                                    class="text-none font-weight-bold px-5 py-2 rounded-lg"
                                    :disabled="loading"
                                    @click="
                                        resetQuiz();
                                        fetchNextQuestion();
                                    "
                                >
                                    <v-icon start class="mr-1">mdi-refresh</v-icon>
                                    Làm mới khảo sát
                                </v-btn>
                            </v-card-actions>
                        </v-card>
                    </v-col>
                </v-row>
            </v-container>

            <!-- Dynamic AI Recommended Products Section -->
            <section v-if="recommendedProducts.length > 0" class="ai-recommendations-section py-12 bg-slate-100 border-t">
                <v-container>
                    <div class="d-flex align-center justify-space-between flex-wrap ga-4 mb-8">
                        <div>
                            <div class="d-flex align-center mb-2">
                                <v-chip color="primary" variant="flat" size="small" class="font-weight-bold mr-2">
                                    <v-icon start size="16">mdi-robot-outline</v-icon>
                                    AI ASSISTED
                                </v-chip>
                                <span class="text-caption font-weight-bold text-slate-500 text-uppercase tracking-wider">Tự động gợi ý theo lựa chọn</span>
                            </div>
                            <h2 class="text-h4 font-weight-black text-slate-800">DANH SÁCH SẢN PHẨM GỢI Ý BỞI AI</h2>
                            <p class="text-body-2 text-slate-600 mb-0">
                                Dưới đây là danh sách đôi giày phù hợp nhất với tiêu chí của bạn (Tìm thấy {{ recommendedProducts.length }} sản phẩm):
                            </p>
                        </div>
                    </div>

                    <v-row class="products-grid-row">
                        <v-col v-for="prod in recommendedProducts" :key="prod.id" cols="6" sm="6" md="4" lg="3" class="pa-2 pa-sm-3">
                            <v-card class="product-item-card bg-white rounded-xl overflow-hidden border elevation-1 h-100 d-flex flex-column cursor-pointer transition" @click="viewProductDetail(prod.id)">
                                <div class="position-relative overflow-hidden bg-slate-100 recommend-card-img-box">
                                    <v-img :src="prod.hinhAnh || '/assets/images/products/s4.jpg'" height="100%" cover class="product-img">
                                        <template #placeholder>
                                            <div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
                                                <v-progress-circular indeterminate color="primary"></v-progress-circular>
                                            </div>
                                        </template>
                                    </v-img>
                                    <v-chip color="primary" size="x-small" class="position-absolute font-weight-bold shadow-sm" style="top: 10px; left: 10px; z-index: 2">
                                        🤖 AI Phù hợp 98%
                                    </v-chip>
                                </div>

                                <div class="pa-4 d-flex flex-column flex-grow-1">
                                    <div class="text-caption font-weight-bold text-primary text-uppercase mb-1">
                                        {{ prod.tenThuongHieu || 'AEROSTRIDE' }}
                                    </div>
                                    <h3 class="product-title font-weight-bold text-body-1 text-slate-800 mb-2 line-clamp-2" style="height: 42px; line-height: 1.3">
                                        {{ prod.tenSanPham }}
                                    </h3>
                                    <div class="d-flex ga-1 flex-wrap mb-3">
                                        <v-chip size="x-small" variant="tonal" color="secondary" class="font-weight-medium" v-if="prod.tenMucDichChay">
                                            {{ prod.tenMucDichChay }}
                                        </v-chip>
                                        <v-chip size="x-small" variant="tonal" color="info" class="font-weight-medium" v-if="prod.tenChatLieu">
                                            {{ prod.tenChatLieu }}
                                        </v-chip>
                                    </div>
                                    <div class="mt-auto d-flex align-center justify-space-between pt-2 border-t">
                                        <div class="font-weight-black text-primary text-body-1">
                                            {{ formatCurrency(prod.giaBanMin || prod.giaBan) }}
                                        </div>
                                        <v-btn size="small" variant="flat" color="primary" class="rounded-pill font-weight-bold text-none px-3">
                                            Xem chi tiết
                                        </v-btn>
                                    </div>
                                </div>
                            </v-card>
                        </v-col>
                    </v-row>
                </v-container>
            </section>
        </main>

        <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t">
            <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
            <p>&copy; 2026 AeroStride All rights reserved.</p>
        </footer>
    </div>
</template>

<style scoped>
.main-content {
    padding-top: 84px;
    min-height: calc(100vh - 150px);
    background-color: #f8fafc;
}

.max-w-2xl {
    max-width: 42rem;
}

.mx-auto {
    margin-left: auto;
    margin-right: auto;
}

.quiz-card {
    border-radius: 20px !important;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05) !important;
    border-color: #e2e8f0 !important;
    background-color: #ffffff;
}

.progress-bar-container {
    background-color: #ffffff;
}

.quiz-progress {
    border-radius: 6px;
}

.quiz-progress :deep(.v-progress-linear__determinate) {
    background: linear-gradient(90deg, #2962ff 0%, #00e676 100%) !important;
}

.quiz-content {
    min-height: 320px;
}

.question-text {
    line-height: 1.4;
}

.option-card {
    border-radius: 14px;
    border-color: #e2e8f0 !important;
    background-color: #f8fafc;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.option-card:hover {
    border-color: #2962ff !important;
    background-color: #f0f4ff;
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(41, 98, 255, 0.15) !important;
}

.option-card:hover .option-arrow {
    transform: translateX(4px);
}

.option-arrow {
    transition: transform 0.2s ease;
}

.result-product-card {
    border-radius: 16px !important;
    border: 1px solid #e2e8f0;
    overflow: hidden;
    cursor: pointer;
    background-color: #ffffff;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.result-product-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12) !important;
    border-color: #2962ff;
}

.product-img-wrapper {
    overflow: hidden;
}

.product-img {
    transition: transform 0.6s ease;
}

.result-product-card:hover .product-img {
    transform: scale(1.06);
}

.brand-badge {
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
}

.quiz-footer {
    background-color: #f8fafc;
}

.animate-fade-in {
    animation: fadeIn 0.45s ease-out forwards;
}

.animate-bounce {
    animation: bounce 2s infinite;
}

.animate-pulse {
    animation: pulse 1.8s infinite;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(12px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@keyframes bounce {
    0%, 100% {
        transform: translateY(0);
    }
    50% {
        transform: translateY(-6px);
    }
}

@keyframes pulse {
    0%, 100% {
        opacity: 1;
    }
    50% {
        opacity: 0.55;
    }
}

.recommend-card-img-box {
    height: 180px;
}

@media (max-width: 600px) {
    .recommend-card-img-box {
        height: 130px !important;
    }
}
</style>
