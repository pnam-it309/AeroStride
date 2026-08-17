package com.example.be.core.admin.banhang.repository;

import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class AdminBanHangHoaDonRepositoryTest {

    @Autowired
    private AdminBanHangHoaDonRepository repository;

    @Test
    void pendingPosQueryShouldLoadAndReturnEmptyWhenDatabaseHasNoOrders() {
        assertTrue(repository.findAllPendingPOSOrders(
                OrderStatus.CHO_XAC_NHAN,
                OrderType.IN_STORE,
                null,
                null
        ).isEmpty());
    }

    @Test
    void pendingPosQueryShouldOnlyReturnPosOrdersWithTaiQuayOrGiaoHang() {
        com.example.be.entity.HoaDon posOrder = new com.example.be.entity.HoaDon();
        posOrder.setMaHoaDon("HD_POS_TEST");
        posOrder.setTrangThai(OrderStatus.CHO_XAC_NHAN);
        posOrder.setOrderType(OrderType.IN_STORE);
        posOrder.setLoaiDon("TAI_QUAY");
        posOrder.setNgayTao(System.currentTimeMillis());
        repository.save(posOrder);

        com.example.be.entity.HoaDon offlineOrder = new com.example.be.entity.HoaDon();
        offlineOrder.setMaHoaDon("HD_OFFLINE_TEST");
        offlineOrder.setTrangThai(OrderStatus.CHO_XAC_NHAN);
        offlineOrder.setOrderType(OrderType.IN_STORE);
        offlineOrder.setLoaiDon("OFFLINE");
        offlineOrder.setNgayTao(System.currentTimeMillis());
        repository.save(offlineOrder);

        var pendingOrders = repository.findAllPendingPOSOrders(
                OrderStatus.CHO_XAC_NHAN,
                OrderType.IN_STORE,
                null,
                null
        );

        org.junit.jupiter.api.Assertions.assertEquals(1, pendingOrders.size());
        org.junit.jupiter.api.Assertions.assertEquals("HD_POS_TEST", pendingOrders.get(0).getMaHoaDon());
    }
}

