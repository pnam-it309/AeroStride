import { reactive, ref, computed, watch } from 'vue';

/**
 * Quản lý VIEW của bảng biến thể trong form sản phẩm: bộ lọc, khoảng giá, phân trang, lựa chọn.
 * Tách nguyên trạng từ SanPhamForm.vue (behavior-preserving) để giảm bulk.
 *
 * Nguồn dữ liệu `variantItems` do component sở hữu và truyền vào (composable không sinh biến thể).
 * Các helper tra cứu nhãn/khóa được truyền vào để tránh phụ thuộc ngược.
 *
 * @param {import('vue').Ref<any[]>} variantItems
 * @param {{
 *   getVariantColorLabel: (id: any) => string,
 *   getVariantSizeLabel: (id: any) => string,
 *   getVariantKey: (variant: any) => any,
 *   minPrice?: number,
 *   maxPrice?: number
 * }} deps
 */
export function useVariantTable(variantItems, {
    getVariantColorLabel,
    getVariantSizeLabel,
    getVariantKey,
    minPrice = 0,
    maxPrice = 100000000
}) {
    const variantTableFilters = reactive({
        keyword: '',
        mauSacId: '',
        kichThuocId: '',
        trangThai: '',
        khoangGia: [minPrice, maxPrice]
    });
    const variantPriceBounds = ref({ min: minPrice, max: maxPrice });
    const variantPage = ref(1);
    const variantPageSize = ref(5);
    const selectedVariantKeys = ref([]);

    const variantPriceStep = computed(() => (variantPriceBounds.value.max > 1000000 ? 50000 : 1000));

    // Danh sách biến thể sau khi áp dụng tìm kiếm, màu, size, trạng thái và khoảng giá.
    const filteredVariantItems = computed(() => variantItems.value.filter((item) => {
        const keyword = variantTableFilters.keyword.trim().toLowerCase();
        const colorLabel = getVariantColorLabel(item.idMauSac).toLowerCase();
        const sizeLabel = getVariantSizeLabel(item.idKichThuoc).toLowerCase();
        const matchesKeyword = !keyword
            || String(item.maChiTietSanPham || '').toLowerCase().includes(keyword)
            || colorLabel.includes(keyword)
            || sizeLabel.includes(keyword);
        const variantPrice = Number(item.giaBan ?? 0);
        const matchesPrice = variantPrice >= variantTableFilters.khoangGia[0]
            && variantPrice <= variantTableFilters.khoangGia[1];

        return matchesKeyword
            && (!variantTableFilters.mauSacId || item.idMauSac === variantTableFilters.mauSacId)
            && (!variantTableFilters.kichThuocId || item.idKichThuoc === variantTableFilters.kichThuocId)
            && (!variantTableFilters.trangThai || item.trangThai === variantTableFilters.trangThai)
            && matchesPrice;
    }));
    const totalVariantElements = computed(() => filteredVariantItems.value.length);
    const totalVariantPages = computed(() => Math.max(Math.ceil(totalVariantElements.value / variantPageSize.value), 1));
    const paginatedVariantItems = computed(() => {
        const start = (variantPage.value - 1) * variantPageSize.value;
        return filteredVariantItems.value.slice(start, start + variantPageSize.value);
    });
    const visibleVariantKeys = computed(() => paginatedVariantItems.value.map((item) => getVariantKey(item)));
    const selectedVariants = computed(() => filteredVariantItems.value.filter((item) => selectedVariantKeys.value.includes(getVariantKey(item))));
    const allVisibleVariantsSelected = computed(() => visibleVariantKeys.value.length > 0
        && visibleVariantKeys.value.every((key) => selectedVariantKeys.value.includes(key)));
    const someVisibleVariantsSelected = computed(() => visibleVariantKeys.value.some((key) => selectedVariantKeys.value.includes(key))
        && !allVisibleVariantsSelected.value);

    const updateVariantPriceBounds = () => {
        const highestPrice = variantItems.value.reduce((maxValue, item) => Math.max(maxValue, Number(item.giaBan ?? 0)), minPrice);
        const safeMax = Math.max(highestPrice, variantPriceStep.value);
        variantPriceBounds.value = { min: minPrice, max: safeMax };

        variantTableFilters.khoangGia = [
            Math.min(variantTableFilters.khoangGia[0], safeMax),
            Math.min(variantTableFilters.khoangGia[1], safeMax)
        ];

        if (variantTableFilters.khoangGia[0] > variantTableFilters.khoangGia[1]) {
            variantTableFilters.khoangGia = [minPrice, safeMax];
        }
    };

    const sanitizeVariantPriceRange = (range, maxBound = variantPriceBounds.value.max) => {
        const safeMaxPrice = Math.max(minPrice, Number(maxBound || maxPrice));
        const rawMin = Math.max(minPrice, Number(range?.[0] ?? minPrice));
        const rawMax = Math.max(minPrice, Number(range?.[1] ?? safeMaxPrice));
        const nextMin = Math.min(rawMin, safeMaxPrice);
        const nextMax = Math.min(Math.max(rawMax, nextMin), safeMaxPrice);
        return [nextMin, nextMax];
    };

    const updateVariantPriceBoundary = (boundary, value) => {
        const nextRange = [...variantTableFilters.khoangGia];
        if (boundary === 'min') {
            nextRange[0] = value === '' ? minPrice : value;
        } else {
            nextRange[1] = value === '' ? variantPriceBounds.value.max : value;
        }
        variantTableFilters.khoangGia = sanitizeVariantPriceRange(nextRange);
    };

    const handleVariantSliderPriceChange = (value) => {
        variantTableFilters.khoangGia = sanitizeVariantPriceRange(value);
    };

    const clearVariantSelection = () => {
        selectedVariantKeys.value = [];
    };

    const syncVariantSelection = () => {
        const validKeys = new Set(filteredVariantItems.value.map((item) => getVariantKey(item)));
        selectedVariantKeys.value = selectedVariantKeys.value.filter((key) => validKeys.has(key));
    };

    const toggleVariantSelection = (variantKey, checked) => {
        if (checked) {
            if (!selectedVariantKeys.value.includes(variantKey)) {
                selectedVariantKeys.value = [...selectedVariantKeys.value, variantKey];
            }
            return;
        }
        selectedVariantKeys.value = selectedVariantKeys.value.filter((key) => key !== variantKey);
    };

    const toggleSelectVisibleVariants = (checked) => {
        if (checked) {
            const mergedKeys = new Set([...selectedVariantKeys.value, ...visibleVariantKeys.value]);
            selectedVariantKeys.value = Array.from(mergedKeys);
            return;
        }
        const visibleKeySet = new Set(visibleVariantKeys.value);
        selectedVariantKeys.value = selectedVariantKeys.value.filter((key) => !visibleKeySet.has(key));
    };

    const resetVariantTableFilters = () => {
        variantTableFilters.keyword = '';
        variantTableFilters.mauSacId = '';
        variantTableFilters.kichThuocId = '';
        variantTableFilters.trangThai = '';
        variantTableFilters.khoangGia = [minPrice, variantPriceBounds.value.max];
        variantPage.value = 1;
        clearVariantSelection();
    };

    watch(variantItems, () => {
        updateVariantPriceBounds();
        syncVariantSelection();
        if (variantPage.value > totalVariantPages.value) {
            variantPage.value = totalVariantPages.value;
        }
    }, { deep: true });

    watch(variantPageSize, () => {
        variantPage.value = 1;
    });

    watch(
        () => [
            variantTableFilters.keyword,
            variantTableFilters.mauSacId,
            variantTableFilters.kichThuocId,
            variantTableFilters.trangThai,
            variantTableFilters.khoangGia[0],
            variantTableFilters.khoangGia[1]
        ],
        () => {
            variantPage.value = 1;
            syncVariantSelection();
        }
    );

    return {
        variantTableFilters,
        variantPriceBounds,
        variantPage,
        variantPageSize,
        selectedVariantKeys,
        variantPriceStep,
        filteredVariantItems,
        totalVariantElements,
        totalVariantPages,
        paginatedVariantItems,
        visibleVariantKeys,
        selectedVariants,
        allVisibleVariantsSelected,
        someVisibleVariantsSelected,
        updateVariantPriceBounds,
        sanitizeVariantPriceRange,
        updateVariantPriceBoundary,
        handleVariantSliderPriceChange,
        clearVariantSelection,
        syncVariantSelection,
        toggleVariantSelection,
        toggleSelectVisibleVariants,
        resetVariantTableFilters
    };
}
