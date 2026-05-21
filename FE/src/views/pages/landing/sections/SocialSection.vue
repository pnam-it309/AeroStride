<script setup>
import { ref, defineProps, computed } from 'vue';
const props = defineProps({ active: Boolean, warm: Boolean });

const promotions = [
    {
        phase: 'P00',
        label: 'PAST',
        title: 'FOUNDER EDITION',
        date: 'APRIL 2026',
        desc: 'Đã hoàn thành đợt phát hành nội bộ cho các nhà sáng lập. Các ưu đãi độc quyền đã được áp dụng cho 100 thành viên nòng cốt.',
        longDesc: 'Chương trình Founder Edition bao gồm việc khắc tên lên đế giày, thẻ thành viên NFT vĩnh viễn và quyền truy cập sớm vào tất cả các bộ sưu tập trong 2 năm tới.',
        status: 'COMPLETED',
        icon: 'mdi-check-decagram',
        color: '#64748b',
        specs: ['100 Slots', 'NFT Badge', 'Custom Engraving']
    },
    {
        phase: 'P01',
        label: 'NOW',
        title: 'EARLY BIRD ACCESS',
        date: '15.05 - 20.05',
        desc: 'Đang mở đăng ký. Giảm ngay 20% cho 500 khách hàng đầu tiên đặt trước phiên bản AeroStride X1.',
        longDesc: 'Cơ hội duy nhất để sở hữu X1 với mức giá ưu đãi nhất. Đi kèm là gói bảo hành mở rộng 24 tháng và bộ kit vệ sinh giày Carbon-Nano.',
        status: 'ACTIVE',
        icon: 'mdi-fire',
        color: '#2962FF',
        specs: ['500 Slots', '-20% Off', 'Extended Warranty']
    },
    {
        phase: 'P02',
        label: 'NEXT',
        title: 'GRAND LAUNCH EVENT',
        date: '25.05.2026',
        desc: 'Sắp diễn ra. Sự kiện ra mắt trực tuyến toàn cầu. Tặng kèm bộ vệ sinh giày chuyên dụng.',
        longDesc: 'Livestream ra mắt toàn cầu trên 5 nền tảng. Khách hàng tham gia có cơ hội nhận voucher may mắn trị giá lên tới 5.000.000 VNĐ.',
        status: 'UPCOMING',
        icon: 'mdi-rocket-launch',
        color: '#00E676',
        specs: ['Global Live', 'Exclusive Gifts', 'Lucky Draw']
    },
    {
        phase: 'P03',
        label: 'FUTURE',
        title: 'LOYALTY REWARDS',
        date: 'JUNE 2026',
        desc: 'Dự kiến kích hoạt hệ thống hoàn tiền 5% cho thành viên và quà tặng sinh nhật.',
        longDesc: 'Hệ thống tích điểm AeroPoints cho phép quy đổi voucher. Thành viên hạng Gold sẽ được miễn phí vận chuyển trọn đời.',
        status: 'PREPARING',
        icon: 'mdi-crown-outline',
        color: '#FFD600',
        specs: ['5% Cashback', 'AeroPoints', 'Birthday Gifts']
    },
    {
        phase: 'P04',
        label: 'LOCKED',
        title: 'SEASONAL COLLAB',
        date: 'Q3 2026',
        desc: 'Các phiên bản giới hạn kết hợp cùng các nghệ sĩ đương đại. Đang trong quá trình thiết kế.',
        longDesc: 'Hợp tác cùng 3 nghệ sĩ Graffiti nổi tiếng thế giới để tạo ra những bản phối màu độc bản không bao giờ tái sản xuất.',
        status: 'LOCKED',
        icon: 'mdi-palette-swatch-outline',
        color: '#FF1744',
        specs: ['Limited 500 prs', 'Artist Signature', 'Unique Box']
    }
];

const activeIndex = ref(1);
const showDetail = ref(false);
const containerRef = ref(null);

