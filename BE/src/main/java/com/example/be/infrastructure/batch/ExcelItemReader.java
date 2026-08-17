package com.example.be.infrastructure.batch;

import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

public class ExcelItemReader<T> implements ItemStreamReader<T> {

    private final String filePath;
    private Workbook workbook;
    private FileInputStream fis;
    private Iterator<Row> rowIterator;

    @Setter
    private ExcelRowMapper<T> rowMapper;

    public ExcelItemReader(String filePath, ExcelRowMapper<T> rowMapper) {
        this.filePath = filePath;
        this.rowMapper = rowMapper;
    }

    public ExcelItemReader(String filePath) {
        this(filePath, null);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new ItemStreamException("Excel file not found: " + filePath);
        }

        try {
            this.fis = new FileInputStream(file);
            this.workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheetAt(0);
            this.rowIterator = sheet.rowIterator();

            // Skip header row
            if (this.rowIterator != null && this.rowIterator.hasNext()) {
                this.rowIterator.next();
            }
        } catch (Exception e) {
            close();
            throw new ItemStreamException("Failed to open and initialize Excel file: " + filePath, e);
        }
    }

    @Override
    public T read() throws Exception {
        if (rowIterator == null) {
            return null;
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (rowMapper != null) {
                T item = rowMapper.mapRow(row);
                if (item != null) {
                    return item;
                }
            }
        }

        return null;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // No-op for read-only stream
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            if (workbook != null) {
                workbook.close();
                workbook = null;
            }
            if (fis != null) {
                fis.close();
                fis = null;
            }
            rowIterator = null;
        } catch (IOException e) {
            throw new ItemStreamException("Error closing Excel resources", e);
        }
    }
}
