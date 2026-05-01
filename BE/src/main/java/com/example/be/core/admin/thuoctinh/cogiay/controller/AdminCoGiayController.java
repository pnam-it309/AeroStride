package com.example.be.core.admin.thuoctinh.cogiay.controller;

import com.example.be.core.admin.thuoctinh.cogiay.service.AdminCoGiayService;
import com.example.be.core.admin.thuoctinh.controller.AbstractAdminAttributeController;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_THUOC_TINH_CO_GIAY)
public class AdminCoGiayController extends AbstractAdminAttributeController {

    public AdminCoGiayController(AdminCoGiayService service) {
        super(service);
    }
}
