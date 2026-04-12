package com.example.be.core.admin.khachhang.controller;

import com.example.be.core.admin.khachhang.model.request.AdminDiaChiRequest;
import com.example.be.core.admin.khachhang.service.AdminDiaChiService;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_DIA_CHI)
@RequiredArgsConstructor
public class AdminDiaChiController {

    private final AdminDiaChiService service;

    @GetMapping("/khach-hang/{khId}")
    public ResponseEntity<?> getByKhachHang(@PathVariable String khId) {
        return ResponseEntity.ok(service.getByKhachHangId(khId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody AdminDiaChiRequest request) {
        return ResponseEntity.ok(service.add(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody AdminDiaChiRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/set-default/{id}")
    public ResponseEntity<?> setDefault(@PathVariable String id) {
        service.setDefault(id);
        return ResponseEntity.ok().build();
    }
}
