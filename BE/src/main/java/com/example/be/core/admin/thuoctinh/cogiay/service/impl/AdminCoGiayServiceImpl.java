package com.example.be.core.admin.thuoctinh.cogiay.service.impl;

import com.example.be.core.admin.thuoctinh.cogiay.repository.AdminCoGiayRepository;
import com.example.be.core.admin.thuoctinh.cogiay.service.AdminCoGiayService;
import com.example.be.core.admin.thuoctinh.service.impl.AdminAttributeCrudSupport;
import com.example.be.entity.CoGiay;
import org.springframework.stereotype.Service;

@Service
public class AdminCoGiayServiceImpl extends AdminAttributeCrudSupport<CoGiay> implements AdminCoGiayService {

    public AdminCoGiayServiceImpl(AdminCoGiayRepository repository) {
        super(repository, CoGiay::new, CoGiay::getGiaTriKichThuoc, CoGiay::setGiaTriKichThuoc, CoGiay::setXoaMem, "co giay");
    }
}
