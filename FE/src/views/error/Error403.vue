<template>
    <div class="error-container mesh-bg d-flex justify-center align-center text-center">
        <v-container>
            <div class="content-box animate-fade-in">
                <div class="lock-wrapper mb-8">
                    <div class="lock-icon">
                        <v-icon size="100" color="warning">mdi-lock-alert</v-icon>
                    </div>
                    <div class="shield-circle"></div>
                </div>

                <h1 class="text-h2 font-weight-black text-warning mb-4">Truy Cập Bị Từ Chối!</h1>
                <h2 class="text-h5 font-weight-bold text-slate-700 mb-8">Rất tiếc, bạn không có quyền truy cập vào trang này.</h2>

                <p class="text-body-1 text-slate-500 mb-10 max-width-400 mx-auto">
                    Khu vực này chỉ dành cho tài khoản có quyền hạn cao hơn. Nếu bạn tin rằng đây là một sự nhầm lẫn, vui lòng liên hệ quản
                    trị viên.
                </p>

                <div class="d-flex justify-center gap-4">
                    <v-btn
                        variant="flat"
                        size="x-large"
                        color="warning"
                        rounded="xl"
                        class="px-10 font-weight-bold elevation-12 action-btn text-white"
                        to="/"
                    >
                        Quay lại an toàn
                    </v-btn>
                </div>
            </div>
        </v-container>

        <!-- Decorative Orbs -->
        <div class="orb o1"></div>
    </div>
</template>

<script setup>
// Error 403 component
</script>

<style scoped>
.error-container {
    min-height: 100vh;
    background: #fffdfa;
    position: relative;
    overflow: hidden;
}

.mesh-bg {
    background-image: radial-gradient(at 100% 0%, rgba(245, 158, 11, 0.05) 0px, transparent 50%);
}

.content-box {
    max-width: 600px;
    margin: 0 auto;
    position: relative;
    z-index: 10;
}

.lock-wrapper {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
}

.shield-circle {
    position: absolute;
    width: 180px;
    height: 180px;
    border: 2px dashed rgba(245, 158, 11, 0.2);
    border-radius: 50%;
    animation: rotate 10s linear infinite;
}

@keyframes rotate {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}

.action-btn {
    transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.action-btn:hover {
    transform: scale(1.05);
}

.animate-fade-in {
    animation: fadeIn 0.8s ease-in-out;
}

@keyframes fadeIn {
    from {
        opacity: 0;
    }
    to {
        opacity: 1;
    }
}

.orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(120px);
    z-index: 1;
}

.o1 {
    width: 600px;
    height: 600px;
    background: rgba(245, 158, 11, 0.05);
    bottom: -200px;
    right: -100px;
}

.max-width-400 {
    max-width: 450px;
}
</style>
