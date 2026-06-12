import { useHead, useSeoMeta as useUnheadSeoMeta } from '@unhead/vue';
import { ref, computed } from 'vue';

const DEFAULTS = {
    siteName: 'AeroStride',
    titleSuffix: ' | AeroStride',
    description: 'AeroStride - Cửa hàng giày thể thao chính hãng. Mua sắm giày Nike, Adidas, Puma với giá tốt nhất, giao hàng toàn quốc.',
    image: '/favicon.png',
    type: 'website',
    locale: 'vi_VN',
    twitterCard: 'summary_large_image'
};

export function useSeoMeta() {
    const title = ref(DEFAULTS.siteName);
    const description = ref(DEFAULTS.description);
    const image = ref(DEFAULTS.image);
    const url = ref(typeof window !== 'undefined' ? window.location.href : '');
    const type = ref(DEFAULTS.type);
    const jsonLd = ref(null);

    const fullTitle = computed(() => title.value !== DEFAULTS.siteName ? `${title.value}${DEFAULTS.titleSuffix}` : DEFAULTS.siteName);

    // Call useUnheadSeoMeta and useHead synchronously during setup!
    useUnheadSeoMeta({
        title: fullTitle,
        description: description,
        ogTitle: fullTitle,
        ogDescription: description,
        ogImage: image,
        ogUrl: url,
        ogType: type,
        ogSiteName: DEFAULTS.siteName,
        ogLocale: DEFAULTS.locale,
        twitterCard: DEFAULTS.twitterCard,
        twitterTitle: fullTitle,
        twitterDescription: description,
        twitterImage: image
    });

    useHead({
        link: computed(() => [
            {
                rel: 'canonical',
                href: url.value
            }
        ]),
        script: computed(() => jsonLd.value ? [
            {
                type: 'application/ld+json',
                innerHTML: JSON.stringify(jsonLd.value)
            }
        ] : [])
    });

    function setSeoMeta(config = {}) {
        if (config.title) title.value = config.title;
        if (config.description) description.value = config.description;
        if (config.image) image.value = config.image;
        if (config.url) url.value = config.url;
        if (config.type) type.value = config.type;
        if (config.jsonLd) jsonLd.value = config.jsonLd;
    }

    function setProductSeo(product) {
        if (!product) return;

        const pTitle = product.tenSanPham || 'Sản phẩm';
        const pDescription = product.moTa
            ? product.moTa.substring(0, 160)
            : `Mua ${product.tenSanPham} chính hãng tại AeroStride. ${product.tenThuongHieu || ''} - ${product.tenDanhMuc || ''}. Giao hàng nhanh, đổi trả miễn phí.`;

        const firstImage = product.variants?.[0]?.images?.[0]?.duongDanAnh || DEFAULTS.image;
        const canonicalUrl = typeof window !== 'undefined' ? window.location.href : '';

        const pJsonLd = {
            '@context': 'https://schema.org',
            '@type': 'Product',
            name: product.tenSanPham,
            description: pDescription,
            image: firstImage,
            brand: {
                '@type': 'Brand',
                name: product.tenThuongHieu || 'AeroStride'
            },
            sku: product.maSanPham || '',
            offers: {
                '@type': 'Offer',
                url: canonicalUrl,
                priceCurrency: 'VND',
                price: product.giaBanThapNhat || 0,
                availability: 'https://schema.org/InStock',
                seller: {
                    '@type': 'Organization',
                    name: 'AeroStride'
                }
            }
        };

        setSeoMeta({
            title: pTitle,
            description: pDescription,
            image: firstImage,
            type: 'product',
            jsonLd: pJsonLd,
            url: canonicalUrl
        });
    }

    function resetSeo() {
        title.value = DEFAULTS.siteName;
        description.value = DEFAULTS.description;
        image.value = DEFAULTS.image;
        url.value = typeof window !== 'undefined' ? window.location.href : '';
        type.value = DEFAULTS.type;
        jsonLd.value = null;
    }

    return {
        setSeoMeta,
        setProductSeo,
        resetSeo
    };
}
