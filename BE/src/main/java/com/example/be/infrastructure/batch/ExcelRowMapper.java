package com.example.be.infrastructure.batch;

import org.apache.poi.ss.usermodel.Row;

public interface ExcelRowMapper<T> {
    T mapRow(Row row) throws Exception;
}
