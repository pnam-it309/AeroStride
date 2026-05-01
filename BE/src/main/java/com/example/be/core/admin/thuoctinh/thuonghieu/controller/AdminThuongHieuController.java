package com.example.be.core.admin.thuoctinh.thuonghieu.controller;

import com.example.be.core.admin.thuoctinh.controller.AbstractAdminAttributeController;
import com.example.be.core.admin.thuoctinh.thuonghieu.service.AdminThuongHieuService;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_THUOC_TINH_THUONG_HIEU)
public class AdminThuongHieuController extends AbstractAdminAttributeController {

    public AdminThuongHieuController(AdminThuongHieuService service) {
        super(service);
    }
}
