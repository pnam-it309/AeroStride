package com.example.be.core.admin.hoadon.repository;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.infrastructure.constants.DeliveryMethod;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.entity.QHoaDon;
import com.example.be.entity.QKhachHang;
import com.example.be.entity.QNhanVien;
import com.example.be.entity.QHoaDonChiTiet;
import com.example.be.entity.QChiTietSanPham;
import com.example.be.entity.QSanPham;
import com.example.be.entity.QMauSac;
import com.example.be.entity.QKichThuoc;
import com.example.be.entity.QDiaChi;
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

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.be.repository.NhanVienRepository nhanVienRepository;

    private JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    private BooleanBuilder buildConditions(AdminHoaDonRequest req) {
        QHoaDon hd = QHoaDon.hoaDon;
        QKhachHang kh = QKhachHang.khachHang;
        BooleanBuilder builder = new BooleanBuilder();

        // Ẩn các hóa đơn nháp (trống) do màn hình Bán Hàng tạo ra (Tổng tiền <= 0 và đang chờ xác nhận)
        // Đồng thời, ẩn CẢ các hóa đơn có NhanVien != null đang ở trạng thái CHO_XAC_NHAN 
        // (đây là các hóa đơn POS draft, dù có được đổi loaiDon thành ONLINE hay thêm sản phẩm thì vẫn là draft chưa thanh toán)
        builder.and(
            hd.trangThai.ne(OrderStatus.CHO_XAC_NHAN)
            .or(hd.loaiDon.notIn("TAI_QUAY", "GIAO_HANG"))
        );

        // Đơn hàng đã hủy (DA_HUY): Chỉ có bên trực tuyến (ONLINE), không có bên bán hàng tại quầy.
        // Loại bỏ hoàn toàn hóa đơn bán hàng tại quầy đã hủy khỏi Quản lý hóa đơn.
        builder.and(
            hd.trangThai.ne(OrderStatus.DA_HUY)
            .or(
                hd.orderType.eq(OrderType.ONLINE)
                .or(hd.orderType.isNull().and(hd.nhanVien.isNull()).and(hd.loaiDon.equalsIgnoreCase("ONLINE")))
            )
        );

        if (req.getSearch() != null && !req.getSearch().trim().isEmpty()) {
            String search = req.getSearch().toLowerCase().trim();
            builder.and(hd.maHoaDon.toLowerCase().contains(search)
                    .or(kh.ten.toLowerCase().contains(search))
                    .or(hd.tenNguoiNhan.toLowerCase().contains(search))
                    .or(hd.soDienThoaiNguoiNhan.contains(search))
                    .or(kh.sdt.contains(search)));
        }

        if (req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty()) {
            builder.and(kh.ten.toLowerCase().contains(req.getTenKhachHang().toLowerCase().trim()));
        }

        if (req.getIdKhachHang() != null && !req.getIdKhachHang().trim().isEmpty()) {
            builder.and(kh.id.eq(req.getIdKhachHang()));
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
            String ld = req.getLoaiDon().trim().toUpperCase();
            if ("GIAO_HANG".equals(ld)) {
                builder.and(hd.loaiDon.equalsIgnoreCase("GIAO_HANG")
                        .or(hd.deliveryMethod.eq(DeliveryMethod.SHIPPING).and(hd.orderType.eq(OrderType.IN_STORE))));
            } else if ("ONLINE".equals(ld)) {
                builder.and(hd.orderType.eq(OrderType.ONLINE)
                        .or(hd.orderType.isNull().and(hd.nhanVien.isNull()).and(hd.loaiDon.equalsIgnoreCase("ONLINE"))));
            } else if ("IN_STORE".equals(ld) || "TAI_QUAY".equals(ld) || "OFFLINE".equals(ld)) {
                builder.and(
                        (hd.orderType.eq(OrderType.IN_STORE).and(hd.deliveryMethod.ne(DeliveryMethod.SHIPPING).or(hd.deliveryMethod.isNull())).and(hd.loaiDon.ne("GIAO_HANG").or(hd.loaiDon.isNull())))
                                .or(hd.orderType.isNull().and(hd.loaiDon.in("TAI_QUAY", "OFFLINE")))
                );
            }
        } else if (req.getOrderType() != null) {
            if (req.getOrderType() == OrderType.IN_STORE) {
                builder.and(hd.orderType.eq(OrderType.IN_STORE)
                        .or(hd.orderType.isNull().and(hd.nhanVien.isNotNull()
                                .or(hd.loaiDon.in("TAI_QUAY", "OFFLINE", "GIAO_HANG")))));
            } else {
                builder.and(hd.orderType.eq(OrderType.ONLINE)
                        .or(hd.orderType.isNull().and(hd.nhanVien.isNull()).and(hd.loaiDon.equalsIgnoreCase("ONLINE"))));
            }
        }

        if (req.getTuNgayLong() != null) {
            builder.and(hd.ngayTao.goe(req.getTuNgayLong()));
        }

        if (req.getDenNgayLong() != null) {
            builder.and(hd.ngayTao.loe(req.getDenNgayLong()));
        }

        if (req.getIdKhachHang() != null && !req.getIdKhachHang().trim().isEmpty()) {
            builder.and(hd.khachHang.id.eq(req.getIdKhachHang().trim()));
        }

        return builder;
    }

    @Override
    public Page<AdminHoaDonResponse> getAllHoaDon(Pageable pageable, AdminHoaDonRequest req) {
        QHoaDon hd = QHoaDon.hoaDon;
        QKhachHang kh = QKhachHang.khachHang;
        QNhanVien nv = QNhanVien.nhanVien;

        boolean isAsc = !"desc".equalsIgnoreCase(req.getSortDirection());
        BooleanBuilder conditions = buildConditions(req);

        // Direct DTO fields projection without heavy extra joins
        List<Tuple> tuples = queryFactory
                .select(
                        hd.id, hd.maHoaDon, hd.ngayTao, kh.ten, kh.sdt,
                        hd.tenNguoiNhan, hd.soDienThoaiNguoiNhan, hd.diaChiNguoiNhan,
                        nv.ma, nv.ten, nv.sdt,
                        hd.orderType, hd.deliveryMethod, hd.loaiDon, hd.phiVanChuyen, hd.phiHoanHang,
                        hd.tongTien, hd.tongTienSauGiam, hd.trangThai, hd.ghiChu
                )
                .from(hd)
                .leftJoin(hd.khachHang, kh)
                .leftJoin(hd.nhanVien, nv)
                .where(conditions)
                .orderBy(
                        isAsc ? hd.ngayTao.asc() : hd.ngayTao.desc(),
                        isAsc ? hd.id.asc() : hd.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<AdminHoaDonResponse> content = tuples.stream().map(t -> {
            OrderStatus status = t.get(hd.trangThai);
            
            String sdtKh = t.get(kh.sdt);
            String sdtNhan = t.get(hd.soDienThoaiNguoiNhan);
            String sdtNv = t.get(nv.sdt);
            String finalSdt = (sdtNhan != null && !sdtNhan.trim().isEmpty()) ? sdtNhan : 
                              (sdtKh != null && !sdtKh.trim().isEmpty()) ? sdtKh : 
                              (sdtNv != null && !sdtNv.trim().isEmpty()) ? sdtNv : "0988888888";

            String maNv = t.get(nv.ma);
            String tenNv = t.get(nv.ten);
            String finalMaNv = (maNv != null && !maNv.trim().isEmpty() && !"Hệ thống".equalsIgnoreCase(maNv)) ? maNv : null;
            String finalTenNv = (tenNv != null && !tenNv.trim().isEmpty() && !"Hệ thống".equalsIgnoreCase(tenNv)) ? tenNv : null;

            String tenKh = t.get(kh.ten);
            String tenNhan = t.get(hd.tenNguoiNhan);
            String finalTenKh = (tenKh != null && !tenKh.isBlank()) ? tenKh : (tenNhan != null && !tenNhan.isBlank()) ? tenNhan : "Khách lẻ";

            return AdminHoaDonResponse.builder()
                    .id(t.get(hd.id))
                    .maHoaDon(t.get(hd.maHoaDon))
                    .ngayTao(t.get(hd.ngayTao))
                    .tenKhachHang(finalTenKh)
                    .soDienThoai(finalSdt)
                    .diaChiNguoiNhan(t.get(hd.diaChiNguoiNhan))
                    .maNhanVien(finalMaNv)
                    .tenNhanVien(finalTenNv)
                    .orderType(t.get(hd.orderType) != null
                            ? t.get(hd.orderType)
                            : (t.get(nv.ma) != null || !OrderType.ONLINE.name().equalsIgnoreCase(t.get(hd.loaiDon))
                                    ? OrderType.IN_STORE : OrderType.ONLINE))
                    .deliveryMethod(t.get(hd.deliveryMethod) != null
                            ? t.get(hd.deliveryMethod)
                            : (java.util.Set.of(OrderType.ONLINE.name(), "GIAO_HANG").contains(
                                    String.valueOf(t.get(hd.loaiDon)).toUpperCase())
                                    ? DeliveryMethod.SHIPPING
                                    : DeliveryMethod.TAKEAWAY))
                    .loaiDon(t.get(hd.loaiDon))
                    .phiVanChuyen(t.get(hd.phiVanChuyen))
                    .phiHoanHang(t.get(hd.phiHoanHang))
                    .tongTien(t.get(hd.tongTien))
                    .tongTienSauGiam(t.get(hd.tongTienSauGiam))
                    .trangThai(status != null ? status.ordinal() : null)
                    .ghiChu(t.get(hd.ghiChu))
                    .bienThes(java.util.Collections.emptyList())
                    .details(java.util.Collections.emptyList())
                    .build();
        }).collect(Collectors.toList());

        long total;
        if (pageable.getOffset() == 0 && tuples.size() < pageable.getPageSize()) {
            total = tuples.size();
        } else {
            Long countVal = queryFactory
                    .select(hd.count())
                    .from(hd)
                    .leftJoin(hd.khachHang, kh)
                    .where(conditions)
                    .fetchOne();
            total = countVal != null ? countVal : 0L;
        }

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
