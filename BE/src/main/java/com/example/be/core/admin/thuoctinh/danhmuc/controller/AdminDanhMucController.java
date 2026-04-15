package com.example.be.core.admin.thuoctinh.danhmuc.controller;

import com.example.be.core.admin.thuoctinh.controller.AbstractAdminAttributeController;
import com.example.be.core.admin.thuoctinh.danhmuc.service.AdminDanhMucService;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_THUOC_TINH_DANH_MUC)
public class AdminDanhMucController extends AbstractAdminAttributeController {

    public AdminDanhMucController(AdminDanhMucService service) {
        super(service);
    }
}
