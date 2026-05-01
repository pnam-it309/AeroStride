package com.example.be.core.admin.thuoctinh.degiay.controller;

import com.example.be.core.admin.thuoctinh.controller.AbstractAdminAttributeController;
import com.example.be.core.admin.thuoctinh.degiay.service.AdminDeGiayService;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_THUOC_TINH_DE_GIAY)
public class AdminDeGiayController extends AbstractAdminAttributeController {

    public AdminDeGiayController(AdminDeGiayService service) {
        super(service);
    }
}
