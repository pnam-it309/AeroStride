package com.example.be.core.admin.thuoctinh.chatlieu.service.impl;

import com.example.be.core.admin.thuoctinh.chatlieu.repository.AdminChatLieuRepository;
import com.example.be.core.admin.thuoctinh.chatlieu.service.AdminChatLieuService;
import com.example.be.core.admin.thuoctinh.service.impl.AdminAttributeCrudSupport;
import com.example.be.entity.ChatLieu;
import org.springframework.stereotype.Service;

@Service
public class AdminChatLieuServiceImpl extends AdminAttributeCrudSupport<ChatLieu> implements AdminChatLieuService {

    public AdminChatLieuServiceImpl(AdminChatLieuRepository repository) {
        super(repository, ChatLieu::new, entity -> null, (entity, value) -> {}, ChatLieu::setXoaMem, "chat lieu");
    }
}
