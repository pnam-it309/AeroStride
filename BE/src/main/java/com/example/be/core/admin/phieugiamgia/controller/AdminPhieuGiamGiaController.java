package com.example.be.core.admin.phieugiamgia.controller;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_PHIEU_GIAM_GIA)
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminPhieuGiamGiaController {

    private final AdminPhieuGiamGiaService service;

    @GetMapping("/hien-thi")
    public ResponseEntity<?> hienThi() {
        return ResponseEntity.ok(ApiResponse.success(service.hienThi()));
    }

    @GetMapping("/detail")
    public ResponseEntity<?> detail(@RequestParam("ma") String ma) {
        return ResponseEntity.ok(ApiResponse.success(service.detail(ma)));
    }

    @GetMapping("/phan-trang")
    public ResponseEntity<?> phanTrang(
            @RequestParam(value = "pageNo", defaultValue = "0", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(value = "keyword", defaultValue = "", required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.phanTrang(pageNo, pageSize, keyword)));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công!"));
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody AdminPhieuGiamGiaRequest req) {
        service.add(req);
        return ResponseEntity.ok(ApiResponse.success("Thêm thành công!"));
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody AdminPhieuGiamGiaRequest req,
                                    @RequestParam String id) {
        service.update(req, id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công!"));
    }
}
