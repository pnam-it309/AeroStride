package com.example.be.core.admin.thongke.repository;

import com.example.be.core.admin.thongke.model.response.AdminThongKeResponse;
import com.example.be.entity.HoaDon;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class AdminThongKeRepositoryCustomImpl implements AdminThongKeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<AdminThongKeResponse.DoanhThuNgay> getDoanhThuTheoNgay(Specification<HoaDon> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AdminThongKeResponse.DoanhThuNgay> query = cb.createQuery(AdminThongKeResponse.DoanhThuNgay.class);
        Root<HoaDon> root = query.from(HoaDon.class);

        // DATE_FORMAT(FROM_UNIXTIME(hd.ngay_tao / 1000), '%Y-%m-%d')
        // cb.prod(root.get("ngayTao").as(Double.class), 0.001) scales milliseconds to Unix timestamp (seconds)
        Expression<Double> unixTimestamp = cb.prod(root.get("ngayTao").as(Double.class), 0.001);
        Expression<java.sql.Date> fromUnixTime = cb.function("from_unixtime", java.sql.Date.class, unixTimestamp);
        Expression<String> dateString = cb.function("date_format", String.class, fromUnixTime, cb.literal("%Y-%m-%d"));

        // Direct DTO mapping via Criteria Construct
        query.select(cb.construct(
                AdminThongKeResponse.DoanhThuNgay.class,
                dateString,
                cb.coalesce(cb.sum(root.get("tongTien")), BigDecimal.ZERO),
                cb.count(root.get("id"))
        ));

        // Apply Specification Predicates
        Predicate specPredicate = spec.toPredicate(root, query, cb);
        if (specPredicate != null) {
            query.where(specPredicate);
        }

        query.groupBy(dateString);
        query.orderBy(cb.asc(dateString));

        return entityManager.createQuery(query).getResultList();
    }
}
