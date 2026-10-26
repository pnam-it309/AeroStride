package com.example.be.infrastructure.websocket;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.springframework.lang.NonNull;

@Component
public class WebSocketContextHolder implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) return null;
        try {
            return context.getBean(beanClass);
        } catch (Exception e) {
            return null;
        }
    }
}
