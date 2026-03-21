package com.example.be.infrastructure.batch;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class ValidationItemProcessor<I, O> implements ItemProcessor<I, O> {

    @Override
    @SuppressWarnings("unchecked")
    public O process(I item) throws Exception {
        // TODO: Implement validation logic
        // If invalid, return null to skip or throw exception to fail job
        return (O) item;
    }
}
