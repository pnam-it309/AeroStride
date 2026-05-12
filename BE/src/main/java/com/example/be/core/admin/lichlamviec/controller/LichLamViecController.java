package com.example.be.core.admin.lichlamviec.controller;

import com.example.be.core.admin.lichlamviec.service.LichLamViecService;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(RoutesConstant.ADMIN + "/lich-lam-viec")
@RequiredArgsConstructor
public class LichLamViecController {

    private final LichLamViecService lichLamViecService;

    @GetMapping("/schedules")
    public ResponseEntity<?> getSchedules() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", lichLamViecService.getAllSchedules());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shifts")
    public ResponseEntity<?> getShifts() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", lichLamViecService.getAllShifts());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/activities")
    public ResponseEntity<?> getActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", lichLamViecService.getActivityHistory(pageable));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-template")
    public ResponseEntity<byte[]> exportTemplate() throws IOException {
        byte[] excelContent = lichLamViecService.exportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lich_lam_viec_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }

    @PostMapping("/import")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", lichLamViecService.importExcel(file));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/preview-import")
    public ResponseEntity<?> previewImport(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", lichLamViecService.previewImport(file));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm-import")
    public ResponseEntity<?> confirmImport(@RequestBody java.util.List<Map<String, Object>> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", lichLamViecService.confirmImport(data));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/schedules")
    public ResponseEntity<?> addSchedule(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", lichLamViecService.addSchedule(request));
        return ResponseEntity.ok(response);
    }
}
