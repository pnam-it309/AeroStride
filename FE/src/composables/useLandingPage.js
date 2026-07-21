import { ref, onMounted, onUnmounted } from 'vue';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { LANDING_MOUSE_OFFSET, LANDING_SECTIONS } from '@/constants/landingPage';

let hasLoadedBefore = false;

export function useLandingPage() {
    const isLoggedIn = ref(false);
    const isLoading = ref(!hasLoadedBefore);
    const activeSection = ref(0);
    const mouseX = ref(0);
    const mouseY = ref(0);

    let mouseFrame = 0;
    let scrollFrame = 0;
    let latestMouseEvent = null;
    let latestScrollTop = 0;

    const updateMousePosition = () => {
        mouseFrame = 0;

        if (!latestMouseEvent || activeSection.value !== 0) {
            return;
        }

        const { clientX, clientY } = latestMouseEvent;
        mouseX.value = (clientX / window.innerWidth - 0.5) * LANDING_MOUSE_OFFSET;
        mouseY.value = (clientY / window.innerHeight - 0.5) * LANDING_MOUSE_OFFSET;
    };

    const handleMouseMove = (e) => {
        if (activeSection.value !== 0) {
            return;
        }

        latestMouseEvent = e;

        if (!mouseFrame) {
            mouseFrame = window.requestAnimationFrame(updateMousePosition);
        }
    };

    const onScroll = (e) => {
        latestScrollTop = e.target ? e.target.scrollTop : window.scrollY;

        if (scrollFrame) {
            return;
        }

        scrollFrame = window.requestAnimationFrame(() => {
            scrollFrame = 0;

            const height = window.innerHeight;
            const index = Math.round(latestScrollTop / height);

            if (activeSection.value !== index) {
                activeSection.value = index;
            }
        });
    };

    onMounted(() => {
        // Scroll is handled by the container @scroll event, do not bind to window
    });

    onUnmounted(() => {
        // No window scroll event to remove
    });

    const warmedSections = ref(new Set([0, 1, 2]));

    const handleLogout = async () => {
        await dichVuXacThuc.dangXuat();
        isLoggedIn.value = false;
    };

    const handlePreloaderFinish = () => {
        isLoading.value = false;
        hasLoadedBefore = true;
        setTimeout(() => {
            for (let i = 0; i <= LANDING_SECTIONS.length; i++) {
                warmedSections.value.add(i);
            }
        }, 500);
    };

    const isSectionWarm = (index) => {
        if (Math.abs(activeSection.value - index) <= 2 || warmedSections.value.has(index)) {
            warmedSections.value.add(index);
            return true;
        }
        return false;
    };

    onMounted(() => {
        window.addEventListener('mousemove', handleMouseMove, { passive: true });
        isLoggedIn.value = dichVuXacThuc.daDangNhap();
        if (hasLoadedBefore) {
            for (let i = 0; i <= LANDING_SECTIONS.length; i++) {
                warmedSections.value.add(i);
            }
        }
    });

    onUnmounted(() => {
        window.removeEventListener('mousemove', handleMouseMove);

        if (mouseFrame) {
            window.cancelAnimationFrame(mouseFrame);
        }

        if (scrollFrame) {
            window.cancelAnimationFrame(scrollFrame);
        }
    });

    return {
        activeSection,
        handleLogout,
        handlePreloaderFinish,
        isLoading,
        isLoggedIn,
        LANDING_SECTIONS,
        isSectionWarm,
        mouseX,
        mouseY,
        onScroll
    };
}
