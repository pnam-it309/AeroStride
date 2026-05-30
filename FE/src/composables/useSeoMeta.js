/**
 * Composable: useSeoMeta
 * ---------------------
 * Quản lý dynamic SEO meta tags cho từng page.
 * Tự động cập nhật <title>, meta description, Open Graph, Twitter Card, và canonical URL.
 *
 * Usage:
 *   const { setSeoMeta } = useSeoMeta();
 *   setSeoMeta({ title: 'AeroStride - Giày chính hãng', description: '...' });
 */

const DEFAULTS = {
    siteName: 'AeroStride',
    titleSuffix: ' | AeroStride',
    description: 'AeroStride - Cửa hàng giày thể thao chính hãng. Mua sắm giày Nike, Adidas, Puma với giá tốt nhất, giao hàng toàn quốc.',
    image: '/favicon.png',
    type: 'website',
    locale: 'vi_VN',
    twitterCard: 'summary_large_image'
};

/**
 * Tạo hoặc cập nhật 1 thẻ <meta>
 */
function setMetaTag(attr, key, content) {
    if (!content) return;
    let el = document.querySelector(`meta[${attr}="${key}"]`);
    if (!el) {
        el = document.createElement('meta');
        el.setAttribute(attr, key);
        document.head.appendChild(el);
    }
    el.setAttribute('content', content);
}

/**
 * Tạo hoặc cập nhật <link rel="canonical">
 */
function setCanonical(url) {
    if (!url) return;
    let el = document.querySelector('link[rel="canonical"]');
    if (!el) {
        el = document.createElement('link');
        el.setAttribute('rel', 'canonical');
        document.head.appendChild(el);
    }
    el.setAttribute('href', url);
}

/**
 * Thêm JSON-LD structured data
 */
function setJsonLd(data) {
    // Xóa script cũ nếu có
    const existing = document.querySelector('script[data-seo-jsonld]');
    if (existing) existing.remove();

    if (!data) return;

    const script = document.createElement('script');
    script.setAttribute('type', 'application/ld+json');
    script.setAttribute('data-seo-jsonld', 'true');
    script.textContent = JSON.stringify(data);
    document.head.appendChild(script);
}

export function useSeoMeta() {
    /**
     * Set SEO meta tags cho trang hiện tại
     * @param {Object} options
     * @param {string} options.title - Tiêu đề trang (không bao gồm suffix)
     * @param {string} [options.description] - Mô tả trang
     * @param {string} [options.image] - Ảnh Open Graph (absolute URL)
     * @param {string} [options.url] - Canonical URL
     * @param {string} [options.type] - OG type (website, product, article...)
     * @param {Object} [options.jsonLd] - JSON-LD structured data
     */
    function setSeoMeta({
        title,
        description,
        image,
        url,
        type,
        jsonLd
    } = {}) {
        const fullTitle = title ? `${title}${DEFAULTS.titleSuffix}` : DEFAULTS.siteName;
        const desc = description || DEFAULTS.description;
        const ogImage = image || DEFAULTS.image;
        const ogType = type || DEFAULTS.type;
        const canonicalUrl = url || window.location.href;

        // <title>
        document.title = fullTitle;

        // Basic meta
        setMetaTag('name', 'description', desc);

        // Open Graph (Facebook, Zalo, LinkedIn...)
        setMetaTag('property', 'og:title', fullTitle);
        setMetaTag('property', 'og:description', desc);
        setMetaTag('property', 'og:image', ogImage);
        setMetaTag('property', 'og:url', canonicalUrl);
        setMetaTag('property', 'og:type', ogType);
        setMetaTag('property', 'og:site_name', DEFAULTS.siteName);
        setMetaTag('property', 'og:locale', DEFAULTS.locale);

        // Twitter Card
        setMetaTag('name', 'twitter:card', DEFAULTS.twitterCard);
        setMetaTag('name', 'twitter:title', fullTitle);
        setMetaTag('name', 'twitter:description', desc);
        setMetaTag('name', 'twitter:image', ogImage);

        // Canonical
        setCanonical(canonicalUrl);

        // JSON-LD Structured Data
        if (jsonLd) {
            setJsonLd(jsonLd);
        }
    }

    /**
     * SEO cho trang sản phẩm — bao gồm Product structured data
     */
    function setProductSeo(product) {
        if (!product) return;

        const title = product.tenSanPham || 'Sản phẩm';
        const description = product.moTa
            ? product.moTa.substring(0, 160)
            : `Mua ${product.tenSanPham} chính hãng tại AeroStride. ${product.tenThuongHieu || ''} - ${product.tenDanhMuc || ''}. Giao hàng nhanh, đổi trả miễn phí.`;

        const firstImage = product.variants?.[0]?.images?.[0]?.duongDanAnh || DEFAULTS.image;

        // Product JSON-LD (Google Rich Results)
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
                url: window.location.href,
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

    /**
     * Reset về mặc định (thường dùng khi unmount)
     */
    function resetSeo() {
        setSeoMeta({
            title: null,
            description: DEFAULTS.description
        });
        setJsonLd(null);
    }

    return {
        setSeoMeta,
        setProductSeo,
        resetSeo
    };
}
