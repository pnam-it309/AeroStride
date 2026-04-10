<script setup>
import Logo from '@/layouts/full/logo/Logo.vue';
import LoginForm from '@/components/auth/LoginForm.vue';
</script>

<template>
    <div class="auth-wrapper mesh-bg h-100vh overflow-hidden">
        <v-container fluid class="pa-0 h-100">
            <v-row no-gutters class="h-100 align-center justify-center">
                <!-- Main Login Card -->
                <v-col cols="11" sm="9" md="10" lg="10" xl="8" class="relative z-index-2">
                    <v-card class="glass-container overflow-hidden elevation-24 mt-n12">
                        <v-row no-gutters>
                            <!-- Visual Branding Side -->
                            <v-col cols="12" lg="7" class="d-none d-lg-flex flex-column align-center justify-center visual-side pa-10 text-center relative overflow-hidden">
                                <div class="floating-elements">
                                    <div class="shape s1"></div>
                                    <div class="shape s2"></div>
                                </div>
                                
                                <div class="content-up relative z-index-2 w-100">
                                    <div class="mb-6 transform-zoom">
                                        <Logo class="mega-logo" />
                                    </div>
                                    <h1 class="text-h2 font-weight-black mb-4 text-white gradient-text">SẢI BƯỚC CÙNG AEROSTRIDE</h1>
                                    <p class="text-h6 text-white opacity-80 mb-8 max-w-500 mx-auto">
                                        Nền tảng quản lý chuyên nghiệp cho kỷ nguyên giày thể thao mới. 
                                        Cùng chúng tôi kiến tạo tương lai.
                                    </p>
                                    <div class="shoe-container">
                                        <v-img
                                            src="https://images.unsplash.com/photo-1542291026-7eec264c27ff?q=80&w=2070&auto=format&fit=crop"
                                            width="550"
                                            class="mx-auto floating-shoe-3d reflect-shoe"
                                        ></v-img>
                                    </div>
                                </div>
                            </v-col>

                            <!-- Auth Form Side -->
                            <v-col cols="12" lg="5" class="d-flex align-center justify-center bg-white-op-90 pa-sm-12 pa-6 relative">
                                <div class="auth-form-wrap w-100" style="max-width: 420px;">
                                    <div class="text-center mb-10 d-lg-none transform-zoom">
                                        <Logo class="mega-logo" />
                                    </div>
                                    <div class="mb-8">
                                        <h2 class="text-h3 font-weight-black mb-2 text-primary">CHÀO MỪNG</h2>
                                        <div class="d-flex align-center">
                                            <div class="h-line mr-2"></div>
                                            <p class="text-subtitle-1 text-grey-darken-1 font-weight-medium">Đăng nhập tài quản trị</p>
                                        </div>
                                    </div>
                                    
                                    <LoginForm />
                                    
                                    <div class="mt-10 text-center">
                                        <v-divider class="mb-6"></v-divider>
                                        <p class="text-body-1 text-grey-darken-1">
                                            Đối tác mới của chúng tôi?
                                            <RouterLink to="/auth/register" class="text-primary text-decoration-none font-weight-black ml-1 premium-link">
                                                Gia nhập ngay
                                            </RouterLink>
                                        </p>
                                    </div>
                                </div>
                            </v-col>
                        </v-row>
                    </v-card>
                </v-col>
            </v-row>
        </v-container>

        <!-- Dynamic Background Orbs -->
        <div class="orb o1"></div>
        <div class="orb o2"></div>
        <div class="orb o3"></div>
    </div>
</template>

<style scoped>
.h-100vh { height: 100vh; }
.relative { position: relative; }
.z-index-2 { z-index: 2; }

/* MEGA MESH BACKGROUND */
.mesh-bg {
    background-color: #f8fafc;
    background-image: 
        radial-gradient(at 0% 0%, rgba(142, 197, 252, 0.4) 0px, transparent 50%),
        radial-gradient(at 100% 0%, rgba(224, 195, 252, 0.4) 0px, transparent 50%),
        radial-gradient(at 100% 100%, rgba(142, 197, 252, 0.4) 0px, transparent 50%),
        radial-gradient(at 0% 100%, rgba(224, 195, 252, 0.4) 0px, transparent 50%);
    overflow: hidden !important;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100vw;
    height: 100vh;
}

/* GLASSMORPHISM CONTAINER */
.glass-container {
    background: rgba(255, 255, 255, 0.1) !important;
    backdrop-filter: blur(25px) saturate(180%);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 40px !important;
    max-height: 90vh;
}

.visual-side {
    background: linear-gradient(135deg, rgba(var(--v-theme-primary), 0.9) 0%, rgba(142, 197, 252, 0.9) 100%);
}

/* LOGO UPGRADE */
.mega-logo :deep(img) {
    height: 120px !important;
    filter: drop-shadow(0 10px 15px rgba(0,0,0,0.1));
}

.transform-zoom {
    transition: transform 0.3s ease;
}
.transform-zoom:hover {
    transform: scale(1.1);
}

/* GRADIENT TEXT */
.gradient-text {
    background: linear-gradient(to right, #ffffff, #e2e8f0);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: -1px;
}

/* FLOATING ANIMATIONS */
.floating-shoe-3d {
    animation: floating-3d 5s ease-in-out infinite;
    filter: drop-shadow(0 30px 20px rgba(0,0,0,0.25));
}

@keyframes floating-3d {
    0% { transform: translateY(0) rotate(0deg); }
    50% { transform: translateY(-25px) rotate(-5deg); }
    100% { transform: translateY(0) rotate(0deg); }
}

.reflect-shoe {
    mask-image: linear-gradient(to bottom, black 80%, transparent 100%);
}

.h-line {
    width: 40px;
    height: 4px;
    background: var(--v-theme-primary);
    border-radius: 2px;
}

.bg-white-op-90 {
    background: rgba(255, 255, 255, 0.9) !important;
}

/* ORBS */
.orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    z-index: 1;
}
.o1 { width: 400px; height: 400px; background: #8ec5fc; top: -10%; left: -10%; opacity: 0.4; }
.o2 { width: 500px; height: 500px; background: #e0c3fc; bottom: -15%; right: -5%; opacity: 0.4; }
.o3 { width: 300px; height: 300px; background: #8ec5fc; top: 40%; right: 10%; opacity: 0.2; }

/* SHAPES */
.shape {
    position: absolute;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(5px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 20%;
}
.s1 { width: 100px; height: 100px; top: 10%; left: 10%; animation: rot 20s infinite linear; }
.s2 { width: 150px; height: 150px; bottom: 10%; right: 10%; animation: rot 25s infinite linear reverse; }
.s3 { width: 80px; height: 80px; top: 40%; right: 20%; animation: rot 15s infinite linear; }

@keyframes rot {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.premium-link {
    position: relative;
    padding-bottom: 2px;
}
.premium-link::after {
    content: '';
    position: absolute;
    width: 0;
    height: 2px;
    bottom: 0;
    left: 0;
    background: var(--v-theme-primary);
    transition: width 0.3s ease;
}
.premium-link:hover::after {
    width: 100%;
}
</style>
