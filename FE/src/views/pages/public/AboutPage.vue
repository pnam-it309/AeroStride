<script setup>
import { onMounted } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

const { setSeoMeta } = useSeoMeta();

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Giới Thiệu | AeroStride',
        description: 'Khám phá câu chuyện thương hiệu AeroStride, sứ mệnh và hành trình mang đến những đôi giày thể thao đẳng cấp nhất cho người Việt.'
    });
});
</script>

<template>
    <div class="app-container bg-white">
        <MainHeader />

        <main class="main-content">
            <!-- Hero Section Enhanced -->
            <section class="about-hero">
                <div class="hero-overlay"></div>
                <!-- Decorative elements -->
                <div class="floating-shape shape-1"></div>
                <div class="floating-shape shape-2"></div>
                <div class="floating-shape shape-3"></div>

                <div class="hero-content text-center">
                    <div class="hero-badge animate-up text-white">EST. 2024</div>
                    <h1 class="hero-title font-weight-black mb-4 animate-up delay-1">
                        BƯỚC CHÂN CỦA SỰ <span class="text-gradient">ĐAM MÊ</span>
                    </h1>
                    <p class="hero-subtitle animate-up delay-2">
                        Hành trình kiến tạo chuẩn mực giày thể thao mới tại Việt Nam.
                    </p>
                    <v-btn color="white" variant="outlined" size="x-large" rounded="pill"
                        class="mt-8 animate-up delay-3 explore-btn" append-icon="mdi-arrow-down" @click="scrollToAbout">
                        KHÁM PHÁ CÂU CHUYỆN
                    </v-btn>
                </div>
            </section>

            <!-- Mission & Vision -->
            <section id="about-content" class="py-16 position-relative overflow-hidden">
                <div class="bg-pattern"></div>
                <v-container>
                    <v-row align="center" justify="center">
                        <v-col cols="12" md="5" class="pr-md-10 z-index-1">
                            <div class="subtitle-badge mb-4">VỀ CHÚNG TÔI</div>
                            <h2 class="text-h3 font-weight-black mb-6 text-black">AeroStride là ai?</h2>
                            <p class="text-body-1 text-grey-darken-2 mb-6 leading-relaxed">
                                Được thành lập vào năm 2024, <strong class="text-black">AeroStride</strong> không chỉ là
                                một thương hiệu bán lẻ giày thể thao, mà còn là nơi hội tụ của những tâm hồn đam mê dịch
                                chuyển. Chúng tôi tin rằng mỗi bước đi đều mang một ý nghĩa riêng, và một đôi giày tốt
                                sẽ là người bạn đồng hành hoàn hảo nhất.
                            </p>
                            <p class="text-body-1 text-grey-darken-2 leading-relaxed mb-6">
                                Với cam kết <strong class="text-black">100% sản phẩm chính hãng</strong>, dịch vụ khách
                                hàng vượt trội và không gian mua sắm hiện đại, AeroStride đang từng bước khẳng định vị
                                thế dẫn đầu trong ngành bán lẻ đồ thể thao cao cấp.
                            </p>

                            <div class="d-flex align-center ga-4 mt-8">
                                <div class="founder-avatar">
                                    <v-img
                                        src="https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&q=80&w=200"
                                        cover></v-img>
                                </div>
                                <div>
                                    <div class="font-weight-black text-black">Tấn Dũng</div>
                                    <div class="text-caption text-grey-darken-1">Founder & CEO</div>
                                </div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="7" class="mt-8 mt-md-0 z-index-1">
                            <div class="image-grid">
                                <div class="img-wrapper img-1 hover-scale">
                                    <v-img
                                        src="https://images.unsplash.com/photo-1556906781-9a412961c28c?auto=format&fit=crop&q=80&w=800"
                                        cover height="100%"></v-img>
                                </div>
                                <div class="img-wrapper img-2 hover-scale">
                                    <v-img
                                        src="https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&q=80&w=800"
                                        cover height="100%"></v-img>
                                </div>
                                <div class="img-wrapper img-3 hover-scale">
                                    <v-img
                                        src="https://images.unsplash.com/photo-1514989940723-e8e51635b782?auto=format&fit=crop&q=80&w=800"
                                        cover height="100%"></v-img>
                                </div>
                            </div>
                        </v-col>
                    </v-row>
                </v-container>
            </section>

            <!-- Core Values (New Section) -->
            <section class="core-values py-16 bg-black text-white">
                <v-container>
                    <div class="text-center mb-12">
                        <div class="subtitle-badge dark mb-4 mx-auto">GIÁ TRỊ CỐT LÕI</div>
                        <h2 class="text-h3 font-weight-black text-white">Điều Chúng Tôi Theo Đuổi</h2>
                    </div>

                    <v-row>
                        <v-col cols="12" md="4" v-for="(value, i) in coreValues" :key="i">
                            <div class="value-card hover-lift">
                                <div class="icon-wrap mb-6">
                                    <v-icon :icon="value.icon" size="40" color="white"></v-icon>
                                </div>
                                <h3 class="text-h5 font-weight-bold mb-3 text-white">{{ value.title }}</h3>
                                <p class="text-grey-lighten-1 leading-relaxed">{{ value.desc }}</p>
                            </div>
                        </v-col>
                    </v-row>
                </v-container>
            </section>

            <!-- Stats -->
            <section class="stats-section py-16 position-relative">
                <v-container>
                    <v-row class="text-center">
                        <v-col cols="12" sm="4">
                            <div class="stat-card">
                                <div class="stat-number">50+</div>
                                <div class="stat-label">Chi Nhánh Toàn Quốc</div>
                            </div>
                        </v-col>
                        <v-col cols="12" sm="4">
                            <div class="stat-card">
                                <div class="stat-number">1M+</div>
                                <div class="stat-label">Khách Hàng Tin Tưởng</div>
                            </div>
                        </v-col>
                        <v-col cols="12" sm="4">
                            <div class="stat-card">
                                <div class="stat-number">100%</div>
                                <div class="stat-label">Sản Phẩm Chính Hãng</div>
                            </div>
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

