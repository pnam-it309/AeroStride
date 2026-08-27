<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
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

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Gợi Ý Chọn Sản Phẩm Phù Hợp Với Bạn | AeroStride',
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
            <!-- Header Section: Logo Blue Gradient + Crisp White Text -->
            <div class="page-header py-12 border-b text-white">
                <v-container>
                    <div class="d-flex justify-center mb-3">
                        <v-chip
                            variant="flat"
                            size="small"
                            class="font-weight-black px-4 shadow-sm"
                            style="background-color: rgba(255, 255, 255, 0.15) !important; color: #ffffff !important; border: 1px solid rgba(255, 255, 255, 0.25) !important;"
                        >
                            <v-icon start size="16" color="white">mdi-robot-excited-outline</v-icon>
                            AEROSTRIDE AI ASSISTANT
                        </v-chip>
                    </div>
                    <h1 class="page-header-title text-h3 font-weight-black text-center mb-3">GỢI Ý CHỌN SẢN PHẨM</h1>
                    <p class="page-header-subtitle text-center text-subtitle-1 max-w-2xl mx-auto mb-0">
                        Hãy hoàn thành bài trắc nghiệm ngắn dưới đây để AeroStride AI phân tích phong cách và nhu cầu để tìm ra những đôi giày hoàn hảo nhất cho bạn.
                    </p>
                </v-container>
            </div>

            <!-- Quiz Layout Container: Dynamic Wide Grid on Results -->
            <v-container :class="['py-10', { 'max-w-1300': recommendedProducts.length > 0 }]">
                <!-- Case 1: In Progress / Loading / Asking Questions -->
                <v-row v-if="!isFinished || loading" justify="center">
                    <v-col cols="12" md="10" lg="8" xl="7">
                        <v-card class="quiz-card overflow-hidden border">
                            <!-- Progress Bar -->
                            <div class="progress-bar-container px-6 px-md-10 pt-6 pb-2">
                                <v-progress-linear
                                    :model-value="progressPercentage"
                                    color="primary"
                                    height="10"
                                    rounded
                                    striped
                                    class="quiz-progress"
                                ></v-progress-linear>
                                <div class="d-flex justify-space-between text-caption text-medium-emphasis mt-2">
                                    <span class="font-weight-bold text-slate-700">Bước {{ currentStep }} / {{ totalSteps }}</span>
                                    <span class="font-weight-bold text-primary">{{ Math.round(progressPercentage) }}% Hoàn thành</span>
                                </div>
                            </div>

                            <!-- Divider -->
                            <v-divider></v-divider>

                            <v-card-text class="quiz-content px-6 px-md-10 py-8">
                                <!-- Loading state -->
                                <div v-if="loading" class="d-flex flex-column align-center justify-center py-12">
                                    <v-progress-circular indeterminate color="primary" size="64" width="5"></v-progress-circular>
                                    <div class="mt-6 text-h6 font-weight-bold text-slate-700 animate-pulse">
                                        AeroStride AI đang phân tích dữ liệu...
                                    </div>
                                </div>

                                <!-- Question Step -->
                                <div v-else-if="currentQuestion" class="animate-fade-in">
                                    <div class="d-flex align-center justify-center mb-6">
                                        <v-icon color="primary" size="30" class="mr-2 animate-bounce">mdi-wizard-hat</v-icon>
                                        <span class="text-overline font-weight-bold text-primary tracking-wider">AEROSTRIDE AI QUIZ</span>
                                    </div>
                                    <h2 class="text-h5 font-weight-black mb-8 text-slate-900 text-center question-text">
                                        {{ currentQuestion.questionText }}
                                    </h2>
                                    <v-row class="ga-3" justify="center">
                                        <v-col
                                            v-for="opt in currentQuestion.options"
                                            :key="opt.value"
                                            cols="12"
                                            :sm="currentQuestion.options.length > 2 ? 6 : 12"
                                            class="pa-1"
                                        >
                                            <v-card
                                                class="option-card h-100 pa-4 pa-md-5 d-flex align-center justify-space-between border cursor-pointer transition elevation-1"
                                                variant="outlined"
                                                @click="selectOption(opt.value)"
                                            >
                                                <div class="font-weight-bold text-subtitle-1 text-slate-800">{{ opt.label }}</div>
                                                <v-icon color="primary" size="22" class="option-arrow">mdi-chevron-right</v-icon>
                                            </v-card>
                                        </v-col>
                                    </v-row>
                                </div>
                            </v-card-text>

                            <!-- Footer Navigation -->
                            <v-card-actions class="quiz-footer px-6 px-md-10 py-4 border-t bg-grey-lighten-4 d-flex justify-space-between">
                                <v-btn
                                    variant="outlined"
                                    color="secondary"
                                    class="text-none font-weight-bold px-4 py-2 rounded-lg"
                                    :disabled="history.length === 0 || loading"
                                    @click="goBack"
                                >
                                    <v-icon start class="mr-1">mdi-arrow-left</v-icon>
                                    Quay lại
                                </v-btn>

                                <v-btn
                                    variant="text"
                                    color="grey-darken-2"
                                    class="text-none font-weight-bold"
                                    @click="
                                        resetQuiz();
                                        fetchNextQuestion();
                                    "
                                >
                                    <v-icon start class="mr-1">mdi-refresh</v-icon>
                                    Làm lại từ đầu
                                </v-btn>
                            </v-card-actions>
                        </v-card>
                    </v-col>
                </v-row>

                <!-- Case 2: Quiz Finished & Recommendations Results (Full Wide Grid) -->
                <div v-else-if="recommendedProducts.length > 0" class="animate-fade-in">
                    <div class="results-header-banner bg-white border rounded-2xl pa-6 pa-md-8 mb-8 text-center elevation-1">
                        <v-icon color="success" size="56" class="mb-3 animate-bounce">mdi-check-circle-outline</v-icon>
                        <h2 class="text-h4 font-weight-black text-slate-900 mb-2">Sản Phẩm Phù Hợp Hoàn Hảo Với Bạn!</h2>
                        <p class="text-subtitle-1 text-slate-600 max-w-2xl mx-auto mb-4">
                            Dựa trên phân tích tiêu chí của bạn, AeroStride AI đã chọn lọc được <strong>{{ recommendedProducts.length }}</strong> mẫu giày phù hợp nhất:
                        </p>
                        <div class="d-flex justify-center flex-wrap ga-3">
                            <v-btn
                                color="primary"
                                variant="flat"
                                class="rounded-pill font-weight-bold text-none px-6"
                                @click="
                                    resetQuiz();
                                    fetchNextQuestion();
                                "
                            >
                                <v-icon start class="mr-1">mdi-refresh</v-icon>
                                Làm lại khảo sát
                            </v-btn>
                            <v-btn
                                variant="outlined"
                                color="primary"
                                class="rounded-pill font-weight-bold text-none px-6"
                                to="/shoes"
                            >
                                <v-icon start class="mr-1">mdi-shoe-sneaker</v-icon>
                                Khám phá tất cả sản phẩm
                            </v-btn>
                        </div>
                    </div>

                    <!-- Products Grid: 4 columns on large desktop, 3 on laptop, 2 on tablet/mobile -->
                    <v-row class="products-grid-row">
                        <v-col
                            v-for="prod in recommendedProducts"
                            :key="prod.id"
                            cols="12"
                            sm="6"
                            md="4"
                            lg="3"
                            class="pa-3"
                        >
                            <v-card
                                class="result-product-card h-100 d-flex flex-column elevation-2 hover-lift border bg-white rounded-2xl overflow-hidden cursor-pointer"
                                @click="viewProductDetail(prod.id)"
                            >
                                <div class="product-img-wrapper position-relative bg-slate-50">
                                    <v-img
                                        :src="prod.hinhAnh || '/assets/images/products/s4.jpg'"
                                        height="230"
                                        cover
                                        class="product-img"
                                    >
                                        <template #placeholder>
                                            <div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
                                                <v-progress-circular indeterminate color="primary" size="28"></v-progress-circular>
                                            </div>
                                        </template>
                                    </v-img>
                                    <v-chip
                                        color="primary"
                                        size="small"
                                        class="brand-badge position-absolute font-weight-bold"
                                        style="top: 12px; left: 12px"
                                    >
                                        {{ prod.tenThuongHieu || 'AeroStride' }}
                                    </v-chip>
                                </div>
                                <div class="pa-5 d-flex flex-column justify-space-between flex-grow-1">
                                    <div>
                                        <h3 class="font-weight-bold text-subtitle-1 text-slate-900 text-truncate" :title="prod.tenSanPham">
                                            {{ prod.tenSanPham }}
                                        </h3>
                                        <div class="d-flex ga-1 flex-wrap mt-2">
                                            <v-chip size="x-small" variant="tonal" color="primary" class="font-weight-medium" v-if="prod.tenMucDichChay">
                                                {{ prod.tenMucDichChay }}
                                            </v-chip>
                                            <v-chip size="x-small" variant="tonal" color="info" class="font-weight-medium" v-if="prod.tenChatLieu">
                                                {{ prod.tenChatLieu }}
                                            </v-chip>
                                        </div>
                                    </div>
                                    <div class="d-flex align-center justify-space-between mt-4 pt-3 border-t">
                                        <div>
                                            <div class="text-caption text-slate-400 font-weight-medium">Giá bán</div>
                                            <div class="price-text text-h6 font-weight-black text-primary">
                                                {{ formatProductPrice(prod) }}
                                            </div>
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

                <!-- Case 3: No Matching Products -->
                <v-row v-else justify="center" class="animate-fade-in">
                    <v-col cols="12" md="8" lg="6">
                        <v-card class="pa-10 text-center rounded-2xl border elevation-2 bg-white">
                            <v-icon color="warning" size="64" class="mb-4">mdi-alert-circle-outline</v-icon>
                            <h2 class="text-h5 font-weight-black text-slate-900 mb-2">Không tìm thấy sản phẩm phù hợp</h2>
                            <p class="text-body-1 text-slate-600 mb-6">
                                Rất tiếc, hiện tại không có mẫu giày nào đáp ứng đầy đủ tất cả các tiêu chí bạn đã chọn. Bạn hãy thử làm lại khảo sát với tiêu chí mở rộng hơn nhé!
                            </p>
                            <div class="d-flex justify-center flex-wrap ga-3">
                                <v-btn
                                    color="primary"
                                    size="large"
                                    class="font-weight-bold text-none px-6 rounded-lg"
                                    @click="
                                        resetQuiz();
                                        fetchNextQuestion();
                                    "
                                >
                                    <v-icon start class="mr-2">mdi-refresh</v-icon>
                                    Thử Lại Khảo Sát
                                </v-btn>
                                <v-btn
                                    variant="outlined"
                                    color="primary"
                                    size="large"
                                    class="font-weight-bold text-none px-6 rounded-lg"
                                    to="/shoes"
                                >
                                    Xem Tất Cả Sản Phẩm
                                </v-btn>
                            </div>
                        </v-card>
                    </v-col>
                </v-row>
            </v-container>
        </main>

        <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t">
            <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
            <p>&copy; 2026 AeroStride All rights reserved.</p>
        </footer>

        <CustomerChat />
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

