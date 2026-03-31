package com.example.be.infrastructure.batch;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ValidationItemProcessor<I, O> implements ItemProcessor<I, O> {

    private final Validator validator;

    @Override
    @SuppressWarnings("unchecked")
    public O process(I item) throws Exception {
        Set<ConstraintViolation<I>> violations = validator.validate(item);
        if (!violations.isEmpty()) {
            // Log or handle validation errors
            return null; // Skip this item
        }
        return (O) item;
    }
}
