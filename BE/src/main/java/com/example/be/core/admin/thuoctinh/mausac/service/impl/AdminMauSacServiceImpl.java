package com.example.be.core.admin.thuoctinh.mausac.service.impl;

import com.example.be.core.admin.thuoctinh.mausac.repository.AdminMauSacRepository;
import com.example.be.core.admin.thuoctinh.mausac.service.AdminMauSacService;
import com.example.be.core.admin.thuoctinh.service.impl.AdminAttributeCrudSupport;
import com.example.be.entity.MauSac;
import org.springframework.stereotype.Service;

@Service
public class AdminMauSacServiceImpl extends AdminAttributeCrudSupport<MauSac> implements AdminMauSacService {

    public AdminMauSacServiceImpl(AdminMauSacRepository repository) {
        super(repository, MauSac::new, MauSac::getMaMauHex, MauSac::setMaMauHex, MauSac::setXoaMem, "mau sac");
    }
}