const selectPhase = (index) => {
    activeIndex.value = index;
    if (containerRef.value) {
        const itemWidth = 500; // approximation
        containerRef.value.scrollTo({
            left: index * itemWidth - (window.innerWidth / 2) + (itemWidth / 2),
            behavior: 'smooth'
        });
    }
};

const toggleDetail = () => {
    showDetail.value = !showDetail.value;
};

const currentPromo = computed(() => promotions[activeIndex.value]);

</script>

<template>
    <section class="snap-section promo-timeline-section bg-white overflow-hidden">
        <!-- Background HUD -->
        <div class="hud-bg-system">
            <div class="grid-lines"></div>
            <div class="parallax-title" :style="{ transform: `translateX(${-activeIndex * 50}px)` }">
                STRATEGIC ROADMAP 2026 // ASSET ANALYSIS
            </div>
        </div>

        <v-container fluid class="fill-height relative z-10 px-0 py-0 d-flex flex-column">
            
            <!-- Interactive Milestone Navigation (Bấm được) -->
            <div class="milestone-nav-top py-10 px-16">
                <div class="nav-track">
                    <div v-for="(promo, i) in promotions" :key="'nav-'+i" 
                         class="nav-node" 
                         :class="{ 'active': activeIndex === i, 'past': promo.status === 'COMPLETED' }"
                         @click="selectPhase(i)">
                        <div class="node-content">
                            <span class="p-code">{{ promo.phase }}</span>
                            <div class="node-dot" :style="{ background: promo.color }">
                                <div class="node-pulse" :style="{ background: promo.color }"></div>
                            </div>
                            <span class="p-label">{{ promo.label }}</span>
                        </div>
                        <div v-if="i < promotions.length - 1" class="node-line"></div>
                    </div>
                </div>
            </div>

            <!-- Horizontal Carousel (Chạy liên tục như slider) -->
            <div class="horizontal-slider-wrapper flex-grow-1">
                <div class="marquee-track">
                    <!-- Triple items for seamless loop -->
                    <div v-for="n in 3" :key="'group-'+n" class="marquee-group">
                        <div v-for="(promo, i) in promotions" :key="n+'-'+i" 
                             class="timeline-card-item" 
                             @click="showDetail = true; activeIndex = i">
                            
                            <div class="glass-promo-card">
                                <div class="card-top">
                                    <div class="icon-circle" :style="{ background: promo.color + '15' }">
                                        <v-icon :color="promo.color" size="40">{{ promo.icon }}</v-icon>
                                    </div>
                                    <div class="status-badge" :style="{ border: `1px solid ${promo.color}`, color: promo.color }">
                                        {{ promo.status }}
                                    </div>
                                </div>

                                <div class="card-mid">
                                    <h3 class="card-title">{{ promo.title }}</h3>
                                    <div class="card-date">{{ promo.date }}</div>
                                    <p class="card-desc">{{ promo.desc }}</p>
                                </div>

                                <div class="card-bot">
                                    <v-btn block rounded="pill" size="large" 
                                           :color="promo.color" variant="tonal" 
                                           class="font-weight-black">
                                        DATA ANALYSIS <v-icon class="ml-2">mdi-chart-timeline-variant</v-icon>
                                    </v-btn>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Detailed Analysis Overlay (Bấm được - Cảm nhận được) -->
            <transition name="sheet-up">
                <div v-if="showDetail" class="detail-sheet-overlay" @click.self="showDetail = false">
                    <div class="sheet-content">
                        <div class="sheet-header">
                            <div class="d-flex align-center">
                                <v-icon :color="currentPromo.color" size="32" class="mr-4">{{ currentPromo.icon }}</v-icon>
                                <div>
                                    <h4 class="text-h5 font-weight-black">{{ currentPromo.title }}</h4>
                                    <span class="text-caption font-weight-bold opacity-60">{{ currentPromo.phase }} // {{ currentPromo.status }}</span>
                                </div>
                            </div>
                            <v-btn icon="mdi-close" variant="text" @click="showDetail = false"></v-btn>
                        </div>

                        <div class="sheet-body py-8">
                            <v-row>
                                <v-col cols="12" md="7">
                                    <h5 class="section-label">OBJECTIVE</h5>
                                    <p class="long-desc">{{ currentPromo.longDesc }}</p>
                                    
                                    <div class="mt-8">
                                        <h5 class="section-label">TECHNICAL SPECIFICATIONS</h5>
                                        <div class="spec-grid">
                                            <div v-for="spec in currentPromo.specs" :key="spec" class="spec-tag">
                                                <v-icon size="14" class="mr-2">mdi-check-circle-outline</v-icon> {{ spec }}
                                            </div>
                                        </div>
                                    </div>
                                </v-col>
                                <v-col cols="12" md="5" class="d-flex align-center justify-center">
                                    <div class="analysis-visual">
                                        <div class="scanning-circle" :style="{ borderColor: currentPromo.color }"></div>
                                        <v-icon size="80" :color="currentPromo.color">mdi-database-eye-outline</v-icon>
                                    </div>
                                </v-col>
                            </v-row>
                        </div>

                        <div class="sheet-footer">
                            <v-btn block color="black" size="x-large" rounded="xl" class="font-weight-black">
                                ĐĂNG KÝ NHẬN THÔNG BÁO GIAI ĐOẠN NÀY
                            </v-btn>
                        </div>
                    </div>
                </div>
            </transition>

        </v-container>

        <!-- Bottom Status Bar -->
        <div class="system-status-bar">
            <div class="item">ROADMAP_v2.0</div>
            <div class="item">ACTIVE_PHASE: {{ currentPromo.phase }}</div>
            <div class="item">SYSTEM: READY</div>
            <div class="spacer"></div>
            <div class="item font-weight-black">AEROSTRIDE TECHNOLOGY CORP.</div>
        </div>
      </section>
