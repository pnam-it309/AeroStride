package com.example.be.core.admin.khachhang.repository;

import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.infrastructure.constants.TrangThai;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AdminKhachHangRepositoryCustomImpl implements AdminKhachHangRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    // ── Shared query fragments (DRY) ──────────────────────────────────────

    private static final String SELECT_CLAUSE =
            "SELECT new com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse(" +
            " kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan," +
            " kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu," +
            " kh.trangThai, kh.ngayTao, kh.ngayCapNhat," +
            " CONCAT(COALESCE(dc.diaChiChiTiet, ''), ', ', COALESCE(dc.phuongXa, ''), ', ', COALESCE(dc.thanhPho, ''), ', ', COALESCE(dc.tinh, ''))," +
            " SUM(CASE WHEN hd.trangThai = com.example.be.infrastructure.constants.OrderStatus.HOAN_THANH THEN hd.tongTienSauGiam ELSE 0.0 END)," +
            " MAX(hd.ngayTao)," +
            " COUNT(CASE WHEN hd.trangThai = com.example.be.infrastructure.constants.OrderStatus.HOAN_THANH THEN hd.id ELSE NULL END)," +
            " COUNT(CASE WHEN hd.trangThai = com.example.be.infrastructure.constants.OrderStatus.HOAN_DON THEN hd.id ELSE NULL END)" +
            ")";

    private static final String FROM_CLAUSE =
            " FROM KhachHang kh" +
            " LEFT JOIN kh.diaChi dc" +
            " LEFT JOIN HoaDon hd ON hd.khachHang = kh";

    private static final String GROUP_BY_CLAUSE =
            " GROUP BY kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan," +
            "          kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu," +
            "          kh.trangThai, kh.ngayTao, kh.ngayCapNhat," +
            "          dc.diaChiChiTiet, dc.phuongXa, dc.thanhPho, dc.tinh";

    // ── Interface implementations ─────────────────────────────────────────

    @Override
    public List<AdminKhachHangResponse> hienThi() {
        String jpql = SELECT_CLAUSE + FROM_CLAUSE + GROUP_BY_CLAUSE + " ORDER BY kh.ngayTao DESC";
        return entityManager.createQuery(jpql, AdminKhachHangResponse.class).getResultList();
    }

    @Override
    public AdminKhachHangResponse detail(String id) {
        String jpql = SELECT_CLAUSE + FROM_CLAUSE + " WHERE kh.id = :id" + GROUP_BY_CLAUSE;
        List<AdminKhachHangResponse> list = entityManager.createQuery(jpql, AdminKhachHangResponse.class)
                .setParameter("id", id)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Page<AdminKhachHangResponse> filterAll(
            com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest request,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        StringBuilder countJpql = new StringBuilder("SELECT COUNT(kh) FROM KhachHang kh WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String filterKeyword = "%" + request.getKeyword().toLowerCase().trim() + "%";
            String condition = " AND (LOWER(kh.ten) LIKE :keyword OR LOWER(kh.email) LIKE :keyword OR kh.sdt LIKE :keyword OR LOWER(kh.ma) LIKE :keyword)";
            where.append(condition);
            countJpql.append(condition);
            params.put("keyword", filterKeyword);
        }

        if (request.getTrangThai() != null) {
            String condition = " AND kh.trangThai = :trangThai";
            where.append(condition);
            countJpql.append(condition);
            params.put("trangThai", request.getTrangThai());
        }

        if (request.getGioiTinh() != null) {
            String condition = " AND kh.gioiTinh = :gioiTinh";
            where.append(condition);
            countJpql.append(condition);
            params.put("gioiTinh", request.getGioiTinh());
        }
        
        if (request.getSdtSearch() != null && !request.getSdtSearch().trim().isEmpty()) {
            String condition = " AND kh.sdt LIKE :sdtSearch";
            where.append(condition);
            countJpql.append(condition);
            params.put("sdtSearch", "%" + request.getSdtSearch().trim() + "%");
        }
        
        if (request.getMinTongChiTieu() != null) {
            String condition = " AND (SELECT COALESCE(SUM(hd.tongTienSauGiam), 0.0) FROM HoaDon hd WHERE hd.khachHang = kh AND hd.trangThai = com.example.be.infrastructure.constants.OrderStatus.HOAN_THANH) >= :minTongChiTieu";
            where.append(condition);
            countJpql.append(condition);
            params.put("minTongChiTieu", request.getMinTongChiTieu());
        }

        if (request.getMaxTongChiTieu() != null) {
            String condition = " AND (SELECT COALESCE(SUM(hd.tongTienSauGiam), 0.0) FROM HoaDon hd WHERE hd.khachHang = kh AND hd.trangThai = com.example.be.infrastructure.constants.OrderStatus.HOAN_THANH) <= :maxTongChiTieu";
            where.append(condition);
            countJpql.append(condition);
            params.put("maxTongChiTieu", request.getMaxTongChiTieu());
        }
        
        if (request.getMinNgayDonHang() != null) {
            String condition = " AND (SELECT MAX(hd.ngayTao) FROM HoaDon hd WHERE hd.khachHang = kh) >= :minNgayDonHang";
            where.append(condition);
            countJpql.append(condition);
            params.put("minNgayDonHang", request.getMinNgayDonHang().atStartOfDay());
        }
        
        if (request.getMaxNgayDonHang() != null) {
            String condition = " AND (SELECT MAX(hd.ngayTao) FROM HoaDon hd WHERE hd.khachHang = kh) <= :maxNgayDonHang";
            where.append(condition);
            countJpql.append(condition);
            params.put("maxNgayDonHang", request.getMaxNgayDonHang().plusDays(1).atStartOfDay().minusNanos(1));
        }

        // Build main query from shared fragments
        String jpql = SELECT_CLAUSE + FROM_CLAUSE + where + GROUP_BY_CLAUSE + " ORDER BY kh.ngayTao DESC";

        TypedQuery<AdminKhachHangResponse> query = entityManager.createQuery(jpql, AdminKhachHangResponse.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql.toString(), Long.class);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
            countQuery.setParameter(entry.getKey(), entry.getValue());
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<AdminKhachHangResponse> content = query.getResultList();
        long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
