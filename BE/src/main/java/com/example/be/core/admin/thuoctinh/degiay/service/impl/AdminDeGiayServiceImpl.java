package com.example.be.core.admin.thuoctinh.degiay.service.impl;

import com.example.be.core.admin.thuoctinh.degiay.repository.AdminDeGiayRepository;
import com.example.be.core.admin.thuoctinh.degiay.service.AdminDeGiayService;
import com.example.be.core.admin.thuoctinh.model.request.AdminAttributeRequest;
import com.example.be.core.admin.thuoctinh.model.response.AdminAttributeResponse;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.entity.DeGiay;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDeGiayServiceImpl implements AdminDeGiayService {

}
