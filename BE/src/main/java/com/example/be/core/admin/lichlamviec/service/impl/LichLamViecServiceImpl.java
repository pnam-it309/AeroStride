package com.example.be.core.admin.lichlamviec.service.impl;

import com.example.be.core.admin.lichlamviec.model.request.CaLamRequest;
import com.example.be.core.admin.lichlamviec.model.request.LichLamViecRequest;
import com.example.be.core.admin.lichlamviec.model.request.AutoScheduleRequest;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.constants.VaiTro;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public List<LichLamViecResponse> getAllSchedules(String search, String ca, String ngay) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanCa = (ca != null && !ca.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(ca.trim())) ? ca.trim() : null;
        LocalDate cleanNgay = null;
        if (ngay != null && !ngay.trim().isEmpty()) {
            try {
                cleanNgay = LocalDate.parse(ngay.trim(), dateFormatter);
            } catch (Exception ignored) {}
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + VaiTro.STAFF) || a.getAuthority().equals(VaiTro.STAFF));
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + VaiTro.ADMIN) || a.getAuthority().equals(VaiTro.ADMIN));

        String staffId = null;
        if (isStaff && !isAdmin && auth != null) {
            String identifier = auth.getName();
            if (identifier != null && identifier.contains(":")) {
                identifier = identifier.substring(identifier.indexOf(":") + 1);
            }
            Optional<NhanVien> currentNv = nhanVienRepository.findCurrentProfileByIdentifier(identifier);
            if (currentNv.isPresent()) {
                staffId = currentNv.get().getId();
            }
        }

        final String finalStaffId = staffId;

        return lichLamViecRepository.searchScheduleProjections(cleanSearch, cleanCa, cleanNgay).stream()
                .filter(l -> finalStaffId == null || (l.getNhanVienId() != null && finalStaffId.equals(l.getNhanVienId())))
                .map(l -> LichLamViecResponse.builder()
                        .id(l.getId())
                        .nhanVien(l.getTenNhanVien() != null ? l.getTenNhanVien() : "N/A")
                        .nhanVienId(l.getNhanVienId())
                        .maNhanVien(l.getMaNhanVien() != null ? l.getMaNhanVien() : "N/A")
                        .ca(l.getTenCa() != null ? l.getTenCa() : "N/A")
                        .caId(l.getCaId())
                        .ngay(l.getNgayLam() != null ? l.getNgayLam().format(dateFormatter) : "")
                        .trangThai(resolveTrangThaiString(l.getTrangThaiLich()))
                        .tangCa(l.getTangCa() != null ? l.getTangCa() : false)
                        .gioBatDauTangCa(l.getGioBatDauTangCa() != null ? l.getGioBatDauTangCa().format(timeFormatter) : null)
                        .gioKetThucTangCa(l.getGioKetThucTangCa() != null ? l.getGioKetThucTangCa().format(timeFormatter) : null)
                        .gioVao(l.getGioVao() != null ? l.getGioVao().format(timeFormatter) : null)
                        .gioRa(l.getGioRa() != null ? l.getGioRa().format(timeFormatter) : null)
                        .tongSoGio(calculateTotalHours(l.getGioVao(), l.getGioRa(), l.getGioBatDauCa(), l.getGioKetThucCa()))
                        .ghiChu(l.getGhiChu())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveTrangThaiString(LichLamViec.TrangThaiLichLamViec status) {
        if (status == null) return "CHUA_VAO_CA";
        return switch (status) {
            case DUNG_GIO, DA_XAC_NHAN -> "DUNG_GIO";
            case DI_MUON -> "DI_MUON";
            case CHUA_VAO_CA, CHO_XAC_NHAN, DA_HUY -> "CHUA_VAO_CA";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaLamResponse> getAllShifts() {
        return caLamRepository.findByXoaMemFalse().stream()
                .map(c -> CaLamResponse.builder()
                        .id(c.getId())
                        .tenCa(c.getTenCa())
                        .gioBatDau(c.getGioBatDau() != null ? c.getGioBatDau().format(timeFormatter) : "")
                        .gioKetThuc(c.getGioKetThuc() != null ? c.getGioKetThuc().format(timeFormatter) : "")
                        .moTa(c.getMoTa())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LichSuHoatDongResponse> getActivityHistory(String search, String ngay, Pageable pageable) {
        Long ngayBatDau = null;
        Long ngayKetThuc = null;
        if (ngay != null && !ngay.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(ngay, dateFormatter);
                ngayBatDau = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                ngayKetThuc = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception e) {
                // Ignore invalid date
            }
        }
        return lichSuHoatDongRepository.searchActivities(search, ngayBatDau, ngayKetThuc, pageable)
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

    private LocalDate parseSafeDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new RuntimeException("Ngày làm việc không được để trống!");
        }
        String clean = dateStr.trim();
        try {
            if (clean.contains("T")) {
                clean = clean.substring(0, clean.indexOf("T"));
            }
            if (clean.contains("/")) {
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            return LocalDate.parse(clean, dateFormatter);
        } catch (Exception e) {
            try {
                return LocalDate.parse(clean);
            } catch (Exception ex) {
                throw new RuntimeException("Định dạng ngày không hợp lệ (hỗ trợ yyyy-MM-dd hoặc dd/MM/yyyy)!");
            }
        }
    }

    @Override
    @Transactional
    public String addSchedule(LichLamViecRequest request) {
        List<String> nhanVienIds = request.getNhanVien();
        List<String> caNames = request.getCa();
        String ngayStr = request.getNgay();
        String trangThaiStr = request.getTrangThai();

        LocalDate ngayLam = parseSafeDate(ngayStr);
        LichLamViec.TrangThaiLichLamViec trangThai = LichLamViec.TrangThaiLichLamViec.CHUA_VAO_CA;
        if (trangThaiStr != null && !trangThaiStr.trim().isEmpty()) {
            try {
                trangThai = LichLamViec.TrangThaiLichLamViec.valueOf(trangThaiStr.trim().toUpperCase());
            } catch (Exception e) {
                // Ignore
            }
        }

        if (caNames == null || caNames.isEmpty()) {
            throw new RuntimeException("Danh sách ca làm không được để trống!");
        }

        List<CaLam> allShifts = caLamRepository.findByXoaMemFalse();
        List<CaLam> caLams = allShifts.stream()
                .filter(c -> caNames.stream().anyMatch(name -> {
                    if (name == null) return false;
                    String trimmed = name.trim();
                    return trimmed.equalsIgnoreCase(c.getTenCa() != null ? c.getTenCa().trim() : "") ||
                            trimmed.equals(c.getId());
                }))
                .toList();

        if (caLams.isEmpty()) {
            throw new RuntimeException("Không tìm thấy ca làm việc hợp lệ!");
        }

        if (nhanVienIds == null || nhanVienIds.isEmpty()) {
            throw new RuntimeException("Danh sách nhân viên không được để trống!");
        }

        List<NhanVien> allEmployees = nhanVienRepository.findAll();
        List<NhanVien> nhanViens = allEmployees.stream()
                .filter(nv -> nhanVienIds.stream().anyMatch(id -> {
                    if (id == null) return false;
                    String trimmed = id.trim();
                    return trimmed.equals(nv.getId()) ||
                            trimmed.equalsIgnoreCase(nv.getMa() != null ? nv.getMa().trim() : "") ||
                            trimmed.equalsIgnoreCase(nv.getTenTaiKhoan() != null ? nv.getTenTaiKhoan().trim() : "");
                }))
                .toList();

        if (nhanViens.isEmpty()) {
            throw new RuntimeException("Không tìm thấy nhân viên hợp lệ!");
        }

        Boolean tangCa = request.getTangCa() != null && request.getTangCa();
        LocalTime gioBatDauTangCa = null;
        LocalTime gioKetThucTangCa = null;
        if (tangCa && request.getGioBatDauTangCa() != null && request.getGioKetThucTangCa() != null) {
            try {
                gioBatDauTangCa = LocalTime.parse(request.getGioBatDauTangCa().trim(), timeFormatter);
                gioKetThucTangCa = LocalTime.parse(request.getGioKetThucTangCa().trim(), timeFormatter);
            } catch (Exception e) {
                // Ignore
            }
        }

        int scheduleCount = 0;
        for (NhanVien nv : nhanViens) {
            for (CaLam caLam : caLams) {
                Optional<LichLamViec> existing = lichLamViecRepository
                        .findByNhanVienIdAndCaLamIdAndNgayLam(nv.getId(), caLam.getId(), ngayLam);

                LichLamViec schedule = existing.orElseGet(() -> LichLamViec.builder()
                        .nhanVien(nv)
                        .caLam(caLam)
                        .ngayLam(ngayLam)
                        .build());

                schedule.setTrangThaiLich(trangThai);
                schedule.setTangCa(tangCa);
                schedule.setGioBatDauTangCa(gioBatDauTangCa);
                schedule.setGioKetThucTangCa(gioKetThucTangCa);
                if (schedule.getTrangThai() == null) {
                    schedule.setTrangThai(TrangThai.DANG_HOAT_DONG);
                }
                lichLamViecRepository.save(schedule);

                try {
                    // Log activity history
                    LichSuHoatDong activity = LichSuHoatDong.builder()
                            .hanhDong(existing.isPresent() ? "Cập nhật lịch làm việc" : "Tạo lịch làm việc")
                            .doiTuong("Nhân viên " + nv.getTen() + " (" + nv.getMa() + ") - Ngày " + ngayLam + " (" + caLam.getTenCa() + ")")
                            .build();
                    activity.setTrangThai(TrangThai.DANG_HOAT_DONG);
                    lichSuHoatDongRepository.save(activity);
                } catch (Exception ex) {
                    // Non-blocking for activity log
                }
                scheduleCount++;
            }
        }

        return "Đã thêm " + scheduleCount + " lịch làm việc thành công!";
    }

    @Override
    @Transactional
    public String autoSchedule(AutoScheduleRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("Khoảng thời gian không được để trống!");
        }

        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(request.getStartDate(), dateFormatter);
            end = LocalDate.parse(request.getEndDate(), dateFormatter);
        } catch (Exception e) {
            throw new RuntimeException("Định dạng ngày không hợp lệ!");
        }

        if (start.isAfter(end)) {
            throw new RuntimeException("Ngày bắt đầu phải trước ngày kết thúc!");
        }

        // 1. Xóa lịch cũ trong khoảng thời gian này
        List<LichLamViec> oldSchedules = lichLamViecRepository.findByNgayLamBetween(start, end);
        if (!oldSchedules.isEmpty()) {
            lichLamViecRepository.deleteAll(oldSchedules);
        }

        // 2. Lấy danh sách nhân viên đang hoạt động (loại trừ các tài khoản quản lý/admin)
        List<NhanVien> activeEmployees = nhanVienRepository.findAll().stream()
                .filter(nv -> (nv.getXoaMem() == null || !nv.getXoaMem()) && nv.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .filter(nv -> !VaiTro.isManagementRole(nv))
                .collect(Collectors.toList());

        if (activeEmployees.isEmpty()) {
            throw new RuntimeException("Không tìm thấy nhân viên đang hoạt động nào trong hệ thống!");
        }

        // 3. Lấy danh sách ca làm việc chưa bị xóa
        List<CaLam> shifts = caLamRepository.findByXoaMemFalse();
        if (shifts.isEmpty()) {
            throw new RuntimeException("Không tìm thấy ca làm việc nào đang hoạt động!");
        }

        // 4. Trộn ngẫu nhiên nhân viên
        List<NhanVien> shuffledEmployees = new ArrayList<>(activeEmployees);
        java.util.Collections.shuffle(shuffledEmployees);

        // 5. Tạo danh sách các ngày trong tuần
        List<LocalDate> daysInWeek = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            daysInWeek.add(current);
            current = current.plusDays(1);
        }

        int empIndex = 0;
        int numEmployees = shuffledEmployees.size();
        List<LichLamViec> newSchedules = new ArrayList<>();

        for (LocalDate day : daysInWeek) {
            for (CaLam shift : shifts) {
                NhanVien assignedEmployee = shuffledEmployees.get(empIndex);

                LichLamViec schedule = LichLamViec.builder()
                        .nhanVien(assignedEmployee)
                        .caLam(shift)
                        .ngayLam(day)
                        .trangThaiLich(LichLamViec.TrangThaiLichLamViec.CHUA_VAO_CA)
                        .tangCa(false)
                        .build();

                newSchedules.add(schedule);

                // Xoay vòng nhân viên
                empIndex = (empIndex + 1) % numEmployees;
            }
        }

        lichLamViecRepository.saveAll(newSchedules);

        // 6. Ghi log hoạt động
        LichSuHoatDong activity = LichSuHoatDong.builder()
                .hanhDong("Xếp ca tự động")
                .doiTuong("Tuần từ " + request.getStartDate() + " đến " + request.getEndDate() + " (" + newSchedules.size() + " ca làm việc)")
                .build();
        lichSuHoatDongRepository.save(activity);

        return "Đã xếp ca tự động thành công cho " + newSchedules.size() + " ca làm việc trong tuần!";
    }

    @Override
    @Transactional
    public String updateSchedule(String id, LichLamViecRequest request) {
        LichLamViec schedule = lichLamViecRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc với ID: " + id));

        List<String> nhanVienIds = request.getNhanVien();
        String ngayStr = request.getNgay();
        String trangThaiStr = request.getTrangThai();

        if (ngayStr != null) {
            schedule.setNgayLam(parseSafeDate(ngayStr));
        }

        if (trangThaiStr != null) {
            try {
                schedule.setTrangThaiLich(LichLamViec.TrangThaiLichLamViec.valueOf(trangThaiStr));
            } catch (Exception e) {
                // Ignore
            }
        }

        String caName = (request.getCa() != null && !request.getCa().isEmpty()) ? request.getCa().get(0) : null;
        if (caName != null) {
            final String finalCaName = caName;
            CaLam caLam = caLamRepository.findByXoaMemFalse().stream()
                    .filter(c -> c.getTenCa().equalsIgnoreCase(finalCaName) || c.getId().equals(finalCaName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Ca làm việc không tồn tại: " + finalCaName));
            schedule.setCaLam(caLam);
        }

        if (nhanVienIds != null && !nhanVienIds.isEmpty()) {
            String firstId = nhanVienIds.get(0);
            NhanVien nv = nhanVienRepository.findById(firstId)
                    .orElseGet(() -> nhanVienRepository.findAll().stream()
                            .filter(n -> firstId.equals(n.getMa()) || firstId.equals(n.getTenTaiKhoan()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + firstId)));
            schedule.setNhanVien(nv);
        }

        if (request.getTangCa() != null) {
            schedule.setTangCa(request.getTangCa());
            if (request.getTangCa() && request.getGioBatDauTangCa() != null && request.getGioKetThucTangCa() != null) {
                try {
                    schedule.setGioBatDauTangCa(LocalTime.parse(request.getGioBatDauTangCa(), timeFormatter));
                    schedule.setGioKetThucTangCa(LocalTime.parse(request.getGioKetThucTangCa(), timeFormatter));
                } catch (Exception e) {
                    // Ignore
                }
            } else {
                schedule.setGioBatDauTangCa(null);
                schedule.setGioKetThucTangCa(null);
            }
        }

        lichLamViecRepository.save(schedule);

        // Log activity history
        LichSuHoatDong activity = LichSuHoatDong.builder()
                .hanhDong("Cập nhật lịch làm việc")
                .doiTuong("Nhân viên " + schedule.getNhanVien().getTen() + " (" + schedule.getNhanVien().getMa() + ") - Ngày " + schedule.getNgayLam() + " (" + schedule.getCaLam().getTenCa() + ")")
                .build();
        lichSuHoatDongRepository.save(activity);

        return "Cập nhật lịch làm việc thành công!";
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
                        .trangThaiLich(LichLamViec.TrangThaiLichLamViec.CHUA_VAO_CA)
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

    @Override
    @Transactional
    public String processFaceAttendance(org.springframework.web.multipart.MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new RuntimeException("Hình ảnh không hợp lệ");
        }
        
        NhanVien matchedEmployee = null;
        double maxSimilarity = 0.0;
        
        try {
            for (NhanVien nv : nhanVienRepository.findAll()) {
                if (nv.getFaceEncoding() != null && !nv.getFaceEncoding().isEmpty()) {
                    double similarity = com.example.be.utils.ImageComparisonUtil.compareImage(image, nv.getFaceEncoding());
                    if (similarity > maxSimilarity) {
                        maxSimilarity = similarity;
                        matchedEmployee = nv;
                    }
                }
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Lỗi khi so sánh hình ảnh", e);
        }
        
        if (matchedEmployee == null || maxSimilarity < 85.0) {
            throw new RuntimeException("Khuôn mặt không khớp với bất kỳ nhân viên nào trong hệ thống (Độ giống cao nhất: " + String.format("%.2f", maxSimilarity) + "%)");
        }
                
        // Now automatically clock in/out for today
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        
        final NhanVien finalMatchedEmployee = matchedEmployee;
        
        // Find if this employee already has an open attendance today
        LichLamViec activeShift = lichLamViecRepository
                .findFirstByNhanVienIdAndNgayLamAndGioVaoIsNotNullAndGioRaIsNull(finalMatchedEmployee.getId(), today)
                .orElse(null);
                
        if (activeShift != null) {
            // Check out
            activeShift.setGioRa(nowTime);
            lichLamViecRepository.save(activeShift);
            
            LichSuHoatDong activity = LichSuHoatDong.builder()
                .hanhDong("Chấm công bằng khuôn mặt (Checkout)")
                .doiTuong("Nhân viên " + matchedEmployee.getTen() + " (" + matchedEmployee.getMa() + ")")
                .build();
            lichSuHoatDongRepository.save(activity);
            
            return "Đã nhận diện: " + matchedEmployee.getTen() + ". Cập nhật giờ ra thành công lúc " + timeFormatter.format(nowTime) + "!";
        } else {
            // Check in
            LichLamViec schedule = LichLamViec.builder()
                .nhanVien(matchedEmployee)
                .ngayLam(today)
                .gioVao(nowTime)
                .trangThaiLich(LichLamViec.TrangThaiLichLamViec.DUNG_GIO)
                .ghiChu("Chấm công tự động qua khuôn mặt")
                .build();
            lichLamViecRepository.save(schedule);
            
            LichSuHoatDong activity = LichSuHoatDong.builder()
                .hanhDong("Chấm công bằng khuôn mặt (Checkin)")
                .doiTuong("Nhân viên " + matchedEmployee.getTen() + " (" + matchedEmployee.getMa() + ")")
                .build();
            lichSuHoatDongRepository.save(activity);
            
            return "Đã nhận diện: " + matchedEmployee.getTen() + ". Ghi nhận giờ vào thành công lúc " + timeFormatter.format(nowTime) + "!";
        }
    }


    @Override
    @Transactional
    public String saveAttendance(com.example.be.core.admin.lichlamviec.model.request.AttendanceRequest request) {
        if (request.getNhanVienId() == null || request.getNgay() == null) {
            throw new RuntimeException("Vui lòng chọn nhân viên và ngày làm!");
        }

        NhanVien nv = nhanVienRepository.findById(request.getNhanVienId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên!"));

        LocalDate ngayLam = LocalDate.parse(request.getNgay(), dateFormatter);

        LichLamViec schedule = LichLamViec.builder()
                .nhanVien(nv)
                .ngayLam(ngayLam)
                .trangThaiLich(LichLamViec.TrangThaiLichLamViec.DUNG_GIO)
                .build();

        if (request.getGioVao() != null && !request.getGioVao().isEmpty()) {
            schedule.setGioVao(LocalTime.parse(request.getGioVao(), timeFormatter));
        }
        if (request.getGioRa() != null && !request.getGioRa().isEmpty()) {
            schedule.setGioRa(LocalTime.parse(request.getGioRa(), timeFormatter));
        }
        schedule.setGhiChu(request.getGhiChu());

        lichLamViecRepository.save(schedule);

        LichSuHoatDong activity = LichSuHoatDong.builder()
                .hanhDong("Chấm công")
                .doiTuong("Nhân viên " + nv.getTen() + " (" + nv.getMa() + ") - Ngày " + request.getNgay())
                .build();
        lichSuHoatDongRepository.save(activity);

        return "Ghi nhận chấm công thành công!";
    }

    @Override
    @Transactional
    public String updateAttendance(String id, com.example.be.core.admin.lichlamviec.model.request.AttendanceRequest request) {
        LichLamViec schedule = lichLamViecRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc với ID: " + id));

        if (request.getGioVao() != null && !request.getGioVao().isEmpty()) {
            schedule.setGioVao(LocalTime.parse(request.getGioVao(), timeFormatter));
        }
        if (request.getGioRa() != null && !request.getGioRa().isEmpty()) {
            schedule.setGioRa(LocalTime.parse(request.getGioRa(), timeFormatter));
        }
        schedule.setGhiChu(request.getGhiChu());

        lichLamViecRepository.save(schedule);

        LichSuHoatDong activity = LichSuHoatDong.builder()
                .hanhDong("Cập nhật chấm công")
                .doiTuong("Nhân viên " + schedule.getNhanVien().getTen() + " - Ngày " + schedule.getNgayLam())
                .build();
        lichSuHoatDongRepository.save(activity);

        return "Cập nhật chấm công thành công!";
    }

    // Private helper for character & string validation
    private String validateAndTrimString(String input, String fieldName, int maxLength, boolean allowEmpty) {
        if (input == null || input.trim().isEmpty()) {
            if (!allowEmpty) {
                throw new RuntimeException(fieldName + " không được để trống!");
            }
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.length() > maxLength) {
            throw new RuntimeException(fieldName + " không được vượt quá " + maxLength + " ký tự!");
        }
        if (trimmed.contains("<") || trimmed.contains(">") || trimmed.toLowerCase().contains("script")) {
            throw new RuntimeException(fieldName + " chứa ký tự không hợp lệ!");
        }
        return trimmed;
    }

    // Shift (Ca Lam) CRUD implementations
    @Override
    @Transactional
    public String createShift(CaLamRequest request) {
        String tenCa = validateAndTrimString(request.getTenCa(), "Tên ca làm việc", 50, false);
        String moTa = validateAndTrimString(request.getMoTa(), "Mô tả ca làm việc", 255, true);

        String gioBatDauStr = request.getGioBatDau();
        String gioKetThucStr = request.getGioKetThuc();

        if (gioBatDauStr == null || gioBatDauStr.trim().isEmpty()) {
            throw new RuntimeException("Giờ bắt đầu không được để trống!");
        }
        if (gioKetThucStr == null || gioKetThucStr.trim().isEmpty()) {
            throw new RuntimeException("Giờ kết thúc không được để trống!");
        }

        LocalTime gioBatDau;
        LocalTime gioKetThuc;
        try {
            gioBatDau = LocalTime.parse(gioBatDauStr.trim(), timeFormatter);
            gioKetThuc = LocalTime.parse(gioKetThucStr.trim(), timeFormatter);
        } catch (Exception e) {
            throw new RuntimeException("Định dạng giờ không hợp lệ (định dạng chuẩn HH:mm)!");
        }

        boolean exists = caLamRepository.findByXoaMemFalse().stream()
                .anyMatch(c -> c.getTenCa().equalsIgnoreCase(tenCa));
        if (exists) {
            throw new RuntimeException("Tên ca làm việc đã tồn tại!");
        }

        CaLam caLam = CaLam.builder()
                .tenCa(tenCa)
                .gioBatDau(gioBatDau)
                .gioKetThuc(gioKetThuc)
                .moTa(moTa)
                .xoaMem(false)
                .build();

        caLamRepository.save(caLam);

        LichSuHoatDong activity = LichSuHoatDong.builder()
                .hanhDong("Tạo ca làm việc")
                .doiTuong("Ca " + tenCa + " (" + gioBatDauStr + " - " + gioKetThucStr + ")")
                .build();
        lichSuHoatDongRepository.save(activity);

        return "Tạo ca làm việc thành công!";
    }

    @Override
    @Transactional
    public String updateShift(String id, CaLamRequest request) {
        CaLam caLam = caLamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc với ID: " + id));

        String tenCa = validateAndTrimString(request.getTenCa(), "Tên ca làm việc", 50, false);
        String moTa = validateAndTrimString(request.getMoTa(), "Mô tả ca làm việc", 255, true);

        String gioBatDauStr = request.getGioBatDau();
        String gioKetThucStr = request.getGioKetThuc();

        if (gioBatDauStr == null || gioBatDauStr.trim().isEmpty()) {
            throw new RuntimeException("Giờ bắt đầu không được để trống!");
        }
        if (gioKetThucStr == null || gioKetThucStr.trim().isEmpty()) {
            throw new RuntimeException("Giờ kết thúc không được để trống!");
        }

        LocalTime gioBatDau;
        LocalTime gioKetThuc;
        try {
            gioBatDau = LocalTime.parse(gioBatDauStr.trim(), timeFormatter);
            gioKetThuc = LocalTime.parse(gioKetThucStr.trim(), timeFormatter);
        } catch (Exception e) {
            throw new RuntimeException("Định dạng giờ không hợp lệ (định dạng chuẩn HH:mm)!");
        }

        boolean exists = caLamRepository.findByXoaMemFalse().stream()
                .anyMatch(c -> !c.getId().equals(id) && c.getTenCa().equalsIgnoreCase(tenCa));
        if (exists) {
            throw new RuntimeException("Tên ca làm việc đã tồn tại!");
        }

        caLam.setTenCa(tenCa);
        caLam.setGioBatDau(gioBatDau);
        caLam.setGioKetThuc(gioKetThuc);
        caLam.setMoTa(moTa);

        caLamRepository.save(caLam);

        LichSuHoatDong activity = LichSuHoatDong.builder()
                .hanhDong("Cập nhật ca làm việc")
                .doiTuong("Ca " + tenCa + " (" + gioBatDauStr + " - " + gioKetThucStr + ")")
                .build();
        lichSuHoatDongRepository.save(activity);

        return "Cập nhật ca làm việc thành công!";
    }

    @Override
    @Transactional
    public void deleteShift(String id) {
        CaLam caLam = caLamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc với ID: " + id));

        caLam.setXoaMem(true);
        caLamRepository.save(caLam);

        LichSuHoatDong activity = LichSuHoatDong.builder()
                .hanhDong("Xóa ca làm việc")
                .doiTuong("Ca " + caLam.getTenCa())
                .build();
        lichSuHoatDongRepository.save(activity);
    }

    private String calculateTotalHours(LocalTime gioVao, LocalTime gioRa, LocalTime caGioBatDau, LocalTime caGioKetThuc) {
        if (gioVao != null && gioRa != null) {
            long minutes = java.time.Duration.between(gioVao, gioRa).toMinutes();
            if (minutes < 0) {
                minutes += 24 * 60;
            }
            long h = minutes / 60;
            long m = minutes % 60;
            if (h == 0) return m + " phút";
            if (m == 0) return h + " giờ";
            return h + "h " + m + "p";
        }
        if (caGioBatDau != null && caGioKetThuc != null) {
            long minutes = java.time.Duration.between(caGioBatDau, caGioKetThuc).toMinutes();
            if (minutes < 0) {
                minutes += 24 * 60;
            }
            long h = minutes / 60;
            long m = minutes % 60;
            if (h == 0) return m + " phút";
            if (m == 0) return h + " giờ";
            return h + "h " + m + "p";
        }
        return "--";
    }

    private String calculateTotalHours(LichLamViec l) {
        if (l == null) return "--";
        LocalTime caGioBatDau = l.getCaLam() != null ? l.getCaLam().getGioBatDau() : null;
        LocalTime caGioKetThuc = l.getCaLam() != null ? l.getCaLam().getGioKetThuc() : null;
        return calculateTotalHours(l.getGioVao(), l.getGioRa(), caGioBatDau, caGioKetThuc);
    }
}
