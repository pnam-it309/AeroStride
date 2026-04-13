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
import com.example.be.infrastructure.notification.EmailServiceImpl;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.PhieuGiamGiaCaNhanRepository;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.MaGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class AdminPhieuGiamGiaServiceImpl implements AdminPhieuGiamGiaService {

    @Autowired
    private AdminPhieuGiamGiaRepository repo;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;

    @Autowired
    private EmailService emailService;

    @Override
    public List<AdminPhieuGiamGiaResponse> hienThi() {
        return repo.hienThi();
    }

    @Override
    public AdminPhieuGiamGiaResponse detail(String id) {
        return repo.detail(id);
    }

    @Override
    public Page<AdminPhieuGiamGiaResponse> phanTrang(Integer pageNo, Integer pageSize, String keyword) {
        // Sắp xếp theo ID giảm dần hoặc ngayTao giảm dần để dữ liệu mới luôn ở trên cùng
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        return repo.phanTrang(keyword, pageable);
    }

    @Override
    public void delete(String id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public void add(AdminPhieuGiamGiaRequest req) {
        PhieuGiamGia p = new PhieuGiamGia();
        BeanUtils.copyProperties(req, p);
        
        // Tự động sinh mã nếu trống
        if (p.getMa() == null || p.getMa().trim().isEmpty()) {
            p.setMa(MaGenerator.generate(PhieuGiamGia.class));
        }
        
        if ("CA_NHAN".equals(req.getHinhThuc())) {
            // Cho phiếu cá nhân, số lượng = số khách hàng được chọn
            if (req.getListIdKhachHang() != null) {
                p.setSoLuong(req.getListIdKhachHang().size());
            }
        }
        
        repo.save(p);

        // Xử lý phiếu cá nhân
        if ("CA_NHAN".equals(req.getHinhThuc()) && req.getListIdKhachHang() != null) {
            for (String khId : req.getListIdKhachHang()) {
                KhachHang kh = khachHangRepository.findById(khId).orElse(null);
                if (kh != null) {
                    PhieuGiamGiaCaNhan pgn = PhieuGiamGiaCaNhan.builder()
                            .khachHang(kh)
                            .phieuGiamGia(p)
                            .maPhieuGiamGiaCaNhan(p.getMa() + "-" + kh.getMa())
                            .ngayNhan(System.currentTimeMillis())
                            .daSuDung(false)
                            .build();
                    phieuGiamGiaCaNhanRepository.save(pgn);
                    
                    // Gửi email cho khách hàng
                    sendVoucherEmail(kh, p);
                }
            }
        }
    }

    private void sendVoucherEmail(KhachHang kh, PhieuGiamGia p) {
        Map<String, Object> variables = new HashMap<>();
        variables.getOrDefault("name", kh.getTen());
        variables.put("voucherCode", p.getMa());
        variables.put("voucherName", p.getTen());
        variables.put("discountValue", p.getPhanTramGiamGia() != null ? p.getPhanTramGiamGia() + "%" : p.getSoTienGiam() + " VNĐ");
        variables.put("minOrder", p.getDonHangToiThieu());
        variables.put("expiryDate", new java.util.Date(p.getNgayKetThuc() != null ? p.getNgayKetThuc() : System.currentTimeMillis()).toLocaleString());

        EmailRequest emailRequest = EmailRequest.builder()
                .to(kh.getEmail())
                .subject("AeroStride - Bạn nhận được phiếu giảm giá mới!")
                .templateName("voucher-email")
                .variables(variables)
                .build();
        emailService.sendHtmlEmail(emailRequest);
    }

    @Override
    public void update(AdminPhieuGiamGiaRequest req, String id) {
        PhieuGiamGia p = repo.findById(id).get();
        BeanUtils.copyProperties(req, p);
        repo.save(p);
    }

    @Override
    public void updateStatus(String id, com.example.be.infrastructure.constants.TrangThai status) {
        PhieuGiamGia p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá!"));
        p.setTrangThai(status);
        repo.save(p);
    }
    @Override
    public byte[] exportExcel() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("id").descending());
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
                "DANG_HOAT_DONG".equals(item.getTrangThai()) ? "Đang hoạt động" : "Ngừng hoạt động"
            });
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file Excel: " + e.getMessage());
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
            throw new RuntimeException("Lỗi tải template: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void importExcel(org.springframework.web.multipart.MultipartFile file) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;

                PhieuGiamGia p = new PhieuGiamGia();
                p.setMa(ExcelUtils.getCellValueAsString(row.getCell(0)));
                p.setTen(ExcelUtils.getCellValueAsString(row.getCell(1)));
                p.setLoaiPhieu(ExcelUtils.getCellValueAsString(row.getCell(2)));
                
                String valStr = ExcelUtils.getCellValueAsString(row.getCell(3));
                if ("PHAN_TRAM".equals(p.getLoaiPhieu())) {
                    p.setPhanTramGiamGia(Integer.parseInt(valStr));
                } else {
                    p.setSoTienGiam(new java.math.BigDecimal(valStr));
                }

                p.setDonHangToiThieu(new java.math.BigDecimal(ExcelUtils.getCellValueAsString(row.getCell(4))));
                p.setSoLuong(Integer.parseInt(ExcelUtils.getCellValueAsString(row.getCell(5))));
                p.setHinhThuc(ExcelUtils.getCellValueAsString(row.getCell(6)));
                
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                p.setNgayBatDau(sdf.parse(ExcelUtils.getCellValueAsString(row.getCell(7))).getTime());
                p.setNgayKetThuc(sdf.parse(ExcelUtils.getCellValueAsString(row.getCell(8))).getTime());
                
                p.setTrangThai(com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG);
                repo.save(p);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi nhập Excel Voucher: " + e.getMessage());
        }
    }
}
