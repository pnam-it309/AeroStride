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
@import '@/scss/pages/_auth-layout.scss';
</style>
