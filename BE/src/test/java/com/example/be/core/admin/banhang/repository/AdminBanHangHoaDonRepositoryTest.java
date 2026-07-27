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
                null
        ).isEmpty());
    }
}
