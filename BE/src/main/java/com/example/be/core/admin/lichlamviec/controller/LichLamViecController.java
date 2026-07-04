package com.example.be.core.admin.lichlamviec.controller;

import com.example.be.core.admin.lichlamviec.model.request.CaLamRequest;
import com.example.be.core.admin.lichlamviec.model.request.LichLamViecRequest;
import com.example.be.core.admin.lichlamviec.service.LichLamViecService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.be.infrastructure.constants.VaiTro;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_LICH_LAM_VIEC)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
public class LichLamViecController {

    private final LichLamViecService lichLamViecService;

    @GetMapping(RoutesConstant.SCHEDULES)
    public ResponseEntity<ApiResponse<?>> getSchedules(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String ca,
            @RequestParam(required = false) String ngay) {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.getAllSchedules(search, ca, ngay)));
    }

    @GetMapping(RoutesConstant.SHIFTS)
    public ResponseEntity<ApiResponse<?>> getShifts() {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.getAllShifts()));
    }

    @GetMapping(RoutesConstant.ACTIVITIES)
    public ResponseEntity<ApiResponse<?>> getActivities(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String ngay,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.getActivityHistory(search, ngay, pageable)));
    }

    @GetMapping(RoutesConstant.EXPORT_TEMPLATE)
    public ResponseEntity<byte[]> exportTemplate() throws IOException {
        byte[] excelContent = lichLamViecService.exportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lich_lam_viec_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelContent);
    }

    @PostMapping(RoutesConstant.IMPORT_EXCEL)
    public ResponseEntity<ApiResponse<?>> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.importExcel(file)));
    }

    @PostMapping(RoutesConstant.PREVIEW_IMPORT)
    public ResponseEntity<ApiResponse<?>> previewImport(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.previewImport(file)));
    }

    @PostMapping(RoutesConstant.CONFIRM_IMPORT)
    public ResponseEntity<ApiResponse<?>> confirmImport(@RequestBody List<Map<String, Object>> data) {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.confirmImport(data)));
    }

    @PostMapping(RoutesConstant.SCHEDULES)
    public ResponseEntity<ApiResponse<?>> addSchedule(@RequestBody LichLamViecRequest request) {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.addSchedule(request)));
    }

    @PutMapping(RoutesConstant.SCHEDULES + "/{id}")
    public ResponseEntity<ApiResponse<?>> updateSchedule(@PathVariable String id, @RequestBody LichLamViecRequest request) {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.updateSchedule(id, request)));
    }

    @DeleteMapping(RoutesConstant.SCHEDULES + "/{id}")
    public ResponseEntity<ApiResponse<?>> deleteSchedule(@PathVariable String id) {
        lichLamViecService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa lịch làm việc thành công!"));
    }

    @PostMapping("/attendance")
    public ResponseEntity<ApiResponse<?>> saveAttendance(@RequestBody com.example.be.core.admin.lichlamviec.model.request.AttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.saveAttendance(request)));
    }

    @PutMapping("/attendance/{id}")
    public ResponseEntity<ApiResponse<?>> updateAttendance(@PathVariable String id, @RequestBody com.example.be.core.admin.lichlamviec.model.request.AttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.updateAttendance(id, request)));
    }

    // Shift CRUD Controller Endpoints
    @PostMapping(RoutesConstant.SHIFTS)
    public ResponseEntity<ApiResponse<?>> createShift(@RequestBody CaLamRequest request) {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.createShift(request)));
    }

    @PutMapping(RoutesConstant.SHIFTS + "/{id}")
    public ResponseEntity<ApiResponse<?>> updateShift(@PathVariable String id, @RequestBody CaLamRequest request) {
        return ResponseEntity.ok(ApiResponse.success(lichLamViecService.updateShift(id, request)));
    }

    @DeleteMapping(RoutesConstant.SHIFTS + "/{id}")
    public ResponseEntity<ApiResponse<?>> deleteShift(@PathVariable String id) {
        lichLamViecService.deleteShift(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa ca làm việc thành công!"));
    }
}