<script>
export default {
    data() {
        return {
            coreValues: [
                { icon: 'mdi-shield-check-outline', title: 'Chất Lượng', desc: 'Chỉ cung cấp những sản phẩm chính hãng với chất lượng kiểm định khắt khe nhất.' },
                { icon: 'mdi-lightning-bolt-outline', title: 'Tốc Độ', desc: 'Giao hàng nhanh chóng, hỗ trợ tức thì. Chúng tôi không để niềm đam mê của bạn phải chờ đợi.' },
                { icon: 'mdi-heart-outline', title: 'Tận Tâm', desc: 'Dịch vụ chăm sóc khách hàng 24/7, luôn đặt sự hài lòng của bạn lên hàng đầu.' }
            ]
        }
    },
    methods: {
        scrollToAbout() {
            document.getElementById('about-content').scrollIntoView({ behavior: 'smooth' });
        }
    }
}
</script>

<style scoped>
/* Force text colors to override Vuetify defaults */
.text-white {
    color: #ffffff !important;
}

.text-black {
    color: #000000 !important;
}

.main-content {
    padding-top: 60px;
    /* Offset for header */
}

/* Hero Section */
.about-hero {
    position: relative;
    height: 75vh;
    min-height: 500px;
    background-image: url('https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=2000');
    background-size: cover;
    background-position: center;
    background-attachment: fixed;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
}

.hero-overlay {
    position: absolute;
    inset: 0;
    /* Stronger, darker gradient to guarantee white text contrast */
    background: linear-gradient(135deg, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.5) 50%, rgba(10, 10, 30, 0.85) 100%);
}

.hero-content {
    position: relative;
    z-index: 10;
    max-width: 900px;
    padding: 0 20px;
}

