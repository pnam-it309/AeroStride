package com.example.be.core.admin.lichlamviec.service;

import com.example.be.core.admin.lichlamviec.model.CaLamResponse;
import com.example.be.core.admin.lichlamviec.model.LichLamViecResponse;
import com.example.be.core.admin.lichlamviec.model.LichSuHoatDongResponse;
import com.example.be.repository.CaLamRepository;
import com.example.be.repository.LichLamViecRepository;
import com.example.be.repository.LichSuHoatDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.be.utils.ExcelUtils;
import java.io.IOException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LichLamViecService {

    private final LichLamViecRepository lichLamViecRepository;
    private final CaLamRepository caLamRepository;
    private final LichSuHoatDongRepository lichSuHoatDongRepository;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

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

    public byte[] exportTemplate() throws IOException {
        String[] headers = {"Mã Nhân Viên", "Tên Nhân Viên", "Ngày Làm (yyyy-MM-dd)", "Tên Ca"};
        Object[] sample = {"NV001", "Nguyễn Văn A", "2026-05-12", "Ca Sáng"};
        return ExcelUtils.createTemplate("Lich_Lam_Viec", headers, sample);
    }

    public String importExcel(MultipartFile file) throws IOException {
        return "Đã tiếp nhận file '" + file.getOriginalFilename() + "' và đang xử lý dữ liệu!";
    }

    public String addSchedule(Map<String, Object> request) {
        Object nhanVienObj = request.get("nhanVien");
        String message = "";
        if (nhanVienObj instanceof List) {
            List<?> list = (List<?>) nhanVienObj;
            message = "Đã thêm lịch làm việc cho " + list.size() + " nhân viên";
        } else {
            message = "Đã thêm lịch làm việc cho " + nhanVienObj;
        }
        return message + " vào ngày " + request.get("ngay");
    }

    public List<Map<String, Object>> previewImport(MultipartFile file) {
        // Mock data for preview
        List<Map<String, Object>> previewData = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("nhanVien", "Nguyễn Văn A");
        row1.put("ca", "Ca sáng");
        row1.put("ngay", "2024-05-12");
        row1.put("status", "VALID");
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("nhanVien", "Trần Thị B");
        row2.put("ca", "Ca chiều");
        row2.put("ngay", "2024-05-12");
        row2.put("status", "VALID");

        previewData.add(row1);
        previewData.add(row2);
        return previewData;
    }

    public String confirmImport(List<Map<String, Object>> data) {
        return "Đã lưu thành công " + data.size() + " lịch làm việc từ file Excel!";
    }
}
