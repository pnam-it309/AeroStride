package com.example.be.core.admin.phieugiamgia.service.impl;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import com.example.be.core.admin.phieugiamgia.repository.AdminPhieuGiamGiaRepository;
import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import com.example.be.core.notification.EmailService;
import com.example.be.core.notification.dto.EmailRequest;
import com.example.be.entity.KhachHang;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.entity.PhieuGiamGiaCaNhan;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.infrastructure.exceptions.SystemException;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.PhieuGiamGiaCaNhanRepository;
import com.example.be.utils.AccountUtils;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.PaginationUtils;
import com.example.be.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminPhieuGiamGiaServiceImpl implements AdminPhieuGiamGiaService {

    private final AdminPhieuGiamGiaRepository repo;
    private final KhachHangRepository khachHangRepository;
    private final PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    private final EmailService emailService;

    @Override
    public List<AdminPhieuGiamGiaResponse> hienThi() {
        return repo.hienThi();
    }

    @Override
    public AdminPhieuGiamGiaResponse detail(String id) {
        AdminPhieuGiamGiaResponse res = repo.detail(id);
        if (res == null) {
            throw new ResourceNotFoundException("Không tìm thấy phiếu giảm giá với id: " + id);
        }
        return res;
    }

    @Override
    public Page<AdminPhieuGiamGiaResponse> phanTrang(AdminPhieuGiamGiaRequest req) {
        return SearchUtils.execute(req, pageable -> repo.phanTrang(req.getKeyword(), pageable));
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy phiếu giảm giá để xóa");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public void add(AdminPhieuGiamGiaRequest req) {
        PhieuGiamGia p = new PhieuGiamGia();
        BeanUtils.copyProperties(req, p);
        
        // Tự động sinh mã nếu trống
        if (p.getMa() == null || p.getMa().trim().isEmpty()) {
            // Using placeholder logic or common generator
            p.setMa("PGG" + System.currentTimeMillis() % 100000); 
        }
        
        if ("CA_NHAN".equals(req.getHinhThuc()) && req.getListIdKhachHang() != null) {
            p.setSoLuong(req.getListIdKhachHang().size());
        }
        
        repo.save(p);

        // Xử lý phiếu cá nhân
        if ("CA_NHAN".equals(req.getHinhThuc()) && req.getListIdKhachHang() != null) {
            for (String khId : req.getListIdKhachHang()) {
                khachHangRepository.findById(khId).ifPresent(kh -> {
                    PhieuGiamGiaCaNhan pgn = PhieuGiamGiaCaNhan.builder()
                            .khachHang(kh)
                            .phieuGiamGia(p)
                            .maPhieuGiamGiaCaNhan(p.getMa() + "-" + kh.getMa())
                            .ngayNhan(System.currentTimeMillis())
                            .daSuDung(false)
                            .build();
                    phieuGiamGiaCaNhanRepository.save(pgn);
                    sendVoucherEmail(kh, p);
                });
            }
        }
    }

    private void sendVoucherEmail(KhachHang kh, PhieuGiamGia p) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", kh.getTen());
        variables.put("voucherCode", p.getMa());
        variables.put("voucherName", p.getTen());
        variables.put("discountValue", p.getPhanTramGiamGia() != null ? p.getPhanTramGiamGia() + "%" : p.getSoTienGiam() + " VNĐ");
        variables.put("minOrder", p.getDonHangToiThieu());
        
        // Better date formatting
        java.time.Instant expiryInstant = java.time.Instant.ofEpochMilli(p.getNgayKetThuc() != null ? p.getNgayKetThuc() : System.currentTimeMillis());
        java.time.LocalDateTime expiryDate = java.time.LocalDateTime.ofInstant(expiryInstant, java.time.ZoneId.systemDefault());
        variables.put("expiryDate", expiryDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        EmailRequest emailRequest = EmailRequest.builder()
                .to(kh.getEmail())
                .subject("🎉 AeroStride - Bạn nhận được phiếu giảm giá mới!")
                .templateName("voucher-email")
                .variables(variables)
                .build();
        emailService.sendHtmlEmail(emailRequest);
    }

    @Override
    @Transactional
    public void update(AdminPhieuGiamGiaRequest req, String id) {
        PhieuGiamGia p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giảm giá để cập nhật"));
        BeanUtils.copyProperties(req, p);
        p.setId(id); // Ensure ID is preserved
        repo.save(p);
    }

    @Override
    @Transactional
    public void updateStatus(String id, TrangThai status) {
        PhieuGiamGia p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giảm giá!"));
        p.setTrangThai(status);
        repo.saveAndFlush(p);
    }

    @Override
    public byte[] exportExcel() {
        Pageable pageable = PaginationUtils.createPageable(0, Integer.MAX_VALUE, "id", "desc");
        List<AdminPhieuGiamGiaResponse> data = repo.phanTrang(null, pageable).getContent();
        
        String[] headers = {"STT", "Mã", "Tên", "Giá trị giảm", "Giảm tối đa", "Điều kiện", "Số lượng", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái"};
        
        try {
            return ExcelUtils.exportToExcel("Danh sách phiếu giảm giá", headers, data, item -> new Object[]{
                data.indexOf(item) + 1,
                item.getMa(),
                item.getTen(),
                item.getPhanTramGiamGia() != null ? item.getPhanTramGiamGia() + "%" : item.getSoTienGiam(),
                item.getGiamToiDa(),
                item.getDonHangToiThieu(),
                item.getSoLuong(),
                item.getNgayBatDau(),
                item.getNgayKetThuc(),
                "DANG_HOAT_DONG".equalsIgnoreCase(item.getTrangThai()) ? "Đang hoạt động" : "Ngừng hoạt động"
            });
        } catch (IOException e) {
            throw new SystemException("Lỗi xuất file Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] downloadTemplate() {
        String[] headers = {"Mã *", "Tên *", "Loại (PHAN_TRAM/TIEN_MAT) *", "Giá trị *", "Đơn tối thiểu *", "Số lượng *", "Cá nhân/Công khai (CA_NHAN/CONG_KHAI) *", "Ngày BD (dd/MM/yyyy) *", "Ngày KT (dd/MM/yyyy) *"};
        try {
            return ExcelUtils.createTemplate("Template Nhập Voucher", headers, new Object[]{
                "VOUCHER_NEW", "Giảm giá khai trương", "PHAN_TRAM", 10, 200000, 100, "CONG_KHAI", "12/04/2026", "20/04/2026"
            });
        } catch (IOException e) {
            throw new SystemException("Lỗi tải template: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void importExcel(MultipartFile file) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;

                PhieuGiamGia p = new PhieuGiamGia();
                p.setMa(ExcelUtils.getCellValueAsString(row.getCell(0)));
                p.setTen(ExcelUtils.getCellValueAsString(row.getCell(1)));
                p.setLoaiPhieu(ExcelUtils.getCellValueAsString(row.getCell(2)));
                
                String valStr = ExcelUtils.getCellValueAsString(row.getCell(3));
                if ("PHAN_TRAM".equalsIgnoreCase(p.getLoaiPhieu())) {
                    p.setPhanTramGiamGia(Integer.parseInt(valStr));
                } else {
                    p.setSoTienGiam(new java.math.BigDecimal(valStr));
                }

                p.setDonHangToiThieu(new java.math.BigDecimal(ExcelUtils.getCellValueAsString(row.getCell(4))));
                p.setSoLuong(Integer.parseInt(ExcelUtils.getCellValueAsString(row.getCell(5))));
                p.setHinhThuc(ExcelUtils.getCellValueAsString(row.getCell(6)));
                
                p.setNgayBatDau(sdf.parse(ExcelUtils.getCellValueAsString(row.getCell(7))).getTime());
                p.setNgayKetThuc(sdf.parse(ExcelUtils.getCellValueAsString(row.getCell(8))).getTime());
                
                p.setTrangThai(TrangThai.DANG_HOAT_DONG);
                repo.save(p);
            }
        } catch (Exception e) {
            throw new SystemException("Lỗi nhập Excel Voucher: " + e.getMessage());
        }
    }
}
