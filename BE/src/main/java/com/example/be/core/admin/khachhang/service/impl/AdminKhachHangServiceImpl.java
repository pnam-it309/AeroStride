package com.example.be.core.admin.khachhang.service.impl;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.core.admin.khachhang.repository.AdminKhachHangRepository;
import com.example.be.core.admin.khachhang.service.AdminKhachHangService;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.entity.KhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminKhachHangServiceImpl implements AdminKhachHangService {

}
