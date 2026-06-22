package com.example.be.core.customer.sitemap;

import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping(RoutesConstant.CUSTOMER_SITEMAP)
@RequiredArgsConstructor
public class CustomerSitemapController {

    private final SanPhamRepository sanPhamRepository;

    @GetMapping(value = "/products.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getProductSitemap() {
        List<SanPham> products = sanPhamRepository.findAllByXoaMemFalseAndTrangThai(TrangThai.DANG_HOAT_DONG);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (SanPham p : products) {
            String url = "https://aerostride.me/product/" + p.getId();
            long timestamp = p.getNgayCapNhat() != null ? p.getNgayCapNhat() : (p.getNgayTao() != null ? p.getNgayTao() : System.currentTimeMillis());
            String lastMod = formatter.format(Instant.ofEpochMilli(timestamp));
            
            xml.append("  <url>\n");
            xml.append("    <loc>").append(url).append("</loc>\n");
            xml.append("    <lastmod>").append(lastMod).append("</lastmod>\n");
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }
}
