package com.example.be.core.admin.hoadon.repository;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.infrastructure.constants.OrderStatus;
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
            .or(hd.nhanVien.isNull())
        );

        if (req.getSearch() != null && !req.getSearch().trim().isEmpty()) {
            String search = req.getSearch().toLowerCase().trim();
            builder.and(hd.maHoaDon.toLowerCase().contains(search)
                    .or(kh.ten.toLowerCase().contains(search))
                    .or(hd.soDienThoaiNguoiNhan.contains(search)));
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
            if ("TAI_QUAY".equalsIgnoreCase(req.getLoaiDon()) || "OFFLINE".equalsIgnoreCase(req.getLoaiDon())) {
                builder.and(hd.loaiDon.in("TAI_QUAY", "OFFLINE"));
            } else {
                builder.and(hd.loaiDon.eq(req.getLoaiDon()));
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
        QDiaChi khdc = QDiaChi.diaChi;

        BooleanBuilder conditions = buildConditions(req);

        // Use Tuple to avoid problematic Enum-to-Ordinal SQL function translation in projections
        List<Tuple> tuples = queryFactory
                .select(
                        hd.id, hd.maHoaDon, hd.ngayTao, kh.ten, kh.sdt,
                        hd.soDienThoaiNguoiNhan, hd.diaChiNguoiNhan,
                        khdc.diaChiChiTiet, khdc.phuongXa, khdc.thanhPho, khdc.tinh,
                        nv.ma, nv.ten,
                        hd.loaiDon, hd.phiVanChuyen, hd.phiHoanHang, hd.tongTien, hd.tongTienSauGiam, hd.trangThai, hd.ghiChu
                )
                .from(hd)
                .leftJoin(hd.khachHang, kh)
                .leftJoin(khdc).on(khdc.khachHang.id.eq(kh.id).and(khdc.laMacDinh.eq(true)))
                .leftJoin(hd.nhanVien, nv)
                .where(conditions)
                .orderBy("asc".equalsIgnoreCase(req.getSortDirection()) ? hd.ngayTao.asc() : hd.ngayTao.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        com.example.be.entity.NhanVien loggedInNv = null;
        try {
            java.util.Optional<String> currentUser = com.example.be.utils.SecurityUtils.getCurrentUserEmail();
            if (currentUser.isPresent() && nhanVienRepository != null) {
                loggedInNv = nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(
                        currentUser.get(), currentUser.get(), currentUser.get(), currentUser.get()
                ).orElse(null);
            }
        } catch (Exception e) {
            // ignore
        }
        final com.example.be.entity.NhanVien currentNv = loggedInNv;
        final String fallbackMaNv = (currentNv != null && currentNv.getMa() != null) ? currentNv.getMa() : 
                                    (com.example.be.utils.SecurityUtils.getCurrentUserEmail().orElse("ADMIN")).toUpperCase();
        final String fallbackTenNv = (currentNv != null && currentNv.getTen() != null) ? currentNv.getTen() : "Quản trị viên";
        final String fallbackSdtNv = (currentNv != null && currentNv.getSdt() != null && !currentNv.getSdt().trim().isEmpty()) ? currentNv.getSdt() : "0988888888";

        List<AdminHoaDonResponse> content = tuples.stream().map(t -> {
            OrderStatus status = t.get(hd.trangThai);
            
            String sdtKh = t.get(kh.sdt);
            String sdtNhan = t.get(hd.soDienThoaiNguoiNhan);
            String sdtNv = t.get(nv.sdt);
            String finalSdt = (sdtNhan != null && !sdtNhan.trim().isEmpty()) ? sdtNhan : 
                              (sdtKh != null && !sdtKh.trim().isEmpty()) ? sdtKh : 
                              (sdtNv != null && !sdtNv.trim().isEmpty()) ? sdtNv : fallbackSdtNv;

            String maNv = t.get(nv.ma);
            String tenNv = t.get(nv.ten);
            String finalMaNv = (maNv != null && !maNv.trim().isEmpty() && !"Hệ thống".equalsIgnoreCase(maNv)) ? maNv : fallbackMaNv;
            String finalTenNv = (tenNv != null && !tenNv.trim().isEmpty() && !"Hệ thống".equalsIgnoreCase(tenNv)) ? tenNv : fallbackTenNv;

            String dcNhan = t.get(hd.diaChiNguoiNhan);
            String finalDiaChi;
            if (dcNhan != null && !dcNhan.trim().isEmpty()) {
                finalDiaChi = dcNhan;
            } else {
                String ct = t.get(khdc.diaChiChiTiet);
                String px = t.get(khdc.phuongXa);
                String tp = t.get(khdc.thanhPho);
                String tinh = t.get(khdc.tinh);
                
                StringBuilder sb = new StringBuilder();
                if (ct != null && !ct.trim().isEmpty()) sb.append(ct.trim());
                if (px != null && !px.trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(px.trim());
                }
                if (tp != null && !tp.trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(tp.trim());
                }
                if (tinh != null && !tinh.trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(tinh.trim());
                }
                finalDiaChi = sb.length() > 0 ? sb.toString() : null;
            }

            return AdminHoaDonResponse.builder()
                    .id(t.get(hd.id))
                    .maHoaDon(t.get(hd.maHoaDon))
                    .ngayTao(t.get(hd.ngayTao))
                    .tenKhachHang(t.get(kh.ten))
                    .soDienThoai(finalSdt)
                    .diaChiNguoiNhan(finalDiaChi)
                    .maNhanVien(finalMaNv)
                    .tenNhanVien(finalTenNv)
                    .loaiDon(t.get(hd.loaiDon))
                    .phiVanChuyen(t.get(hd.phiVanChuyen))
                    .phiHoanHang(t.get(hd.phiHoanHang))
                    .tongTien(t.get(hd.tongTien))
                    .tongTienSauGiam(t.get(hd.tongTienSauGiam))
                    .trangThai(status != null ? status.ordinal() : null)
                    .ghiChu(t.get(hd.ghiChu))
                    .build();
        }).collect(Collectors.toList());

        List<String> hdIds = content.stream().map(AdminHoaDonResponse::getId).collect(Collectors.toList());
        if (!hdIds.isEmpty()) {
            QHoaDonChiTiet hdct = QHoaDonChiTiet.hoaDonChiTiet;
            QChiTietSanPham ctsp = QChiTietSanPham.chiTietSanPham;
            QSanPham sp = QSanPham.sanPham;
            QMauSac ms = QMauSac.mauSac;
            QKichThuoc kt = QKichThuoc.kichThuoc;

            List<Tuple> items = queryFactory
                    .select(hdct.hoaDon.id, sp.ten, ms.ten, kt.ten, hdct.soLuong)
                    .from(hdct)
                    .join(hdct.chiTietSanPham, ctsp)
                    .join(ctsp.sanPham, sp)
                    .leftJoin(ctsp.mauSac, ms)
                    .leftJoin(ctsp.kichThuoc, kt)
                    .where(hdct.hoaDon.id.in(hdIds))
                    .fetch();

            Map<String, List<String>> itemMap = new HashMap<>();
            Map<String, List<AdminHoaDonResponse.OrderDetailResponse>> detailsMap = new HashMap<>();
            for (Tuple item : items) {
                String hdId = item.get(hdct.hoaDon.id);
                String spTen = item.get(sp.ten);
                String msTen = item.get(ms.ten);
                String ktTen = item.get(kt.ten);
                Integer sl = item.get(hdct.soLuong);
                BigDecimal dg = item.get(hdct.donGia);

                StringBuilder sb = new StringBuilder(spTen);
                if (msTen != null || ktTen != null) {
                    sb.append(" [");
                    if (msTen != null) sb.append(msTen);
                    if (ktTen != null) {
                        if (msTen != null) sb.append(" - ");
                        sb.append(ktTen);
                    }
                    sb.append("]");
                }
                sb.append(" (x").append(sl).append(")");

                itemMap.computeIfAbsent(hdId, k -> new java.util.ArrayList<>()).add(sb.toString());
                
                detailsMap.computeIfAbsent(hdId, k -> new java.util.ArrayList<>()).add(
                    AdminHoaDonResponse.OrderDetailResponse.builder()
                        .tenSanPham(spTen)
                        .mauSac(msTen != null ? msTen : "-")
                        .kichThuoc(ktTen != null ? ktTen : "-")
                        .soLuong(sl)
                        .donGia(dg)
                        .build()
                );
            }

            for (AdminHoaDonResponse res : content) {
                res.setBienThes(itemMap.getOrDefault(res.getId(), java.util.Collections.emptyList()));
                res.setDetails(detailsMap.getOrDefault(res.getId(), java.util.Collections.emptyList()));
            }
        }

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
