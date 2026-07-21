import { computed, onMounted, ref, watch } from 'vue';
import { dichVuLanding } from '@/services/public/dichVuLanding';

export function useLandingCatalog(activeSectionRef) {
    const landingProducts = ref([]);
    const featuredVariants = ref([]);
    const topVariantsByQty = ref([]);
    const isCatalogLoading = ref(true);

    const loadInitialCatalog = async () => {
        isCatalogLoading.value = true;
        try {
            landingProducts.value = await dichVuLanding.laySanPhamNoiBat(6);
        } catch (error) {
            landingProducts.value = [];
            if (import.meta.env.DEV) {
                console.warn('Failed to load initial landing catalog:', error);
            }
        } finally {
            isCatalogLoading.value = false;
        }
    };

    const loadSecondaryCatalog = async () => {
        if (topVariantsByQty.value.length > 0 || featuredVariants.value.length > 0) return;

        try {
            const [variants, topVariants] = await Promise.all([
                dichVuLanding.layBienTheNoiBat(12),
                dichVuLanding.layTopBienTheTheoSoLuong(5)
            ]);
            featuredVariants.value = variants;
            topVariantsByQty.value = topVariants;
        } catch (error) {
            if (import.meta.env.DEV) {
                console.warn('Failed to load secondary landing catalog:', error);
            }
        }
    };

    onMounted(loadInitialCatalog);

    if (activeSectionRef) {
        watch(activeSectionRef, (sectionIndex) => {
            if (sectionIndex >= 1) {
                loadSecondaryCatalog();
            }
        }, { immediate: true });
    }

    const heroProduct = computed(() => landingProducts.value[0] || null);
    const problemProducts = computed(() => landingProducts.value.slice(0, 3));
    const howProducts = computed(() => landingProducts.value.slice(0, 3));

    return {
        featuredVariants,
        heroProduct,
        howProducts,
        isCatalogLoading,
        landingProducts,
        topVariantsByQty,
        problemProducts,
        reloadLandingCatalog: loadInitialCatalog
    };
}
