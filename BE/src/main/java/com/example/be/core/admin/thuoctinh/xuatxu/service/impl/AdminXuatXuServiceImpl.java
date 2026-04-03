package com.example.be.core.admin.thuoctinh.xuatxu.service.impl;

import com.example.be.core.admin.thuoctinh.service.impl.AdminAttributeCrudSupport;
import com.example.be.core.admin.thuoctinh.xuatxu.repository.AdminXuatXuRepository;
import com.example.be.core.admin.thuoctinh.xuatxu.service.AdminXuatXuService;
import com.example.be.entity.XuatXu;
import org.springframework.stereotype.Service;

@Service
public class AdminXuatXuServiceImpl extends AdminAttributeCrudSupport<XuatXu> implements AdminXuatXuService {

    public AdminXuatXuServiceImpl(AdminXuatXuRepository repository) {
        super(repository, XuatXu::new, entity -> null, (entity, value) -> {}, XuatXu::setXoaMem, "xuat xu");
    }
}
