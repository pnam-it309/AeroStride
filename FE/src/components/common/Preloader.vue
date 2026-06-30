<script setup>
import { ref, onMounted } from 'vue';

const emit = defineEmits(['finish']);
const loadingProgress = ref(0);
const isFinished = ref(false);

onMounted(() => {
    const interval = setInterval(() => {
        if (loadingProgress.value < 100) {
            // Speed up the loading progress simulation
            loadingProgress.value += Math.floor(Math.random() * 15) + 5;
            if (loadingProgress.value > 100) loadingProgress.value = 100;
        } else {
            clearInterval(interval);
            // Kích hoạt hiệu ứng lan sang phải
            isFinished.value = true;
            
            // Đợi hiệu ứng CSS chạy xong (khoảng 800ms) rồi mới báo kết thúc
            setTimeout(() => {
                emit('finish');
            }, 800);
        }
    }, 50); // Faster check interval
});
</script>

<template>
    <div class="preloader">
        <div class="preloader-inner">
            <!-- Background Elements -->
            <div class="blue-gradient-bg"></div>
            <div class="topo-pattern"></div>

            <!-- Vertical Progress Bar (Full height left, wipes right on finish) -->
            <div class="progress-container" :class="{ 'wipe-right': isFinished }">
                <div class="progress-bar-vertical">
                    <div class="progress-fill" :style="{ height: loadingProgress + '%' }"></div>
                </div>
            </div>

            <!-- Center Content -->
            <div class="content-box">
                <div class="logo-wrapper">
                    <h1 class="brand-name">AEROSTRIDE</h1>
                    <div class="version">GIAO DIỆN CAO CẤP</div>
                </div>
                <div class="status-text"><span class="pulse">●</span> ĐANG TẢI DỮ LIỆU...</div>
            </div>

            <!-- Bottom Percentage -->
            <div class="stats-box">
                <div class="percent-container">
                    <span class="num">{{ loadingProgress }}</span>
                    <span class="unit">%</span>
                </div>
                <div class="meta-info">HỆ THỐNG HOẠT ĐỘNG / SẴN SÀNG</div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.preloader {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: #ffffff; /* Theme: White & Blue */
    z-index: 9999;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: 'Space Grotesk', sans-serif;
    overflow: hidden;
}

.blue-gradient-bg {
    position: absolute;
    width: 150%;
    height: 150%;
    background: radial-gradient(circle at center, rgba(41, 98, 255, 0.05) 0%, transparent 70%);
    animation: drift 15s linear infinite;
}

@keyframes drift {
    0% {
        transform: rotate(0deg);
    }
    100% {
        transform: rotate(360deg);
    }
}

.topo-pattern {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-image: url('https://www.transparenttextures.com/patterns/topography.png');
    opacity: 0.03;
    filter: invert(1);
}

.progress-container {
    position: absolute;
    left: 0;
    top: 0;
    height: 100%;
    width: 8px; /* Độ rộng ban đầu của thanh bên trái */
    background: transparent;
    z-index: 50; /* Nổi lên trên cùng */
    transition: width 0.8s cubic-bezier(0.8, 0, 0.2, 1);
}

/* Khi tải xong 100%, class wipe-right kích hoạt làm thanh lan đầy màn hình */
.progress-container.wipe-right {
    width: 100vw;
}

.progress-bar-vertical {
    position: relative;
    width: 100%;
    height: 100%;
    background: rgba(41, 98, 255, 0.05); /* Màu nền mờ của track */
}

.progress-fill {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%; /* Luôn lấy đầy chiều rộng của container */
    background: #2962ff; /* Màu xanh chủ đạo */
    box-shadow: 0 0 15px rgba(41, 98, 255, 0.5);
    transition: height 0.1s ease;
}

.content-box {
    text-align: center;
    z-index: 10;
}

.brand-name {
    font-size: 5rem;
    font-weight: 950;
    color: #1e293b;
    letter-spacing: 20px;
    margin: 0;
    line-height: 1;
}

.version {
    font-size: 0.8rem;
    letter-spacing: 6px;
    color: #2962ff;
    margin-top: 15px;
    font-weight: 700;
}

.status-text {
    margin-top: 40px;
    font-size: 0.9rem;
    letter-spacing: 3px;
    color: #64748b;

    .pulse {
        color: #2962ff;
        animation: blink 1s infinite;
    }
}

@keyframes blink {
    0%,
    100% {
        opacity: 1;
    }
    50% {
        opacity: 0.3;
    }
}

.stats-box {
    position: absolute;
    bottom: 60px;
    right: 60px;
    text-align: right;
    z-index: 10;
}

.percent-container {
    color: #1e293b;
    font-weight: 900;
    line-height: 1;

    .num {
        font-size: 6rem;
    }
    .unit {
        font-size: 2rem;
        color: #2962ff;
        margin-left: 5px;
    }
}

.meta-info {
    font-size: 0.7rem;
    letter-spacing: 2px;
    color: #94a3b8;
    margin-top: 10px;
}

@media (max-width: 768px) {
    .brand-name {
        font-size: 2.5rem;
        letter-spacing: 10px;
    }
    .stats-box {
        bottom: 30px;
        right: 30px;
    }
    .percent-container .num {
        font-size: 3rem;
    }
}
</style>
