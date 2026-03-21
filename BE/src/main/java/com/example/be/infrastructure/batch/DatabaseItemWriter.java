package com.example.be.infrastructure.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseItemWriter<T> implements ItemWriter<T> {

    @Override
    public void write(Chunk<? extends T> items) throws Exception {
        // TODO: Use JPA Repository or EntityManager for bulk insert
        System.out.println("Writing " + items.size() + " items to database...");
    }
}
