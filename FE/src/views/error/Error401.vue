<template>
    <div class="error-container mesh-bg d-flex justify-center align-center text-center">
        <v-container>
            <div class="content-box animate-scale-up">
                <div class="image-wrapper mb-8">
                    <img src="@/assets/images/backgrounds/errorimg.svg" class="error-image" alt="401 - Unauthorized" />
                    <div class="glitch-effect">401</div>
                </div>

                <h1 class="text-h1 font-weight-black gradient-text mb-4">Truy cập bị từ chối!</h1>
                <h2 class="text-h4 font-weight-bold text-slate-700 mb-8">Bạn cần đăng nhập để truy cập vào nội dung này.</h2>

                <div class="d-flex justify-center ga-4">
                    <v-btn
                        variant="flat"
                        size="x-large"
                        color="primary"
                        rounded="xl"
                        class="px-8 font-weight-bold elevation-8 action-btn"
                        to="/login"
                    >
                        <v-icon start icon="mdi-login" class="mr-2"></v-icon>
                        Đăng nhập ngay
                    </v-btn>

                    <v-btn variant="outlined" size="x-large" color="primary" rounded="xl" class="px-8 font-weight-bold action-btn" to="/">
                        Về trang chủ
                    </v-btn>
                </div>
            </div>
        </v-container>

        <!-- Decorative Orbs -->
        <div class="orb o1"></div>
        <div class="orb o2"></div>
    </div>
</template>

<script setup>
// Error 401 component
</script>

<style scoped>
.error-container {
    min-height: 100vh;
    background: #f8fafc;
    position: relative;
    overflow: hidden;
}

.mesh-bg {
    background-image:
        radial-gradient(at 0% 0%, rgba(30, 58, 138, 0.05) 0px, transparent 50%),
        radial-gradient(at 100% 100%, rgba(16, 185, 129, 0.05) 0px, transparent 50%);
}

.content-box {
    max-width: 700px;
    margin: 0 auto;
    position: relative;
    z-index: 10;
}

.error-image {
    width: 100%;
    max-width: 500px;
    filter: drop-shadow(0 20px 30px rgba(0, 0, 0, 0.1));
}

.image-wrapper {
    position: relative;
}

.glitch-effect {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    font-size: 120px;
    font-weight: 900;
    color: rgba(30, 58, 138, 0.1);
    letter-spacing: 10px;
    z-index: -1;
}

.gradient-text {
    background: linear-gradient(135deg, #1e3a8a 0%, #10b981 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.action-btn {
    transition: all 0.3s ease;
}

.action-btn:hover {
    transform: translateY(-5px);
}

.animate-scale-up {
    animation: scaleUp 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes scaleUp {
    from {
        opacity: 0;
        transform: scale(0.8);
    }
    to {
        opacity: 1;
        transform: scale(1);
    }
}

.orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    z-index: 1;
}

.o1 {
    width: 400px;
    height: 400px;
    background: rgba(30, 58, 138, 0.1);
    top: -100px;
    right: -100px;
}

.o2 {
    width: 300px;
    height: 300px;
    background: rgba(16, 185, 129, 0.1);
    bottom: -50px;
    left: -50px;
}

@media (max-width: 600px) {
    .text-h1 {
        font-size: 3rem !important;
    }
    .text-h4 {
        font-size: 1.25rem !important;
    }
    .error-image {
        max-width: 300px;
    }
}
</style>
