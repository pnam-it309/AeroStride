import { computed, onMounted, ref } from 'vue';
import { dichVuLanding } from '@/services/public/dichVuLanding';

export function useLandingCatalog() {
    const landingProducts = ref([]);
    const featuredVariants = ref([]);
    const isCatalogLoading = ref(true);

    const loadLandingCatalog = async () => {
        isCatalogLoading.value = true;

        try {
            const [products, variants] = await Promise.all([
                dichVuLanding.laySanPhamNoiBat(6),
                dichVuLanding.layBienTheNoiBat(12)
            ]);
            landingProducts.value = products;
            featuredVariants.value = variants;
        } catch (error) {
            landingProducts.value = [];
            featuredVariants.value = [];
            if (import.meta.env.DEV) {
                console.warn('Failed to load landing catalog:', error);
            }
        } finally {
            isCatalogLoading.value = false;
        }
    };

    onMounted(loadLandingCatalog);

    const heroProduct = computed(() => landingProducts.value[0] || null);
    const problemProducts = computed(() => landingProducts.value.slice(0, 3));
    const howProducts = computed(() => landingProducts.value.slice(0, 3));

    return {
        featuredVariants,
        heroProduct,
        howProducts,
        isCatalogLoading,
        landingProducts,
        problemProducts,
        reloadLandingCatalog: loadLandingCatalog
    };
}
