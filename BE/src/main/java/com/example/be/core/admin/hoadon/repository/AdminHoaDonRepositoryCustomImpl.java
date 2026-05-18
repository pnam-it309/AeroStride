package com.example.be.core.admin.hoadon.repository;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.entity.QHoaDon;
import com.example.be.entity.QKhachHang;
import com.example.be.entity.QNhanVien;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class AdminHoaDonRepositoryCustomImpl implements AdminHoaDonRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    private BooleanBuilder buildConditions(AdminHoaDonRequest req) {
        QHoaDon hd = QHoaDon.hoaDon;
        QKhachHang kh = QKhachHang.khachHang;
        BooleanBuilder builder = new BooleanBuilder();

        if (req.getSearch() != null && !req.getSearch().trim().isEmpty()) {
            String search = req.getSearch().toLowerCase().trim();
            builder.and(hd.maHoaDon.toLowerCase().contains(search)
                    .or(kh.ten.toLowerCase().contains(search))
                    .or(hd.soDienThoaiNguoiNhan.contains(search)));
        }

        if (req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty()) {
            builder.and(kh.ten.toLowerCase().contains(req.getTenKhachHang().toLowerCase().trim()));
        }

        if (req.getTrangThai() != null) {
            try {
                OrderStatus status = OrderStatus.values()[req.getTrangThai()];
                builder.and(hd.trangThai.eq(status));
            } catch (Exception e) {
                // Invalid status index, skip or handle
            }
        }

        if (req.getLoaiDon() != null && !req.getLoaiDon().trim().isEmpty()) {
            builder.and(hd.loaiDon.eq(req.getLoaiDon()));
        }

        if (req.getTuNgayLong() != null) {
            builder.and(hd.ngayTao.goe(req.getTuNgayLong()));
        }

        if (req.getDenNgayLong() != null) {
            builder.and(hd.ngayTao.loe(req.getDenNgayLong()));
        }

        return builder;
    }

    @Override
    public Page<AdminHoaDonResponse> getAllHoaDon(Pageable pageable, AdminHoaDonRequest req) {
        QHoaDon hd = QHoaDon.hoaDon;
        QKhachHang kh = QKhachHang.khachHang;
        QNhanVien nv = QNhanVien.nhanVien;

        BooleanBuilder conditions = buildConditions(req);

        // Use Tuple to avoid problematic Enum-to-Ordinal SQL function translation in projections
        List<Tuple> tuples = queryFactory
                .select(
                        hd.id, hd.maHoaDon, hd.ngayTao, kh.ten, hd.soDienThoaiNguoiNhan, nv.ma, nv.ten,
                        hd.loaiDon, hd.phiVanChuyen, hd.tongTien, hd.tongTienSauGiam, hd.trangThai, hd.ghiChu
                )
                .from(hd)
                .leftJoin(hd.khachHang, kh)
                .leftJoin(hd.nhanVien, nv)
                .where(conditions)
                .orderBy(hd.ngayTao.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<AdminHoaDonResponse> content = tuples.stream().map(t -> {
            OrderStatus status = t.get(hd.trangThai);
            return AdminHoaDonResponse.builder()
                    .id(t.get(hd.id))
                    .maHoaDon(t.get(hd.maHoaDon))
                    .ngayTao(t.get(hd.ngayTao))
                    .tenKhachHang(t.get(kh.ten))
                    .soDienThoai(t.get(hd.soDienThoaiNguoiNhan))
                    .maNhanVien(t.get(nv.ma))
                    .tenNhanVien(t.get(nv.ten))
                    .loaiDon(t.get(hd.loaiDon))
                    .phiVanChuyen(t.get(hd.phiVanChuyen))
                    .tongTien(t.get(hd.tongTien))
                    .tongTienSauGiam(t.get(hd.tongTienSauGiam))
                    .trangThai(status != null ? status.ordinal() : null)
                    .ghiChu(t.get(hd.ghiChu))
                    .build();
        }).collect(Collectors.toList());

        long total = queryFactory
                .select(hd.count())
                .from(hd)
                .leftJoin(hd.khachHang, kh)
                .where(conditions)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Map<String, Object>> countByTrangThai(AdminHoaDonRequest req) {
        QHoaDon hd = QHoaDon.hoaDon;
        QKhachHang kh = QKhachHang.khachHang;
        BooleanBuilder conditions = buildConditions(req);

        List<Tuple> results = queryFactory
                .select(hd.trangThai, hd.count())
                .from(hd)
                .leftJoin(hd.khachHang, kh)
                .where(conditions)
                .groupBy(hd.trangThai)
                .fetch();

        return results.stream().map(tuple -> {
            Map<String, Object> map = new HashMap<>();
            OrderStatus status = tuple.get(hd.trangThai);
            map.put("status", status != null ? status.ordinal() : null);
            map.put("count", tuple.get(1, Long.class));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public long countWithFilter(AdminHoaDonRequest req) {
        QHoaDon hd = QHoaDon.hoaDon;
        QKhachHang kh = QKhachHang.khachHang;
        BooleanBuilder conditions = buildConditions(req);

        Long count = queryFactory
                .select(hd.count())
                .from(hd)
                .leftJoin(hd.khachHang, kh)
                .where(conditions)
                .fetchOne();

        return count != null ? count : 0L;
    }
}
