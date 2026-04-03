package com.example.be.core.admin.thuoctinh.xuatxu.controller;

import com.example.be.core.admin.thuoctinh.controller.AbstractAdminAttributeController;
import com.example.be.core.admin.thuoctinh.xuatxu.service.AdminXuatXuService;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_THUOC_TINH_XUAT_XU)
public class AdminXuatXuController extends AbstractAdminAttributeController {

    public AdminXuatXuController(AdminXuatXuService service) {
        super(service);
    }
}
