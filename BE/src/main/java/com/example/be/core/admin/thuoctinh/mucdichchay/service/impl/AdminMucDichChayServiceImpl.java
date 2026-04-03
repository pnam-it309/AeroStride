package com.example.be.core.admin.thuoctinh.mucdichchay.service.impl;

import com.example.be.core.admin.thuoctinh.mucdichchay.repository.AdminMucDichChayRepository;
import com.example.be.core.admin.thuoctinh.mucdichchay.service.AdminMucDichChayService;
import com.example.be.core.admin.thuoctinh.service.impl.AdminAttributeCrudSupport;
import com.example.be.entity.MucDichChay;
import org.springframework.stereotype.Service;

@Service
public class AdminMucDichChayServiceImpl extends AdminAttributeCrudSupport<MucDichChay> implements AdminMucDichChayService {

    public AdminMucDichChayServiceImpl(AdminMucDichChayRepository repository) {
        super(repository, MucDichChay::new, entity -> null, (entity, value) -> {}, MucDichChay::setXoaMem, "muc dich chay");
    }
}
