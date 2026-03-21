package com.example.be.core.shoe;

import com.example.be.entity.Shoe;
import com.example.be.infrastructure.constants.EntityStatus;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.repository.ShoeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShoeService {

    private final ShoeRepository shoeRepository;

    /**
     * Paginated retrieval of shoes with caching.
     */
    @Cacheable(value = "shoes", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<Shoe> getAllShoes(Pageable pageable) {
        log.info("Fetching shoes from database (paginated)...");
        return shoeRepository.findAllByStatusNot(EntityStatus.DELETED, pageable);
    }

    public Shoe getShoeById(UUID id) {
        return shoeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shoe not found with ID: " + id));
    }

    @Transactional
    @CacheEvict(value = "shoes", allEntries = true)
    public Shoe addShoe(Shoe shoe) {
        log.info("Adding new shoe: {}", shoe.getName());
        return shoeRepository.save(shoe);
    }

    @Transactional
    @CacheEvict(value = "shoes", allEntries = true)
    public Shoe updateShoe(UUID id, Shoe shoeDetails) {
        log.info("Updating shoe with ID: {}", id);
        Shoe existingShoe = getShoeById(id);
        
        existingShoe.setName(shoeDetails.getName());
        existingShoe.setPrice(shoeDetails.getPrice());
        existingShoe.setStock(shoeDetails.getStock());
        existingShoe.setBrand(shoeDetails.getBrand());
        existingShoe.setCategory(shoeDetails.getCategory());
        existingShoe.setImageUrl(shoeDetails.getImageUrl());
        existingShoe.setStatus(shoeDetails.getStatus());
        
        return shoeRepository.save(existingShoe);
    }

    @Transactional
    @CacheEvict(value = "shoes", allEntries = true)
    public void deleteShoe(UUID id) {
        log.info("Deleting (soft delete) shoe with ID: {}", id);
        Shoe shoe = getShoeById(id);
        shoe.setStatus(EntityStatus.DELETED);
        shoeRepository.save(shoe);
    }
}

