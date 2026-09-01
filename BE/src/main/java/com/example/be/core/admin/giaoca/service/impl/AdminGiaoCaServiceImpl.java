package com.example.be.core.admin.giaoca.service.impl;

import com.example.be.core.admin.giaoca.model.request.AdminChotCaRequest;
import com.example.be.core.admin.giaoca.model.request.AdminMoCaRequest;
import com.example.be.core.admin.giaoca.model.response.AdminGiaoCaResponse;
import com.example.be.core.admin.giaoca.repository.AdminGiaoCaRepository;
import com.example.be.core.admin.giaoca.service.AdminGiaoCaService;
import com.example.be.core.admin.lichlamviec.repository.AdminLichLamViecRepository;
import com.example.be.entity.GiaoCa;
import com.example.be.entity.LichLamViec;
import com.example.be.entity.NhanVien;
import com.example.be.repository.NhanVienRepository;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.core.admin.hoadon.repository.AdminHoaDonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminGiaoCaServiceImpl implements AdminGiaoCaService {

    @Autowired
    private AdminGiaoCaRepository giaoCaRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private AdminHoaDonRepository hoaDonRepository;

    @Autowired
    private AdminLichLamViecRepository lichLamViecRepository;

    @Override
    @Transactional
    public AdminGiaoCaResponse moCa(AdminMoCaRequest request) {
        NhanVien nhanVien = nhanVienRepository.findById(request.getNhanVienId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));

        // Kiểm tra xem đã có ca nào mở chưa
        giaoCaRepository.findGiaoCaHienTai(nhanVien.getId()).ifPresent(gc -> {
            throw new RuntimeException("Nhân viên này đang có một ca chưa chốt.");
        });

        GiaoCa giaoCa = GiaoCa.builder()
                .maGiaoCa("GC" + System.currentTimeMillis())
                .nhanVienTrongCa(nhanVien)
                .thoiGianVaoCa(new Date().getTime())
                .tienBanDau(request.getTienBanDau() != null ? request.getTienBanDau() : BigDecimal.ZERO)
                .trangThai("OPEN")
                .build();

        giaoCa = giaoCaRepository.save(giaoCa);

        // Tự động liên kết và cập nhật trạng thái/giờ vào của Lịch làm việc
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<LichLamViec> schedules = lichLamViecRepository.findByNhanVienIdAndNgayLam(nhanVien.getId(), today);
        boolean linked = false;
        for (LichLamViec schedule : schedules) {
            if (schedule.getGioVao() == null) {
                schedule.setGioVao(now);
                if (schedule.getCaLam() != null && schedule.getCaLam().getGioBatDau() != null) {
                    LocalTime shiftStart = schedule.getCaLam().getGioBatDau();
                    if (now.isAfter(shiftStart.plusMinutes(15))) {
                        schedule.setTrangThaiLich(LichLamViec.TrangThaiLichLamViec.DI_MUON);
                    } else {
                        schedule.setTrangThaiLich(LichLamViec.TrangThaiLichLamViec.DUNG_GIO);
                    }
                } else {
                    schedule.setTrangThaiLich(LichLamViec.TrangThaiLichLamViec.DUNG_GIO);
                }
                lichLamViecRepository.save(schedule);
                linked = true;
                break;
            }
        }

        if (!linked && schedules.isEmpty()) {
            LichLamViec newSchedule = LichLamViec.builder()
                    .nhanVien(nhanVien)
                    .ngayLam(today)
                    .gioVao(now)
                    .trangThaiLich(LichLamViec.TrangThaiLichLamViec.DUNG_GIO)
                    .ghiChu("Tự động tạo khi mở ca")
                    .build();
            lichLamViecRepository.save(newSchedule);
        }

        return mapToResponse(giaoCa);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminGiaoCaResponse getCaHienTai(String username) {
        NhanVien nhanVien = nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(username, username, username, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));

        return giaoCaRepository.findGiaoCaHienTai(nhanVien.getId())
                .map(this::mapToResponse)
                .orElse(null); // Trả về null để FE biết chưa có ca
    }

    @Override
    @Transactional
    public AdminGiaoCaResponse chotCa(String username, AdminChotCaRequest request) {
        NhanVien nhanVien = nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(username, username, username, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));

        GiaoCa giaoCa = giaoCaRepository.findGiaoCaHienTai(nhanVien.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không có ca nào đang mở để chốt."));

        // Tính doanh thu: Tổng tiền của tất cả hóa đơn đã hoàn thành/thanh toán trong ca này
        BigDecimal tongDoanhThu = hoaDonRepository.calculateDoanhThuByGiaoCaId(giaoCa.getId());
        if (tongDoanhThu == null) tongDoanhThu = BigDecimal.ZERO;

        giaoCa.setTongDoanhThu(tongDoanhThu);
        giaoCa.setTienThucTe(request.getTienThucTe() != null ? request.getTienThucTe() : BigDecimal.ZERO);
        
        BigDecimal tienChenhLech = giaoCa.getTienThucTe().subtract(giaoCa.getTienBanDau().add(giaoCa.getTongDoanhThu()));
        giaoCa.setTienChenhLech(tienChenhLech);
        
        giaoCa.setThoiGianRaCa(new Date().getTime());
        giaoCa.setGhiChu(request.getGhiChu());
        giaoCa.setTrangThai("CLOSED");

        if (request.getNhanVienNhanCaId() != null && !request.getNhanVienNhanCaId().isEmpty()) {
            NhanVien nhanVienNhanCa = nhanVienRepository.findById(request.getNhanVienNhanCaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên nhận ca"));
            giaoCa.setNhanVienNhanCa(nhanVienNhanCa);
        }

        giaoCa = giaoCaRepository.save(giaoCa);

        // Tự động cập nhật giờ ra của Lịch làm việc
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<LichLamViec> schedules = lichLamViecRepository.findByNhanVienIdAndNgayLam(nhanVien.getId(), today);
        for (LichLamViec schedule : schedules) {
            if (schedule.getGioVao() != null && schedule.getGioRa() == null) {
                schedule.setGioRa(now);
                lichLamViecRepository.save(schedule);
                break;
            }
        }

        return mapToResponse(giaoCa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminGiaoCaResponse> getAllLichSu() {
        return giaoCaRepository.findAllOrderByThoiGianVaoCaDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AdminGiaoCaResponse mapToResponse(GiaoCa gc) {
        return AdminGiaoCaResponse.builder()
                .id(gc.getId())
                .maGiaoCa(gc.getMaGiaoCa())
                .nhanVienTrongCaId(gc.getNhanVienTrongCa() != null ? gc.getNhanVienTrongCa().getId() : null)
                .maNhanVienTrongCa(gc.getNhanVienTrongCa() != null ? gc.getNhanVienTrongCa().getMa() : null)
                .tenNhanVienTrongCa(gc.getNhanVienTrongCa() != null ? gc.getNhanVienTrongCa().getTen() : null)
                .nhanVienNhanCaId(gc.getNhanVienNhanCa() != null ? gc.getNhanVienNhanCa().getId() : null)
                .maNhanVienNhanCa(gc.getNhanVienNhanCa() != null ? gc.getNhanVienNhanCa().getMa() : null)
                .tenNhanVienNhanCa(gc.getNhanVienNhanCa() != null ? gc.getNhanVienNhanCa().getTen() : null)
                .thoiGianVaoCa(gc.getThoiGianVaoCa())
                .thoiGianRaCa(gc.getThoiGianRaCa())
                .tienBanDau(gc.getTienBanDau())
                .tongDoanhThu(gc.getTongDoanhThu())
                .tienThucTe(gc.getTienThucTe())
                .tienChenhLech(gc.getTienChenhLech())
                .ghiChu(gc.getGhiChu())
                .trangThai(gc.getTrangThai())
                .build();
    }
}
