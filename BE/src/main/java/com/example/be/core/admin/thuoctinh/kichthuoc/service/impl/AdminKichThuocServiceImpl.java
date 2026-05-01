package com.example.be.core.admin.thuoctinh.kichthuoc.service.impl;

import com.example.be.core.admin.thuoctinh.kichthuoc.repository.AdminKichThuocRepository;
import com.example.be.core.admin.thuoctinh.kichthuoc.service.AdminKichThuocService;
import com.example.be.core.admin.thuoctinh.service.impl.AdminAttributeCrudSupport;
import com.example.be.entity.KichThuoc;
import org.springframework.stereotype.Service;

@Service
public class AdminKichThuocServiceImpl extends AdminAttributeCrudSupport<KichThuoc> implements AdminKichThuocService {

    public AdminKichThuocServiceImpl(AdminKichThuocRepository repository) {
        super(repository, KichThuoc::new, KichThuoc::getGiaTriKichThuoc, KichThuoc::setGiaTriKichThuoc, KichThuoc::setXoaMem, "kich thuoc");
    }
}
