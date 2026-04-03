package com.example.be.core.admin.thuoctinh.mucdichchay.controller;

import com.example.be.core.admin.thuoctinh.controller.AbstractAdminAttributeController;
import com.example.be.core.admin.thuoctinh.mucdichchay.service.AdminMucDichChayService;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_THUOC_TINH_MUC_DICH_CHAY)
public class AdminMucDichChayController extends AbstractAdminAttributeController {

    public AdminMucDichChayController(AdminMucDichChayService service) {
        super(service);
    }
}
