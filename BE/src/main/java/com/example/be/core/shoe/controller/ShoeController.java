package com.example.be.core.shoe.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.shoe.ShoeService;
import com.example.be.entity.Shoe;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ShoeController {

    private final ShoeService shoeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Shoe>>> getAll(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(shoeService.getAllShoes(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Shoe>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(shoeService.getShoeById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Shoe>> create(@RequestBody Shoe shoe) {
        return ResponseEntity.ok(ApiResponse.success(shoeService.addShoe(shoe), "Product created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Shoe>> update(@PathVariable UUID id, @RequestBody Shoe shoe) {
        return ResponseEntity.ok(ApiResponse.success(shoeService.updateShoe(id, shoe), "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        shoeService.deleteShoe(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }
}
