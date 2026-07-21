package com.example.be.core.common.controller;

import com.example.be.utils.CodeUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.be.infrastructure.constants.RoutesConstant;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping(RoutesConstant.ADMIN_COMMON)
public class CodeGeneratorController {

    @GetMapping("/code/generate")
    public ResponseEntity<Object> generateCode(
            @RequestParam("type") String type,
            @RequestParam(value = "count", defaultValue = "1") int count) {
        if (count > 1) {
            List<String> codes = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                codes.add(CodeUtils.generateRandom(type));
            }
            return ResponseEntity.ok(codes);
        }
        return ResponseEntity.ok(CodeUtils.generateRandom(type));
    }
}
