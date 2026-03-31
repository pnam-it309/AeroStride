package com.example.be.infrastructure.batch;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DatabaseItemWriter<T> implements ItemWriter<T> {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void write(Chunk<? extends T> items) throws Exception {
        for (T item : items) {
            entityManager.persist(item);
        }
        entityManager.flush();
    }
}
