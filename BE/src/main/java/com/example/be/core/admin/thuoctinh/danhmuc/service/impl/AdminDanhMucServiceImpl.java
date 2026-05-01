package com.example.be.core.admin.thuoctinh.danhmuc.service.impl;

import com.example.be.core.admin.thuoctinh.danhmuc.repository.AdminDanhMucRepository;
import com.example.be.core.admin.thuoctinh.danhmuc.service.AdminDanhMucService;
import com.example.be.core.admin.thuoctinh.service.impl.AdminAttributeCrudSupport;
import com.example.be.entity.DanhMuc;
import org.springframework.stereotype.Service;

@Service
public class AdminDanhMucServiceImpl extends AdminAttributeCrudSupport<DanhMuc> implements AdminDanhMucService {

    public AdminDanhMucServiceImpl(AdminDanhMucRepository repository) {
        super(repository, DanhMuc::new, entity -> null, (entity, value) -> {}, DanhMuc::setXoaMem, "danh muc");
    }
}
