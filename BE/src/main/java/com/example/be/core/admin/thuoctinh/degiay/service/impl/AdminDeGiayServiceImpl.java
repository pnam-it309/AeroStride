package com.example.be.core.admin.thuoctinh.degiay.service.impl;

import com.example.be.core.admin.thuoctinh.degiay.repository.AdminDeGiayRepository;
import com.example.be.core.admin.thuoctinh.degiay.service.AdminDeGiayService;
import com.example.be.core.admin.thuoctinh.service.impl.AdminAttributeCrudSupport;
import com.example.be.entity.DeGiay;
import org.springframework.stereotype.Service;

@Service
public class AdminDeGiayServiceImpl extends AdminAttributeCrudSupport<DeGiay> implements AdminDeGiayService {

    public AdminDeGiayServiceImpl(AdminDeGiayRepository repository) {
        super(repository, DeGiay::new, entity -> null, (entity, value) -> {}, DeGiay::setXoaMem, "de giay");
    }
}
