import { ref, onMounted, onUnmounted } from 'vue';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { LANDING_MOUSE_OFFSET, LANDING_SECTIONS } from '@/constants/landingPage';

export function useLandingPage() {
    const isLoggedIn = ref(false);
    const isLoading = ref(true);
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
        latestScrollTop = e.target.scrollTop;

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

    const handleLogout = async () => {
        await dichVuXacThuc.dangXuat();
        isLoggedIn.value = false;
    };

    const handlePreloaderFinish = () => {
        isLoading.value = false;
    };

    const isSectionWarm = (index) => Math.abs(activeSection.value - index) <= 1;

    onMounted(() => {
        window.addEventListener('mousemove', handleMouseMove, { passive: true });
        isLoggedIn.value = dichVuXacThuc.daDangNhap();
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
