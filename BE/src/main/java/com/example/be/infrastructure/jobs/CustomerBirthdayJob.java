package com.example.be.infrastructure.jobs;

import com.example.be.core.notification.EmailService;
import com.example.be.entity.KhachHang;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.entity.PhieuGiamGiaCaNhan;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.PhieuGiamGiaCaNhanRepository;
import com.example.be.repository.PhieuGiamGiaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CustomerBirthdayJob
 * Quét danh sách khách hàng có ngày sinh nhật hôm nay lúc 08:00 AM hàng ngày.
 * Tự động tạo mã Voucher cá nhân giảm 15% (hạn 7 ngày) và gửi Email thiệp chúc mừng sinh nhật.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerBirthdayJob {

    private final KhachHangRepository khachHangRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    private final EmailService emailService;

    // Run every day at 08:00:00 AM
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void scanAndSendBirthdayGifts() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        log.info("Cron Job [CustomerBirthdayJob] - Scanning birthday for {}/{}...", day, month);

        List<KhachHang> birthdayCustomers = khachHangRepository.findCustomersWithBirthdayToday(month, day);
        if (birthdayCustomers.isEmpty()) {
            log.info("Cron Job [CustomerBirthdayJob] - No customers having birthday today.");
            return;
        }

        long now = System.currentTimeMillis();
        long expireAt = now + (7L * 24 * 60 * 60 * 1000L); // 7 days validity

        for (KhachHang customer : birthdayCustomers) {
            try {
                if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                    continue;
                }

                // Check if already sent a birthday voucher this year
                String expectedPrefix = "BDAY" + currentYear + "-";
                List<PhieuGiamGiaCaNhan> existingPersonal = phieuGiamGiaCaNhanRepository.findByKhachHangId(customer.getId());
                boolean alreadyGifted = existingPersonal.stream()
                        .anyMatch(pgn -> pgn.getPhieuGiamGia() != null 
                                && pgn.getPhieuGiamGia().getMa() != null 
                                && pgn.getPhieuGiamGia().getMa().startsWith(expectedPrefix));

                if (alreadyGifted) {
                    log.info("Customer {} ({}) already received birthday voucher for year {}", customer.getTen(), customer.getEmail(), currentYear);
                    continue;
                }

                // Create a unique birthday voucher
                String voucherCode = expectedPrefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                String voucherName = "Quà Sinh Nhật " + currentYear + " - " + (customer.getTen() != null ? customer.getTen() : "Khách Hàng");

                PhieuGiamGia voucher = PhieuGiamGia.builder()
                        .loaiPhieu("PHAN_TRAM")
                        .hinhThuc("CA_NHAN")
                        .phanTramGiamGia(15)
                        .soLuong(1)
                        .donHangToiThieu(BigDecimal.ZERO)
                        .giamToiDa(BigDecimal.valueOf(100000))
                        .ngayBatDau(now)
                        .ngayKetThuc(expireAt)
                        .ghiChu("Voucher sinh nhật tri ân khách hàng năm " + currentYear)
                        .build();
                voucher.setMa(voucherCode);
                voucher.setTen(voucherName);
                voucher.setTrangThai(TrangThai.DANG_HOAT_DONG);
                voucher = phieuGiamGiaRepository.save(voucher);

                // Assign voucher to customer
                PhieuGiamGiaCaNhan personalVoucher = PhieuGiamGiaCaNhan.builder()
                        .phieuGiamGia(voucher)
                        .khachHang(customer)
                        .daSuDung(false)
                        .xoaMem(false)
                        .build();
                phieuGiamGiaCaNhanRepository.save(personalVoucher);

                // Send email
                emailService.guiEmailChucMungSinhNhat(
                        customer.getEmail(),
                        customer.getTen() != null ? customer.getTen() : "Quý khách",
                        voucherCode,
                        voucherName,
                        BigDecimal.valueOf(15),
                        "PERCENTAGE",
                        expireAt
                );

                log.info("Successfully issued birthday voucher {} to {}", voucherCode, customer.getEmail());
            } catch (Exception e) {
                log.error("Error sending birthday gift to customer {}: {}", customer.getEmail(), e.getMessage());
            }
        }

        log.info("Cron Job [CustomerBirthdayJob] - Finished scanning and sending birthday gifts.");
    }
}
