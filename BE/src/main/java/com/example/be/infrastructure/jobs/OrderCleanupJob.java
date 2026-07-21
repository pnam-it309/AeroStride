package com.example.be.infrastructure.jobs;

import com.example.be.infrastructure.constants.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupJob {

    private final EntityManager entityManager;

    /**
     * Runs every hour to clean up zombie POS draft invoices older than 24 hours.
     * Cron: "0 0 * * * *"
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void execute() {
        log.info("Cron Job [OrderCleanupJob] - Started POS Draft Cleanup");
        try {
            long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
            
            // Delete HoaDonChiTiet for these invoices first
            int deletedDetails = entityManager.createQuery(
                    "DELETE FROM HoaDonChiTiet hdct WHERE hdct.hoaDon.id IN " +
                    "(SELECT hd.id FROM HoaDon hd WHERE hd.trangThai = :trangThai " +
                    "AND hd.nhanVien IS NOT NULL AND hd.ngayTao < :timeLimit)")
                    .setParameter("trangThai", OrderStatus.CHO_XAC_NHAN)
                    .setParameter("timeLimit", twentyFourHoursAgo)
                    .executeUpdate();

            // Then delete the HoaDon
            int deletedInvoices = entityManager.createQuery(
                    "DELETE FROM HoaDon hd WHERE hd.trangThai = :trangThai " +
                    "AND hd.nhanVien IS NOT NULL AND hd.ngayTao < :timeLimit")
                    .setParameter("trangThai", OrderStatus.CHO_XAC_NHAN)
                    .setParameter("timeLimit", twentyFourHoursAgo)
                    .executeUpdate();

            log.info("Cron Job [OrderCleanupJob] - Finished successfully. Deleted {} zombie invoices and {} details.", deletedInvoices, deletedDetails);
        } catch (Exception e) {
            log.error("Cron Job [OrderCleanupJob] - Failed with error: {}", e.getMessage());
        }
    }
}
