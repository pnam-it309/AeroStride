package com.example.be.core.admin.lichlamviec.service;

import com.example.be.core.admin.lichlamviec.model.request.CaLamRequest;
import com.example.be.core.admin.lichlamviec.model.request.LichLamViecRequest;
import com.example.be.core.admin.lichlamviec.model.CaLamResponse;
import com.example.be.core.admin.lichlamviec.model.LichLamViecResponse;
import com.example.be.core.admin.lichlamviec.model.LichSuHoatDongResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface LichLamViecService {

    List<LichLamViecResponse> getAllSchedules();

    List<CaLamResponse> getAllShifts();

    Page<LichSuHoatDongResponse> getActivityHistory(String search, String ngay, Pageable pageable);

    byte[] exportTemplate() throws IOException;

    String importExcel(MultipartFile file) throws IOException;

    String addSchedule(LichLamViecRequest request);

    String updateSchedule(String id, LichLamViecRequest request);

    List<Map<String, Object>> previewImport(MultipartFile file);

    String confirmImport(List<Map<String, Object>> data);

    void deleteSchedule(String id);

    // Attendance
    String saveAttendance(com.example.be.core.admin.lichlamviec.model.request.AttendanceRequest request);

    String updateAttendance(String id, com.example.be.core.admin.lichlamviec.model.request.AttendanceRequest request);

    // Shift (Ca Lam) CRUD
    String createShift(CaLamRequest request);

    String updateShift(String id, CaLamRequest request);

    void deleteShift(String id);
}
