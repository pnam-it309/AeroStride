package com.example.be.core.admin.dotgiamgia.controller;

import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaRequest;
import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaSearchRequest;
import com.example.be.core.admin.dotgiamgia.service.AdminDotGiamGiaService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
//@RequestMapping(RoutesConstant.ADMIN_DOT_GIAM_GIA)
@RequestMapping("/api/v1/admin/dot-giam-gia")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AdminDotGiamGiaController {

    @Autowired
    private AdminDotGiamGiaService service;

    @GetMapping("/phan-trang")
    public ResponseEntity<?> phanTrang(
        @RequestParam(defaultValue = "0") Integer pageNo,
        @RequestParam(defaultValue = "5") Integer pageSize,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.phanTrang(pageNo, pageSize, keyword)));
    }

    @PostMapping("/add")
    public void add(@RequestBody AdminDotGiamGiaRequest req) {
        service.add(req);
    }

    @PutMapping("/update")
    public void update(@RequestBody AdminDotGiamGiaRequest req,
                       @RequestParam String id) {
        service.update(req, id);
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String id) {
        service.delete(id);
    }

}
