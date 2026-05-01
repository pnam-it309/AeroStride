package com.example.be.core.admin.thuoctinh.thuonghieu.service.impl;

import com.example.be.core.admin.thuoctinh.service.impl.AdminAttributeCrudSupport;
import com.example.be.core.admin.thuoctinh.thuonghieu.repository.AdminThuongHieuRepository;
import com.example.be.core.admin.thuoctinh.thuonghieu.service.AdminThuongHieuService;
import com.example.be.entity.ThuongHieu;
import org.springframework.stereotype.Service;

@Service
public class AdminThuongHieuServiceImpl extends AdminAttributeCrudSupport<ThuongHieu> implements AdminThuongHieuService {

    public AdminThuongHieuServiceImpl(AdminThuongHieuRepository repository) {
        super(repository, ThuongHieu::new, entity -> null, (entity, value) -> {}, ThuongHieu::setXoaMem, "thuong hieu");
    }
}
