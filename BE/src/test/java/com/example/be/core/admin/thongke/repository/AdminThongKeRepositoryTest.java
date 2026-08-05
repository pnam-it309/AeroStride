package com.example.be.core.admin.thongke.repository;

import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.utils.AccountUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class AdminThongKeRepositoryTest {

    private static final long START_OF_YEAR = AccountUtils.parseDateToLong("2026-01-01", false);
    private static final long AS_OF_DATE = AccountUtils.parseDateToLong("2026-08-05", true);

    @Autowired
    private AdminThongKeRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getOrderTypeStats_ShouldClassifyLegacyOrdersByLoaiDon() {
        persistCompletedOrder("ONLINE", null, "2000000", START_OF_YEAR + 1_000);
        persistCompletedOrder("OFFLINE", null, "3000000", START_OF_YEAR + 2_000);
        persistCompletedOrder("GIAO_HANG", OrderType.IN_STORE, "4000000", START_OF_YEAR + 3_000);
        persistCompletedOrder("TAI_QUAY", OrderType.ONLINE, "5000000", START_OF_YEAR + 4_000);

        // Không được cộng đơn ngoài mốc "đến ngày".
        persistCompletedOrder("ONLINE", null, "9000000", AS_OF_DATE + 1);

        entityManager.flush();
        entityManager.clear();

        List<Object[]> result = repository.getOrderTypeStats(START_OF_YEAR, AS_OF_DATE);
        Object[] row = result.get(0);

        assertMoneyEquals("7000000", row[0]);
        assertEquals(2L, ((Number) row[1]).longValue());
        assertMoneyEquals("7000000", row[2]);
        assertEquals(2L, ((Number) row[3]).longValue());
    }

    private void persistCompletedOrder(String loaiDon, OrderType orderType, String amount, long ngayTao) {
        BigDecimal total = new BigDecimal(amount);
        HoaDon hoaDon = HoaDon.builder()
                .maHoaDon("HD-TEST-" + ngayTao)
                .loaiDon(loaiDon)
                .orderType(orderType)
                .trangThai(OrderStatus.HOAN_THANH)
                .tongTien(total)
                .tongTienSauGiam(total)
                .build();
        hoaDon.setNgayTao(ngayTao);
        entityManager.persist(hoaDon);
    }

    private void assertMoneyEquals(String expected, Object actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())));
    }
}
