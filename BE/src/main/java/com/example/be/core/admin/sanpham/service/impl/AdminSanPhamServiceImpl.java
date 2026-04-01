package com.example.be.core.admin.sanpham.service.impl;

import com.example.be.core.admin.sanpham.model.request.AdminSanPhamRequest;
import com.example.be.core.admin.sanpham.model.response.AdminSanPhamResponse;
import com.example.be.core.admin.sanpham.repository.AdminSanPhamRepository;
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.RestApiException;
import com.example.be.repository.ChatLieuRepository;
import com.example.be.repository.CoGiayRepository;
import com.example.be.repository.DanhMucRepository;
import com.example.be.repository.DeGiayRepository;
import com.example.be.repository.MauSacRepository;
import com.example.be.repository.ThuongHieuRepository;
import com.example.be.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSanPhamServiceImpl implements AdminSanPhamService {

}
