package com.example.be.core.admin.thuoctinh.chatlieu.controller;

import com.example.be.core.admin.thuoctinh.chatlieu.service.AdminChatLieuService;
import com.example.be.core.admin.thuoctinh.controller.AbstractAdminAttributeController;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_THUOC_TINH_CHAT_LIEU)
public class AdminChatLieuController extends AbstractAdminAttributeController {

    public AdminChatLieuController(AdminChatLieuService service) {
        super(service);
    }
}
