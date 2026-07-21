<template>
    <div class="error-container mesh-bg d-flex justify-center align-center text-center">
        <v-container>
            <div class="content-box animate-slide-up">
                <div class="icon-wrapper mb-8">
                    <v-icon size="150" color="error" class="mb-4">mdi-server-off</v-icon>
                    <div class="error-code">500</div>
                </div>

                <h1 class="text-h2 font-weight-black text-error mb-4">Lỗi Hệ Thống!</h1>
                <h2 class="text-h5 font-weight-bold text-slate-700 mb-8">
                    Đã có lỗi xảy ra phía máy chủ. Chúng tôi đang nỗ lực khắc phục sự cố này.
                </h2>

                <div class="info-card mb-8 pa-4 rounded-lg bg-error-lighten-5 border-error">
                    <p class="text-body-1 text-error font-weight-medium mb-0">
                        Vui lòng thử lại sau vài phút hoặc liên hệ bộ phận hỗ trợ kỹ thuật nếu vấn đề vẫn tiếp diễn.
                    </p>
                </div>

                <div class="d-flex justify-center ga-4">
                    <v-btn
                        variant="flat"
                        size="x-large"
                        color="error"
                        rounded="xl"
                        class="px-8 font-weight-bold elevation-8 action-btn"
                        @click="reloadPage"
                    >
                        <v-icon start icon="mdi-refresh" class="mr-2"></v-icon>
                        Thử lại ngay
                    </v-btn>

                    <v-btn variant="text" size="x-large" color="slate-600" rounded="xl" class="px-8 font-weight-bold action-btn" to="/">
                        Về trang chủ
                    </v-btn>
                </div>
            </div>
        </v-container>

        <!-- Decorative Orbs -->
        <div class="orb o1"></div>
    </div>
</template>

<script setup>
const reloadPage = () => {
    window.location.reload();
};
</script>

<style scoped>
.error-container {
    min-height: 100vh;
    background: #fffafa;
    position: relative;
    overflow: hidden;
}

.mesh-bg {
    background-image: radial-gradient(at 0% 0%, rgba(220, 38, 38, 0.05) 0px, transparent 50%);
}

.content-box {
    max-width: 600px;
    margin: 0 auto;
    position: relative;
    z-index: 10;
}

.error-code {
    font-size: 100px;
    font-weight: 900;
    color: rgba(220, 38, 38, 0.1);
    line-height: 1;
    margin-top: -40px;
}

.action-btn {
    transition: all 0.3s ease;
}

.action-btn:hover {
    transform: translateY(-3px);
}

.animate-slide-up {
    animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
    from {
        opacity: 0;
        transform: translateY(30px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(100px);
    z-index: 1;
}

.o1 {
    width: 500px;
    height: 500px;
    background: rgba(220, 38, 38, 0.05);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
}

.bg-error-lighten-5 {
    background-color: #fef2f2;
}

.border-error {
    border: 1px dashed #fee2e2;
}
</style>