.page-header {
    background: linear-gradient(135deg, #1e257c 0%, #23318c 50%, #1d4ed8 100%) !important;
    position: relative;
    box-shadow: 0 4px 20px rgba(30, 37, 124, 0.25);
    color: #ffffff !important;
}

.page-header-title {
    color: #ffffff !important;
    letter-spacing: -0.5px;
    font-size: 2.25rem !important;
    line-height: 1.25 !important;
}

.page-header-subtitle {
    color: rgba(255, 255, 255, 0.9) !important;
    font-weight: 500;
}

.quiz-card {
    border-radius: 24px !important;
    box-shadow: 0 12px 35px rgba(30, 37, 124, 0.08) !important;
    border: 1.5px solid #e2e8f0 !important;
    background-color: #ffffff;
}

.progress-bar-container {
    background-color: #ffffff;
}

.quiz-progress {
    border-radius: 8px;
}

.quiz-progress :deep(.v-progress-linear__determinate) {
    background: linear-gradient(90deg, #1e257c 0%, #2563eb 50%, #10b981 100%) !important;
}

.quiz-content {
    min-height: 340px;
}

.question-text {
    line-height: 1.35;
    letter-spacing: -0.2px;
}

.option-card {
    border-radius: 16px !important;
    border: 2px solid #e2e8f0 !important;
    background-color: #f8fafc;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.option-card:hover {
    border-color: #1e257c !important;
    background-color: #f0f4ff;
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(30, 37, 124, 0.16) !important;
}

.option-card:hover .option-arrow {
    transform: translateX(6px);
}

.option-arrow {
    transition: transform 0.2s ease;
}

.result-product-card {
    border-radius: 18px !important;
    border: 1.5px solid #e2e8f0;
    overflow: hidden;
    cursor: pointer;
    background-color: #ffffff;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.result-product-card:hover {
    transform: translateY(-6px);
    box-shadow: 0 16px 36px rgba(30, 37, 124, 0.15) !important;
    border-color: #1e257c;
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
    background-color: #1e257c !important;
    color: #ffffff !important;
    box-shadow: 0 4px 10px rgba(30, 37, 124, 0.3);
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
