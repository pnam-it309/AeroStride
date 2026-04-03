package com.example.be.core.admin.thuoctinh.mausac.controller;

import com.example.be.core.admin.thuoctinh.controller.AbstractAdminAttributeController;
import com.example.be.core.admin.thuoctinh.mausac.service.AdminMauSacService;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_THUOC_TINH_MAU_SAC)
public class AdminMauSacController extends AbstractAdminAttributeController {

    public AdminMauSacController(AdminMauSacService service) {
        super(service);
    }
}
