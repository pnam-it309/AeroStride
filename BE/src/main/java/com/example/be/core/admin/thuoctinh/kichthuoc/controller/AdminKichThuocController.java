package com.example.be.core.admin.thuoctinh.kichthuoc.controller;

import com.example.be.core.admin.thuoctinh.controller.AbstractAdminAttributeController;
import com.example.be.core.admin.thuoctinh.kichthuoc.service.AdminKichThuocService;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_THUOC_TINH_KICH_THUOC)
public class AdminKichThuocController extends AbstractAdminAttributeController {

    public AdminKichThuocController(AdminKichThuocService service) {
        super(service);
    }
}
