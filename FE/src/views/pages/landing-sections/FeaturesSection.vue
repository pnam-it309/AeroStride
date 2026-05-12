<script setup>
import { defineProps } from 'vue';
import { LANDING_FEATURE_COLUMNS } from '@/constants/landingFeatures';

const props = defineProps({ active: Boolean, warm: Boolean });
</script>

<template>
    <section class="snap-section features-section overflow-hidden" :class="{ 'is-active': props.active }">
        <div class="light-decor">
            <div class="soft-glow glow-1"></div>
            <div class="soft-glow glow-2"></div>
            <div class="soft-glow glow-3"></div>
        </div>

        <div class="diagonal-bento-wall">
            <div
                v-for="(column, columnIndex) in LANDING_FEATURE_COLUMNS"
                :key="`col-${columnIndex}`"
                class="bento-col"
                :class="`col-${columnIndex + 1}`"
            >
                <div v-for="(item, itemIndex) in column" :key="`c${columnIndex}-${itemIndex}`" class="bento-card glass-card" :class="item.size">
                    <div class="card-inner">
                        <div class="icon-wrap" :style="{ background: `${item.color}15` }">
                            <v-icon :color="item.color" size="24">{{ item.icon }}</v-icon>
                        </div>
                        <div class="text-wrap">
                            <h4>{{ item.title }}</h4>
                            <p>{{ item.desc }}</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>

<style scoped lang="scss">
.features-section {
    height: 100vh;
    width: 100%;
    scroll-snap-align: start;
    scroll-snap-stop: always;
    position: relative;
    background: #ffffff;
    display: flex;
    overflow: hidden;
}

.light-decor {
    position: absolute;
    inset: 0;
    z-index: 1;

    .soft-glow {
        position: absolute;
        border-radius: 50%;
        filter: blur(120px);
        opacity: 0.12;
    }

    .glow-1 { width: 900px; height: 900px; background: #2962FF; top: -20%; left: -10%; }
    .glow-2 { width: 700px; height: 700px; background: #00E676; bottom: -10%; right: -10%; }
    .glow-3 { width: 520px; height: 520px; background: #6200EA; top: 35%; right: 20%; }
}

.diagonal-bento-wall {
    display: flex;
    gap: 40px;
    position: absolute;
    width: 320vw;
    height: 320vh;
    top: -110vh;
    left: -110vw;
    transform: rotate(-12deg) skewX(-2deg);
    transform-style: preserve-3d;
    z-index: 5;
    will-change: transform;
    justify-content: center;
}

.bento-col {
    display: flex;
    flex-direction: column;
    gap: 40px;
    width: 340px;
    flex-shrink: 0;
    will-change: transform;
    backface-visibility: hidden;
    transform-style: preserve-3d;
}

.col-1 { animation: scroll-up-loop 35s linear infinite running; }
.col-2 { animation: scroll-down-loop 40s linear infinite running; }
.col-3 { animation: scroll-up-loop 45s linear infinite running; }
.col-4 { animation: scroll-down-loop 38s linear infinite running; }
.col-5 { animation: scroll-up-loop 42s linear infinite running; }
.col-6 { animation: scroll-down-loop 32s linear infinite running; }

@keyframes scroll-up-loop {
    0% { transform: translate3d(0, 0, 0); }
    100% { transform: translate3d(0, -33.33%, 0); }
}

@keyframes scroll-down-loop {
    0% { transform: translate3d(0, -33.33%, 0); }
    100% { transform: translate3d(0, 0, 0); }
}

.bento-card {
    background: rgba(255, 255, 255, 0.72);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.58);
    border-radius: 28px;
    padding: 30px;
    transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.35s cubic-bezier(0.16, 1, 0.3, 1), background 0.35s cubic-bezier(0.16, 1, 0.3, 1);
    box-shadow: 0 8px 26px rgba(0, 0, 0, 0.04);
    cursor: pointer;
    will-change: transform;

    &.s { min-height: 180px; }
    &.m { min-height: 250px; }
    &.l { min-height: 320px; }

    &:hover {
        transform: scale(1.08) rotate(6deg) translateY(-10px);
        background: rgba(255, 255, 255, 0.96);
        box-shadow: 0 24px 50px rgba(0, 0, 0, 0.1);
        border-color: rgba(41, 98, 255, 0.25);

        .icon-wrap { transform: scale(1.15) rotate(-8deg); }
        h4 { color: #2962FF; }
    }
}

.card-inner {
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.icon-wrap {
    width: 56px;
    height: 56px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.3s ease;
}

.text-wrap {
    h4 {
        font-size: 1.05rem;
        font-weight: 950;
        color: #1e293b;
        margin-bottom: 8px;
        letter-spacing: -0.3px;
    }

    p {
        font-size: 0.9rem;
        color: #64748b;
        line-height: 1.55;
        font-weight: 500;
    }
}

.features-section:not(.is-active) {
    .bento-card:hover {
        transform: none !important;
        box-shadow: inherit !important;
    }
}

</style>
