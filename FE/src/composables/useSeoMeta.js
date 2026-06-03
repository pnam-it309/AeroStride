import { useHead, useSeoMeta as useUnheadSeoMeta } from '@unhead/vue';

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
    function setSeoMeta({ title, description, image, url, type, jsonLd } = {}) {
        const fullTitle = title ? `${title}${DEFAULTS.titleSuffix}` : DEFAULTS.siteName;
        const desc = description || DEFAULTS.description;
        const ogImage = image || DEFAULTS.image;
        const ogType = type || DEFAULTS.type;
        const canonicalUrl = url || (typeof window !== 'undefined' ? window.location.href : '');

        useUnheadSeoMeta({
            title: fullTitle,
            description: desc,
            ogTitle: fullTitle,
            ogDescription: desc,
            ogImage: ogImage,
            ogUrl: canonicalUrl,
            ogType: ogType,
            ogSiteName: DEFAULTS.siteName,
            ogLocale: DEFAULTS.locale,
            twitterCard: DEFAULTS.twitterCard,
            twitterTitle: fullTitle,
            twitterDescription: desc,
            twitterImage: ogImage
        });

        useHead({
            link: [
                {
                    rel: 'canonical',
                    href: canonicalUrl
                }
            ]
        });

        if (jsonLd) {
            useHead({
                script: [
                    {
                        type: 'application/ld+json',
                        innerHTML: JSON.stringify(jsonLd)
                    }
                ]
            });
        }
    }

    function setProductSeo(product) {
        if (!product) return;

        const title = product.tenSanPham || 'Sản phẩm';
        const description = product.moTa
            ? product.moTa.substring(0, 160)
            : `Mua ${product.tenSanPham} chính hãng tại AeroStride. ${product.tenThuongHieu || ''} - ${product.tenDanhMuc || ''}. Giao hàng nhanh, đổi trả miễn phí.`;

        const firstImage = product.variants?.[0]?.images?.[0]?.duongDanAnh || DEFAULTS.image;

        const canonicalUrl = typeof window !== 'undefined' ? window.location.href : '';

        const jsonLd = {
            '@context': 'https://schema.org',
            '@type': 'Product',
            name: product.tenSanPham,
            description: description,
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
            title,
            description,
            image: firstImage,
            type: 'product',
            jsonLd
        });
    }

    function resetSeo() {
        setSeoMeta({
            title: null,
            description: DEFAULTS.description
        });
    }

    return {
        setSeoMeta,
        setProductSeo,
        resetSeo
    };
}