</template>

<style scoped lang="scss">
.promo-timeline-section {
    height: 100vh; width: 100%; scroll-snap-align: start; scroll-snap-stop: always;
    position: relative; background: #ffffff; display: flex; flex-direction: column;
}

/* Background HUD */
.hud-bg-system {
    position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none;
    .grid-lines { position: absolute; inset: 0; background-image: radial-gradient(rgba(0,0,0,0.05) 1px, transparent 1px); background-size: 40px 40px; opacity: 0.5; }
    .parallax-title {
        position: absolute; bottom: 10vh; left: 5vw; font-size: 6vw; font-weight: 950; color: rgba(0,0,0,0.03); 
        white-space: nowrap; transition: transform 1s cubic-bezier(0.16, 1, 0.3, 1);
    }
}

/* Milestone Navigation (Interactive) */
.milestone-nav-top {
    width: 100%; z-index: 100;
    .nav-track { display: flex; align-items: center; justify-content: center; max-width: 1000px; margin: 0 auto; }
}

.nav-node {
    display: flex; align-items: center; cursor: pointer; position: relative;
    .node-content {
        display: flex; flex-direction: column; align-items: center; gap: 8px; transition: all 0.3s ease;
        .p-code { font-size: 0.7rem; font-weight: 900; color: #999; }
        .p-label { font-size: 0.6rem; font-weight: 900; color: #ccc; letter-spacing: 1px; }
        .node-dot {
            width: 14px; height: 14px; border-radius: 50%; position: relative;
            transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            .node-pulse { position: absolute; inset: 0; border-radius: 50%; opacity: 0; }
        }
    }
    
    .node-line { width: 100px; height: 1px; background: rgba(0,0,0,0.05); margin: 0 10px; transform: translateY(10px); }

    &:hover {
        .node-content { .node-dot { transform: scale(1.5); } .p-code { color: #000; } }
    }

    &.active {
        .node-content {
            .node-dot { transform: scale(2); .node-pulse { animation: pulse 2s infinite; opacity: 0.5; } }
            .p-code { color: #000; font-size: 0.8rem; }
            .p-label { color: #666; }
        }
    }
}

/* Continuous Slider (Marquee) */
.horizontal-slider-wrapper {
    width: 100%; overflow: hidden; position: relative;
    display: flex; align-items: center;
}

.marquee-track {
    display: flex; width: max-content;
    animation: scroll-roadmap 60s linear infinite;
    &:hover { animation-play-state: paused; }
}

.marquee-group { display: flex; gap: 40px; padding-right: 40px; }

.timeline-card-item {
    width: 420px; flex-shrink: 0; cursor: pointer; transition: all 0.4s ease;
    &:hover { transform: translateY(-10px) scale(1.02); z-index: 10; }
}

@keyframes scroll-roadmap {
    0% { transform: translateX(0); }
    100% { transform: translateX(-33.333%); }
}

.glass-promo-card {
    background: rgba(255, 255, 255, 0.8); backdrop-filter: blur(40px);
    border: 1px solid rgba(0,0,0,0.06); border-radius: 36px; padding: 40px;
    height: 520px; display: flex; flex-direction: column; justify-content: space-between;
    box-shadow: 0 20px 60px rgba(0,0,0,0.04);
    
    .card-top { display: flex; justify-content: space-between; align-items: center; }
    .icon-circle { width: 80px; height: 80px; border-radius: 24px; display: flex; align-items: center; justify-content: center; }
    .status-badge { padding: 4px 12px; border-radius: 100px; font-size: 0.65rem; font-weight: 900; }

    .card-mid {
        .card-title { font-size: 1.8rem; font-weight: 950; color: #000; line-height: 1.1; margin-bottom: 10px; }
        .card-date { font-size: 0.9rem; font-weight: 700; color: #999; margin-bottom: 20px; font-family: monospace; }
        .card-desc { font-size: 1rem; color: #555; line-height: 1.7; font-weight: 500; }
    }
}

/* Detail Sheet (Interactivity) */
.detail-sheet-overlay {
    position: absolute; inset: 0; background: rgba(0,0,0,0.1); backdrop-filter: blur(10px);
    z-index: 200; display: flex; align-items: flex-end; justify-content: center;
    .sheet-content {
        width: 100%; max-width: 900px; background: #fff; border-radius: 40px 40px 0 0;
        padding: 40px; box-shadow: 0 -40px 100px rgba(0,0,0,0.2);
    }
}

.section-label { font-size: 0.7rem; font-weight: 900; color: #999; letter-spacing: 2px; margin-bottom: 15px; }
.long-desc { font-size: 1.2rem; font-weight: 600; color: #1e293b; line-height: 1.6; }

.spec-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.spec-tag {
    background: #f8fafc; padding: 12px 20px; border-radius: 12px; font-size: 0.9rem; font-weight: 700;
    display: flex; align-items: center; color: #475569; border: 1px solid rgba(0,0,0,0.03);
}

.analysis-visual {
    position: relative; width: 200px; height: 200px; display: flex; align-items: center; justify-content: center;
    .scanning-circle {
        position: absolute; inset: 0; border: 2px solid; border-radius: 50%; border-style: dashed;
        animation: rotate 10s linear infinite; opacity: 0.3;
    }
}

/* Status Bar */
.system-status-bar {
    width: 100%; background: #000; color: #fff; display: flex; padding: 12px 40px; font-size: 0.65rem; letter-spacing: 1.5px;
    .item { margin-right: 30px; opacity: 0.6; }
    .spacer { flex: 1; }
}

/* Transitions */
.sheet-up-enter-active, .sheet-up-leave-active { transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1); }
.sheet-up-enter-from, .sheet-up-leave-to { transform: translateY(100%); }

@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes pulse { 0% { transform: scale(1); opacity: 0.5; } 100% { transform: scale(4); opacity: 0; } }

</style>