.hero-title {
    font-size: clamp(2.5rem, 5vw, 4.5rem);
    line-height: 1.1;
    color: #ffffff !important;
    text-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

.hero-subtitle {
    font-size: clamp(1.1rem, 2vw, 1.5rem);
    color: rgba(255, 255, 255, 0.9) !important;
    font-weight: 300;
    text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
}

.text-gradient {
    background: linear-gradient(90deg, #ff416c, #ff4b2b);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.hero-badge {
    display: inline-block;
    padding: 6px 16px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 100px;
    font-size: 0.85rem;
    letter-spacing: 3px;
    margin-bottom: 24px;
    backdrop-filter: blur(4px);
    background: rgba(0, 0, 0, 0.2);
}

.explore-btn {
    transition: all 0.3s ease;
    border-width: 2px;
}

.explore-btn:hover {
    background: #fff;
    color: #000 !important;
    transform: translateY(-3px);
    box-shadow: 0 10px 25px rgba(255, 255, 255, 0.2);
}

/* Floating Shapes */
.floating-shape {
    position: absolute;
    border-radius: 50%;
    background: linear-gradient(45deg, rgba(41, 98, 255, 0.2), rgba(255, 65, 108, 0.2));
    filter: blur(40px);
    z-index: 1;
    animation: float 10s infinite alternate ease-in-out;
}

.shape-1 {
    width: 300px;
    height: 300px;
    top: -100px;
    left: -100px;
}

.shape-2 {
    width: 400px;
    height: 400px;
    bottom: -150px;
    right: -100px;
    animation-delay: -5s;
}

.shape-3 {
    width: 200px;
    height: 200px;
    top: 30%;
    left: 60%;
    background: linear-gradient(45deg, rgba(255, 255, 255, 0.1), transparent);
    animation-duration: 15s;
}

@keyframes float {
    0% {
        transform: translate(0, 0) rotate(0deg);
    }

    100% {
        transform: translate(50px, 30px) rotate(20deg);
    }
}

/* Content Elements */
.subtitle-badge {
    display: inline-block;
    padding: 6px 14px;
    background: rgba(41, 98, 255, 0.1);
    color: #2962ff;
    font-weight: 800;
    letter-spacing: 2px;
    border-radius: 6px;
    font-size: 0.8rem;

    &.dark {
        background: rgba(255, 255, 255, 0.1);
        color: #fff;
    }
}

.leading-relaxed {
    line-height: 1.8;
}

.uppercase {
    text-transform: uppercase;
}

.tracking-wide {
    letter-spacing: 2px;
}

/* Image Grid */
.image-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 200px);
    gap: 16px;
}

.img-wrapper {
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.img-1 {
    grid-column: 1 / 2;
    grid-row: 1 / 3;
}

.img-2 {
    grid-column: 2 / 3;
    grid-row: 1 / 2;
}

.img-3 {
    grid-column: 2 / 3;
    grid-row: 2 / 3;
}

.hover-scale {
    transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.hover-scale:hover {
    transform: scale(1.03);
}

.hover-scale .v-img {
    transition: transform 0.8s ease;
}

.hover-scale:hover .v-img {
    transform: scale(1.1);
}

/* Core Values */
.core-values {
    background-color: #0f172a !important;
    /* Elegant slate-900 dark background */
}

.value-card {
    padding: 40px 30px;
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 20px;
    height: 100%;
    transition: all 0.4s ease;
}

.value-card .icon-wrap {
    width: 80px;
    height: 80px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;
    transition: all 0.4s ease;
}

.hover-lift:hover {
    transform: translateY(-10px);
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(255, 255, 255, 0.2);
}

.hover-lift:hover .icon-wrap {
    background: #fff;
}

.hover-lift:hover .icon-wrap i {
    color: #000 !important;
}

/* Stats */
.stats-section {
    background-color: #f8fafc !important;
    /* Soft light slate background */
}

.stat-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 24px;
    padding: 40px 32px;
    height: 100%;
    box-shadow: 0 10px 25px -5px rgba(30, 37, 124, 0.04), 0 8px 10px -6px rgba(30, 37, 124, 0.04);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;

    &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 4px;
        background: linear-gradient(90deg, #1e257c, #2563eb);
        opacity: 0;
        transition: opacity 0.3s ease;
    }

    &:hover {
        transform: translateY(-8px);
        box-shadow: 0 20px 40px -15px rgba(30, 37, 124, 0.12);
        border-color: rgba(30, 37, 124, 0.15);

        &::before {
            opacity: 1;
        }

        .stat-number {
            background: linear-gradient(135deg, #1e257c 0%, #2563eb 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
    }
}

.stat-number {
    font-size: 3.5rem;
    line-height: 1;
    font-weight: 900;
    background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    transition: all 0.4s ease;
}

.stat-label {
    font-size: 0.9rem;
    font-weight: 700;
    color: #475569;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    margin-top: 12px;
}

/* Founder */
.founder-avatar {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    overflow: hidden;
    border: 2px solid #2962ff;
}

/* Animations */
.animate-up {
    animation: fadeInUp 1s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    opacity: 0;
    transform: translateY(40px);
}

.delay-1 {
    animation-delay: 0.2s;
}

.delay-2 {
    animation-delay: 0.4s;
}

.delay-3 {
    animation-delay: 0.6s;
}

@keyframes fadeInUp {
    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>
