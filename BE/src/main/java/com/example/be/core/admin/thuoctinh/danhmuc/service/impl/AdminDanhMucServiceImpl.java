package com.example.be.core.admin.thuoctinh.danhmuc.service.impl;

import com.example.be.core.admin.thuoctinh.danhmuc.repository.AdminDanhMucRepository;
import com.example.be.core.admin.thuoctinh.danhmuc.service.AdminDanhMucService;
import com.example.be.core.admin.thuoctinh.model.request.AdminAttributeRequest;
import com.example.be.core.admin.thuoctinh.model.response.AdminAttributeResponse;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.entity.DanhMuc;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDanhMucServiceImpl implements AdminDanhMucService {

}
