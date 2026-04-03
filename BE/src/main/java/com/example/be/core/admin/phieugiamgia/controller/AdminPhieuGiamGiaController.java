package com.example.be.core.admin.phieugiamgia.controller;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@RequestMapping(RoutesConstant.ADMIN_PHIEU_GIAM_GIA)
@RequestMapping("/api/v1/admin/phieu-giam-gia")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminPhieuGiamGiaController {

    @Autowired
    private AdminPhieuGiamGiaService service;

    @GetMapping("hien-thi")
    public List<AdminPhieuGiamGiaResponse> hienThi(){
        return service.hienThi();
    }

    @GetMapping("detail")
    public AdminPhieuGiamGiaResponse detail(@RequestParam("ma") String ma){
        return service.detail(ma);
    }

    @GetMapping("/phan-trang")
    public ResponseEntity<?> phanTrang(
        @RequestParam(value = "pageNo", defaultValue = "0", required = false) Integer pageNo,
        @RequestParam(value = "pageSize", defaultValue = "5", required = false) Integer pageSize,
        @RequestParam(value = "keyword", defaultValue = "", required = false) String keyword
    ) {
        return ResponseEntity.ok(service.phanTrang(pageNo, pageSize, keyword));
    }

    @DeleteMapping("delete")
    public void delete(@RequestParam Integer id){
        service.delete(id);
    }

    @PostMapping("add")
    public void add(@RequestBody AdminPhieuGiamGiaRequest req){
        service.add(req);
    }

    @PutMapping("update")
    public void update(@RequestBody AdminPhieuGiamGiaRequest req,
                       @RequestParam Integer id){
        service.update(req,id);
    }
}
