package com.example.be.core.admin.phieugiamgia.service.impl;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import com.example.be.core.admin.phieugiamgia.repository.AdminPhieuGiamGiaCaNhanRepository;
import com.example.be.core.admin.phieugiamgia.repository.AdminPhieuGiamGiaRepository;
import com.example.be.core.admin.phieugiamgia.repository.AdminPhieuGiamGiaSpecification;
import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import com.example.be.core.notification.EmailService;
import com.example.be.core.notification.dto.EmailRequest;
import com.example.be.entity.KhachHang;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.entity.PhieuGiamGiaCaNhan;
import com.example.be.infrastructure.batch.service.ExcelBatchResult;
import com.example.be.infrastructure.batch.service.ExcelBatchService;
import com.example.be.infrastructure.constants.HinhThucPhieuGiamGia;
import com.example.be.infrastructure.constants.LoaiPhieuGiamGia;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.infrastructure.exceptions.SystemException;
import com.example.be.repository.KhachHangRepository;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPhieuGiamGiaServiceImpl implements AdminPhieuGiamGiaService {

    private final AdminPhieuGiamGiaRepository repo;
    private final KhachHangRepository khachHangRepository;
    private final AdminPhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    private final EmailService emailService;
    private final ExcelBatchService excelBatchService;

    @Override
    public List<AdminPhieuGiamGiaResponse> hienThi() {
        List<PhieuGiamGia> list = repo.findAll(Sort.by(Sort.Direction.DESC, "ngayTao"));
        return toResponseList(list);
    }

    @Override
    public AdminPhieuGiamGiaResponse detail(String id) {
        PhieuGiamGia p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PHIEU_GIAM_GIA_NOT_FOUND_ID + id));
        List<String> listIdKhachHang = null;
        if (HinhThucPhieuGiamGia.isCaNhan(p.getHinhThuc())) {
            listIdKhachHang = phieuGiamGiaCaNhanRepository.findByPhieuGiamGiaId(p.getId()).stream()
                    .map(pgn -> pgn.getKhachHang())
                    .filter(kh -> kh != null)
                    .map(kh -> kh.getId())
                    .collect(Collectors.toList());
        }
        return toResponse(p, listIdKhachHang);
    }

    @Override
    public Page<AdminPhieuGiamGiaResponse> phanTrang(AdminPhieuGiamGiaRequest req) {
        Long tuNgayLong = null;
        Long denNgayLong = null;

        try {
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
            if (req.getTuNgay() != null && !req.getTuNgay().isEmpty()) {
                tuNgayLong = java.time.LocalDate.parse(req.getTuNgay(), dtf)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            }
            if (req.getDenNgay() != null && !req.getDenNgay().isEmpty()) {
                denNgayLong = java.time.LocalDate.parse(req.getDenNgay(), dtf)
                        .plusDays(1)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli() - 1L;
            }
        } catch (Exception e) {
            // Ignore parse errors
        }

        final Long finalTuNgay = tuNgayLong;
        final Long finalDenNgay = denNgayLong;

        Specification<PhieuGiamGia> spec = Specification
                .where(AdminPhieuGiamGiaSpecification.keywordLike(req.getKeyword()))
                .and(AdminPhieuGiamGiaSpecification.hasLoaiPhieu(req.getLoaiPhieu()))
                .and(AdminPhieuGiamGiaSpecification.hasHinhThuc(req.getHinhThuc()))
                .and(AdminPhieuGiamGiaSpecification.hasTrangThai(req.getTrangThai()))
                .and(AdminPhieuGiamGiaSpecification.filterTimeline(req.getTimelineStatus(), System.currentTimeMillis()))
                .and(AdminPhieuGiamGiaSpecification.startDateAfter(finalTuNgay))
                .and(AdminPhieuGiamGiaSpecification.endDateBefore(finalDenNgay));

        return SearchUtils.execute(req, pageable -> {
            Page<PhieuGiamGia> page = repo.findAll(spec, pageable);
            List<PhieuGiamGia> content = page.getContent();
            Map<String, List<String>> voucherCustomerMap = getVoucherCustomerMap(content);
            return page.map(p -> toResponse(p, voucherCustomerMap.get(p.getId())));
        });
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException(MessageConstants.PHIEU_GIAM_GIA_DELETE_ERROR);
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public void add(AdminPhieuGiamGiaRequest req) {
        validateRequest(req);
        PhieuGiamGia p = new PhieuGiamGia();
        BeanUtils.copyProperties(req, p);

        // Tự động sinh mã nếu trống
        if (p.getMa() == null || p.getMa().trim().isEmpty()) {
            p.setMa(com.example.be.utils.CodeUtils.generateRandom(PhieuGiamGia.class, repo::existsByMa));
        }

        if (HinhThucPhieuGiamGia.isCaNhan(req.getHinhThuc()) && req.getListIdKhachHang() != null) {
            p.setSoLuong(req.getListIdKhachHang().size());
        }

        repo.save(p);

        // Xử lý phiếu cá nhân
        if (HinhThucPhieuGiamGia.isCaNhan(req.getHinhThuc()) && req.getListIdKhachHang() != null) {
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

    private static String formatVnd(java.math.BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount).replace(",", ".");
    }

    private void sendVoucherEmail(KhachHang kh, PhieuGiamGia p) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", kh.getTen());
        variables.put("voucherCode", p.getMa());
        variables.put("voucherName", p.getTen());
        variables.put("loaiPhieu", p.getLoaiPhieu());
        variables.put("isPhanTram", LoaiPhieuGiamGia.isPhanTram(p.getLoaiPhieu()));
        variables.put("discountPercent", p.getPhanTramGiamGia() != null ? p.getPhanTramGiamGia() : 0);
        variables.put("giamToiDa", p.getGiamToiDa() != null ? formatVnd(p.getGiamToiDa()) : null);
        variables.put("soTienGiam", p.getSoTienGiam() != null ? formatVnd(p.getSoTienGiam()) : "0");
        variables.put("minOrder", p.getDonHangToiThieu() != null ? formatVnd(p.getDonHangToiThieu()) : "0");

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
        validateRequest(req);
        PhieuGiamGia p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PHIEU_GIAM_GIA_UPDATE_ERROR));
        TrangThai oldStatus = p.getTrangThai();
        
        BeanUtils.copyProperties(req, p, "trangThai", "ma");
        p.setId(id);
        repo.save(p);

        if (HinhThucPhieuGiamGia.isCaNhan(p.getHinhThuc())) {
            String statusDesc = p.getTrangThai() == TrangThai.DANG_HOAT_DONG ? "được kích hoạt hoạt động" : "đã kết thúc";
            sendVoucherStatusEmail(p, statusDesc);
        }
    }

    private void validateRequest(AdminPhieuGiamGiaRequest req) {
        if (req.getTen() != null) {
            req.setTen(req.getTen().trim().replaceAll("\\s+", " "));
        }
        if (req.getMa() != null) {
            req.setMa(req.getMa().trim());
        }
        if (req.getGhiChu() != null) {
            req.setGhiChu(req.getGhiChu().trim());
        }
        if (req.getNgayBatDau() != null && req.getNgayKetThuc() != null) {
            if (req.getNgayBatDau() >= req.getNgayKetThuc()) {
                throw new com.example.be.infrastructure.exceptions.SystemException("Ngày kết thúc phải sau ngày bắt đầu");
            }
        }
        if (LoaiPhieuGiamGia.isTienMat(req.getLoaiPhieu())) {
            if (req.getSoTienGiam() != null && req.getDonHangToiThieu() != null) {
                if (req.getDonHangToiThieu().compareTo(req.getSoTienGiam()) <= 0) {
                    throw new com.example.be.infrastructure.exceptions.SystemException("Hóa đơn tối thiểu phải lớn hơn giá trị giảm");
                }
            }
        }
    }

    @Override
    @Transactional
    public void updateStatus(String id, TrangThai status) {
        PhieuGiamGia p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PHIEU_GIAM_GIA_NOT_FOUND));
        TrangThai oldStatus = p.getTrangThai();
        p.setTrangThai(status);
        if (status == TrangThai.NGUNG_HOAT_DONG) {
            p.setNgayKetThuc(System.currentTimeMillis());
        }
        repo.saveAndFlush(p);

        if (HinhThucPhieuGiamGia.isCaNhan(p.getHinhThuc()) && oldStatus != status) {
            String statusDesc = status == TrangThai.DANG_HOAT_DONG ? "được kích hoạt hoạt động" : "đã kết thúc";
            sendVoucherStatusEmail(p, statusDesc);
        }
    }

    @Override
    public byte[] exportExcel() {
        List<PhieuGiamGia> list = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<AdminPhieuGiamGiaResponse> data = toResponseList(list);

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
            throw new SystemException(MessageConstants.EXCEL_EXPORT_ERROR + e.getMessage());
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
            throw new SystemException(MessageConstants.TEMPLATE_LOAD_ERROR + e.getMessage());
        }
    }

    @Override
    public void importExcel(MultipartFile file) {
        log.info("Starting voucher import using Spring Batch...");
        ExcelBatchResult result = excelBatchService.runVoucherImportJob(file);
        log.info("Voucher import completed: {}", result.getMessage());
    }

    private AdminPhieuGiamGiaResponse toResponse(PhieuGiamGia p) {
        if (p == null) return null;
        
        List<String> listIdKhachHang = null;
        if (HinhThucPhieuGiamGia.isCaNhan(p.getHinhThuc())) {
            listIdKhachHang = phieuGiamGiaCaNhanRepository.findByPhieuGiamGiaId(p.getId()).stream()
                    .map(pgn -> pgn.getKhachHang())
                    .filter(kh -> kh != null)
                    .map(kh -> kh.getId())
                    .collect(Collectors.toList());
        }

        return toResponse(p, listIdKhachHang);
    }

    private AdminPhieuGiamGiaResponse toResponse(PhieuGiamGia p, List<String> listIdKhachHang) {
        if (p == null) return null;

        return AdminPhieuGiamGiaResponse.builder()
                .id(p.getId())
                .ma(p.getMa())
                .ten(p.getTen())
                .loaiPhieu(p.getLoaiPhieu())
                .hinhThuc(p.getHinhThuc())
                .phanTramGiamGia(p.getPhanTramGiamGia())
                .soTienGiam(p.getSoTienGiam())
                .soLuong(p.getSoLuong())
                .donHangToiThieu(p.getDonHangToiThieu())
                .giamToiDa(p.getGiamToiDa())
                .ngayBatDau(p.getNgayBatDau())
                .ngayKetThuc(p.getNgayKetThuc())
                .ghiChu(p.getGhiChu())
                .trangThai(p.getTrangThai() != null ? p.getTrangThai().name() : null)
                .listIdKhachHang(listIdKhachHang)
                .build();
    }

    private Map<String, List<String>> getVoucherCustomerMap(List<PhieuGiamGia> list) {
        List<String> personalVoucherIds = list.stream()
                .filter(p -> HinhThucPhieuGiamGia.isCaNhan(p.getHinhThuc()))
                .map(PhieuGiamGia::getId)
                .collect(Collectors.toList());

        Map<String, List<String>> voucherCustomerMap = new java.util.HashMap<>();
        if (!personalVoucherIds.isEmpty()) {
            List<PhieuGiamGiaCaNhan> relations = phieuGiamGiaCaNhanRepository.findAllByPhieuGiamGiaIdIn(personalVoucherIds);
            voucherCustomerMap = relations.stream()
                    .filter(relation -> relation.getPhieuGiamGia() != null && relation.getKhachHang() != null)
                    .collect(Collectors.groupingBy(
                            relation -> relation.getPhieuGiamGia().getId(),
                            Collectors.mapping(relation -> relation.getKhachHang().getId(), Collectors.toList())
                    ));
        }
        return voucherCustomerMap;
    }

    private List<AdminPhieuGiamGiaResponse> toResponseList(List<PhieuGiamGia> list) {
        if (list == null) return List.of();
        Map<String, List<String>> voucherCustomerMap = getVoucherCustomerMap(list);
        return list.stream()
                .map(p -> toResponse(p, voucherCustomerMap.get(p.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void checkAndExpireVouchers() {
        List<PhieuGiamGia> expiredVouchers = repo.findByTrangThaiAndNgayKetThucLessThan(
                TrangThai.DANG_HOAT_DONG,
                System.currentTimeMillis()
        );
        
        if (expiredVouchers == null || expiredVouchers.isEmpty()) {
            return;
        }

        for (PhieuGiamGia p : expiredVouchers) {
            p.setTrangThai(TrangThai.NGUNG_HOAT_DONG);
            repo.save(p);
            
            if (HinhThucPhieuGiamGia.isCaNhan(p.getHinhThuc())) {
                sendVoucherStatusEmail(p, "hết hạn sử dụng");
            }
        }
    }

    private void sendVoucherStatusEmail(PhieuGiamGia p, String statusDesc) {
        if (!HinhThucPhieuGiamGia.isCaNhan(p.getHinhThuc())) {
            return;
        }
        
        List<PhieuGiamGiaCaNhan> list = phieuGiamGiaCaNhanRepository.findByPhieuGiamGiaId(p.getId());
        if (list == null || list.isEmpty()) {
            return;
        }

        String explanation = "Chúng tôi xin thông báo về sự thay đổi trạng thái của phiếu giảm giá dành riêng cho bạn từ AeroStride.";
        if ("hết hạn sử dụng".equals(statusDesc) || "đã kết thúc".equals(statusDesc)) {
            explanation = "Rất tiếc, thời gian áp dụng của phiếu giảm giá dành riêng cho bạn đã kết thúc.";
        }

        for (PhieuGiamGiaCaNhan pgn : list) {
            KhachHang kh = pgn.getKhachHang();
            if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
                continue;
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("name", kh.getTen());
            variables.put("voucherCode", p.getMa());
            variables.put("voucherName", p.getTen());
            variables.put("statusDesc", statusDesc);
            variables.put("explanation", explanation);

            EmailRequest emailRequest = EmailRequest.builder()
                    .to(kh.getEmail())
                    .subject("AeroStride - Cập nhật trạng thái phiếu giảm giá của bạn")
                    .templateName("voucher-status-email")
                    .variables(variables)
                    .build();
            emailService.sendHtmlEmail(emailRequest);
        }
    }
}
