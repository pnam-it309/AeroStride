package com.example.be.core.admin.sanpham.repository;

import com.example.be.entity.QChiTietSanPham;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class AdminChiTietSanPhamRepositoryCustomImpl implements AdminChiTietSanPhamRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<ProductVariantStatisticsProjection> summarizeBySanPhamIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new ArrayList<>();
        }

        QChiTietSanPham c = QChiTietSanPham.chiTietSanPham;

        List<ProductVariantStatisticsDto> dtos = queryFactory
                .select(Projections.constructor(ProductVariantStatisticsDto.class,
                        c.sanPham.id,
                        c.id.count(),
                        c.soLuong.sum().coalesce(0),
                        c.giaBan.min(),
                        c.giaBan.max()
                ))
                .from(c)
                .where(c.sanPham.id.in(productIds).and(c.xoaMem.eq(false)))
                .groupBy(c.sanPham.id)
                .fetch();

        return new ArrayList<>(dtos);
    }

    public static class ProductVariantStatisticsDto implements ProductVariantStatisticsProjection {
        private final String sanPhamId;
        private final Long tongBienThe;
        private final Long tongSoLuongTon;
        private final BigDecimal giaBanThapNhat;
        private final BigDecimal giaBanCaoNhat;

        public ProductVariantStatisticsDto(String sanPhamId, Long tongBienThe, Number tongSoLuongTon, BigDecimal giaBanThapNhat, BigDecimal giaBanCaoNhat) {
            this.sanPhamId = sanPhamId;
            this.tongBienThe = tongBienThe;
            this.tongSoLuongTon = tongSoLuongTon != null ? tongSoLuongTon.longValue() : 0L;
            this.giaBanThapNhat = giaBanThapNhat;
            this.giaBanCaoNhat = giaBanCaoNhat;
        }

        @Override
        public String getSanPhamId() {
            return sanPhamId;
        }

        @Override
        public Long getTongBienThe() {
            return tongBienThe;
        }

        @Override
        public Long getTongSoLuongTon() {
            return tongSoLuongTon;
        }

        @Override
        public BigDecimal getGiaBanThapNhat() {
            return giaBanThapNhat;
        }

        @Override
        public BigDecimal getGiaBanCaoNhat() {
            return giaBanCaoNhat;
        }
    }
}
