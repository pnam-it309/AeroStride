package com.example.be.infrastructure.batch;

import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class ExcelItemReader<T> implements ItemReader<T> {

    private final String filePath;
    private Workbook workbook;
    private Iterator<Row> rowIterator;
    
    @Setter
    private ExcelRowMapper<T> rowMapper;

    public ExcelItemReader(@Value("${batch.excel.file-path:}") String filePath) {
        this.filePath = filePath;
    }

    @Override
    public T read() throws Exception {
        if (rowIterator == null) {
            initReader();
        }

        if (rowIterator != null && rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (rowMapper != null) {
                return rowMapper.mapRow(row);
            }
        }

        close();
        return null;
    }

    private void initReader() throws IOException {
        if (filePath == null || filePath.isEmpty()) return;
        
        File file = new File(filePath);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            this.workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheetAt(0);
            this.rowIterator = sheet.rowIterator();
            
            // Skip header row
            if (this.rowIterator.hasNext()) {
                this.rowIterator.next();
            }
        }
    }

    private void close() throws IOException {
        if (workbook != null) {
            workbook.close();
            workbook = null;
            rowIterator = null;
        }
    }
}
