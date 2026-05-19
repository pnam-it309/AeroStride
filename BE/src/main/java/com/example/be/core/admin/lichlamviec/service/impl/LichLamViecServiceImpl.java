package com.example.be.core.admin.lichlamviec.service.impl;

import com.example.be.core.admin.lichlamviec.model.CaLamResponse;
import com.example.be.core.admin.lichlamviec.model.LichLamViecResponse;
import com.example.be.core.admin.lichlamviec.model.LichSuHoatDongResponse;
import com.example.be.core.admin.lichlamviec.repository.AdminCaLamRepository;
import com.example.be.core.admin.lichlamviec.repository.AdminLichLamViecRepository;
import com.example.be.core.admin.lichlamviec.repository.AdminLichSuHoatDongRepository;
import com.example.be.core.admin.lichlamviec.service.LichLamViecService;
import com.example.be.entity.CaLam;
import com.example.be.entity.LichLamViec;
import com.example.be.entity.LichSuHoatDong;
import com.example.be.entity.NhanVien;
import com.example.be.repository.NhanVienRepository;
import com.example.be.utils.ExcelUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LichLamViecServiceImpl implements LichLamViecService {

    private final AdminLichLamViecRepository lichLamViecRepository;
    private final AdminCaLamRepository caLamRepository;
    private final AdminLichSuHoatDongRepository lichSuHoatDongRepository;
    private final NhanVienRepository nhanVienRepository;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    @Transactional(readOnly = true)
    public List<LichLamViecResponse> getAllSchedules() {
        return lichLamViecRepository.findAll().stream()
                .map(l -> LichLamViecResponse.builder()
                        .id(l.getId())
                        .nhanVien(l.getNhanVien() != null ? l.getNhanVien().getTen() : "N/A")
                        .ca(l.getCaLam() != null ? l.getCaLam().getTenCa() : "N/A")
                        .ngay(l.getNgayLam().format(dateFormatter))
                        .trangThai(l.getTrangThaiLich() != null ? l.getTrangThaiLich().name() : "N/A")
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaLamResponse> getAllShifts() {
        return caLamRepository.findByXoaMemFalse().stream()
                .map(c -> CaLamResponse.builder()
                        .id(c.getId())
                        .tenCa(c.getTenCa())
                        .gioBatDau(c.getGioBatDau().format(timeFormatter))
                        .gioKetThuc(c.getGioKetThuc().format(timeFormatter))
                        .moTa(c.getMoTa())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LichSuHoatDongResponse> getActivityHistory(Pageable pageable) {
        return lichSuHoatDongRepository.findAllByOrderByNgayTaoDesc(pageable)
                .map(h -> LichSuHoatDongResponse.builder()
                        .id(h.getId())
                        .nguoiThucHien(h.getNguoiTao())
                        .hanhDong(h.getHanhDong())
                        .doiTuong(h.getDoiTuong())
                        .ngay(h.getNgayTao() != null ? 
                                Instant.ofEpochMilli(h.getNgayTao()).atZone(ZoneId.systemDefault()).format(dateTimeFormatter) : "N/A")
                        .build());
    }

    @Override
    public byte[] exportTemplate() throws IOException {
        String[] headers = {"Mã Nhân Viên", "Tên Nhân Viên", "Ngày Làm (yyyy-MM-dd)", "Tên Ca"};
        Object[] sample = {"NV001", "Nguyễn Văn A", "2026-05-12", "Ca Sáng"};
        return ExcelUtils.createTemplate("Lich_Lam_Viec", headers, sample);
    }

    @Override
    public String importExcel(MultipartFile file) throws IOException {
        List<Map<String, Object>> previewData = previewImport(file);
        return confirmImport(previewData);
    }

    @Override
    @Transactional
    public String addSchedule(Map<String, Object> request) {
        Object nhanVienObj = request.get("nhanVien");
        String caName = (String) request.get("ca");
        String ngayStr = (String) request.get("ngay");
        String trangThaiStr = (String) request.get("trangThai");

        LocalDate ngayLam = LocalDate.parse(ngayStr);
        LichLamViec.TrangThaiLichLamViec trangThai = LichLamViec.TrangThaiLichLamViec.CHO_XAC_NHAN;
        if (trangThaiStr != null) {
            try {
                trangThai = LichLamViec.TrangThaiLichLamViec.valueOf(trangThaiStr);
            } catch (Exception e) {
                // Ignore
            }
        }

        final String finalCaName = caName;
        CaLam caLam = caLamRepository.findByXoaMemFalse().stream()
                .filter(c -> c.getTenCa().equalsIgnoreCase(finalCaName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Ca làm việc không tồn tại: " + finalCaName));

        List<String> nhanVienIds = new ArrayList<>();
        if (nhanVienObj instanceof List) {
            for (Object obj : (List<?>) nhanVienObj) {
                nhanVienIds.add(String.valueOf(obj));
            }
        } else if (nhanVienObj != null) {
            nhanVienIds.add(String.valueOf(nhanVienObj));
        }

        List<NhanVien> nhanViens = nhanVienRepository.findAllById(nhanVienIds);
        if (nhanViens.isEmpty()) {
            throw new RuntimeException("Không tìm thấy nhân viên hợp lệ!");
        }

        for (NhanVien nv : nhanViens) {
            LichLamViec schedule = LichLamViec.builder()
                    .nhanVien(nv)
                    .caLam(caLam)
                    .ngayLam(ngayLam)
                    .trangThaiLich(trangThai)
                    .build();
            lichLamViecRepository.save(schedule);

            // Log activity history
            LichSuHoatDong activity = LichSuHoatDong.builder()
                    .hanhDong("Tạo lịch làm việc")
                    .doiTuong("Nhân viên " + nv.getTen() + " (" + nv.getMa() + ") - Ngày " + ngayStr + " (" + caName + ")")
                    .build();
            lichSuHoatDongRepository.save(activity);
        }

        return "Đã thêm lịch làm việc cho " + nhanViens.size() + " nhân viên vào ngày " + ngayStr;
    }

    @Override
    public List<Map<String, Object>> previewImport(MultipartFile file) {
        List<Map<String, Object>> previewData = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String maNhanVien = ExcelUtils.getCellValueAsString(row.getCell(0)).trim();
                String tenNhanVien = ExcelUtils.getCellValueAsString(row.getCell(1)).trim();
                String ngayLamStr = ExcelUtils.getCellValueAsString(row.getCell(2)).trim();
                String tenCa = ExcelUtils.getCellValueAsString(row.getCell(3)).trim();

                if (maNhanVien.isEmpty() && tenCa.isEmpty()) continue;

                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("maNhanVien", maNhanVien);
                rowMap.put("nhanVien", tenNhanVien);
                rowMap.put("ca", tenCa);
                rowMap.put("ngay", ngayLamStr);

                // Validate
                String status = "VALID";
                StringBuilder errorMsg = new StringBuilder();

                // 1. Kiểm tra nhân viên tồn tại
                NhanVien nv = nhanVienRepository.findByMa(maNhanVien).orElse(null);
                if (nv == null) {
                    status = "INVALID";
                    errorMsg.append("Nhân viên không tồn tại. ");
                } else {
                    rowMap.put("nhanVien", nv.getTen()); // Dùng tên chuẩn từ CSDL
                    rowMap.put("nhanVienId", nv.getId());
                }

                // 2. Kiểm tra ca làm tồn tại
                final String finalTenCa = tenCa;
                CaLam ca = caLamRepository.findByXoaMemFalse().stream()
                        .filter(c -> c.getTenCa().equalsIgnoreCase(finalTenCa))
                        .findFirst()
                        .orElse(null);
                if (ca == null) {
                    status = "INVALID";
                    errorMsg.append("Ca làm việc không tồn tại. ");
                } else {
                    rowMap.put("caId", ca.getId());
                }

                // 3. Kiểm tra ngày tháng
                try {
                    if (ngayLamStr.contains(" ")) {
                        ngayLamStr = ngayLamStr.split(" ")[0]; // Bỏ phần giờ nếu có
                    }
                    LocalDate.parse(ngayLamStr);
                    rowMap.put("ngay", ngayLamStr);
                } catch (Exception e) {
                    status = "INVALID";
                    errorMsg.append("Ngày làm không đúng định dạng yyyy-MM-dd. ");
                }

                rowMap.put("status", status);
                rowMap.put("message", errorMsg.toString().trim());
                previewData.add(rowMap);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file Excel: " + e.getMessage());
        }
        return previewData;
    }

    @Override
    @Transactional
    public String confirmImport(List<Map<String, Object>> data) {
        int successCount = 0;
        for (Map<String, Object> row : data) {
            String status = (String) row.get("status");
            if (!"VALID".equals(status)) continue;

            String maNhanVien = (String) row.get("maNhanVien");
            String caName = (String) row.get("ca");
            String ngayStr = (String) row.get("ngay");

            NhanVien nv = nhanVienRepository.findByMa(maNhanVien).orElse(null);
            final String finalCaName = caName;
            CaLam caLam = caLamRepository.findByXoaMemFalse().stream()
                    .filter(c -> c.getTenCa().equalsIgnoreCase(finalCaName))
                    .findFirst()
                    .orElse(null);

            if (nv != null && caLam != null) {
                LocalDate ngayLam = LocalDate.parse(ngayStr);

                LichLamViec schedule = LichLamViec.builder()
                        .nhanVien(nv)
                        .caLam(caLam)
                        .ngayLam(ngayLam)
                        .trangThaiLich(LichLamViec.TrangThaiLichLamViec.DA_XAC_NHAN)
                        .build();
                lichLamViecRepository.save(schedule);

                // Log activity history
                LichSuHoatDong activity = LichSuHoatDong.builder()
                        .hanhDong("Import lịch làm việc")
                        .doiTuong("Nhân viên " + nv.getTen() + " (" + nv.getMa() + ") - Ngày " + ngayStr + " (" + caName + ")")
                        .build();
                lichSuHoatDongRepository.save(activity);

                successCount++;
            }
        }
        return "Đã lưu thành công " + successCount + " lịch làm việc từ file Excel!";
    }

    @Override
    @Transactional
    public void deleteSchedule(String id) {
        lichLamViecRepository.deleteById(id);
    }
}
