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
            " CONCAT(COALESCE(dc.diaChiChiTiet, ''), ', ', COALESCE(dc.phuongXa, ''), ', ', COALESCE(dc.thanhPho, ''), ', ', COALESCE(dc.tinh, ''))" +
            ")";

    private static final String FROM_CLAUSE =
            " FROM KhachHang kh" +
            " LEFT JOIN kh.diaChi dc";

    private static final String GROUP_BY_CLAUSE = ""; // No longer needed since no aggregates

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
            String keyword,
            TrangThai trangThai,
            Boolean gioiTinh,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        StringBuilder countJpql = new StringBuilder("SELECT COUNT(kh) FROM KhachHang kh WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String filterKeyword = "%" + keyword.toLowerCase().trim() + "%";
            String condition = " AND (LOWER(kh.ten) LIKE :keyword OR LOWER(kh.email) LIKE :keyword OR kh.sdt LIKE :keyword OR LOWER(kh.ma) LIKE :keyword)";
            where.append(condition);
            countJpql.append(condition);
            params.put("keyword", filterKeyword);
        }

        if (trangThai != null) {
            String condition = " AND kh.trangThai = :trangThai";
            where.append(condition);
            countJpql.append(condition);
            params.put("trangThai", trangThai);
        }

        if (gioiTinh != null) {
            String condition = " AND kh.gioiTinh = :gioiTinh";
            where.append(condition);
            countJpql.append(condition);
            params.put("gioiTinh", gioiTinh);
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
