import { computed, ref } from 'vue';

export function useSelectableList({ items, visibleItems, getKey = (item) => item?.id }) {
    const selectedKeys = ref([]);

    const visibleKeys = computed(() => visibleItems.value.map((item) => getKey(item)).filter(Boolean));
    const selectedItems = computed(() => items.value.filter((item) => selectedKeys.value.includes(getKey(item))));
    const allVisibleSelected = computed(
        () => visibleKeys.value.length > 0 && visibleKeys.value.every((key) => selectedKeys.value.includes(key))
    );
    const someVisibleSelected = computed(
        () => visibleKeys.value.some((key) => selectedKeys.value.includes(key)) && !allVisibleSelected.value
    );

    const clearSelection = () => {
        selectedKeys.value = [];
    };

    const syncSelection = () => {
        const validKeys = new Set(items.value.map((item) => getKey(item)).filter(Boolean));
        selectedKeys.value = selectedKeys.value.filter((key) => validKeys.has(key));
    };

    const toggleSelection = (key, checked) => {
        if (checked) {
            if (!selectedKeys.value.includes(key)) {
                selectedKeys.value = [...selectedKeys.value, key];
            }
            return;
        }

        selectedKeys.value = selectedKeys.value.filter((selectedKey) => selectedKey !== key);
    };

    const toggleVisibleSelection = (checked) => {
        if (checked) {
            selectedKeys.value = Array.from(new Set([...selectedKeys.value, ...visibleKeys.value]));
            return;
        }

        const visibleKeySet = new Set(visibleKeys.value);
        selectedKeys.value = selectedKeys.value.filter((key) => !visibleKeySet.has(key));
    };

    return {
        selectedKeys,
        visibleKeys,
        selectedItems,
        allVisibleSelected,
        someVisibleSelected,
        clearSelection,
        syncSelection,
        toggleSelection,
        toggleVisibleSelection
    };
}
