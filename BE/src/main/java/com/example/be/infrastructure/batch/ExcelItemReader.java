package com.example.be.infrastructure.batch;

import com.example.be.infrastructure.exceptions.ExcelProcessingException;
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

    public ExcelItemReader(@Value("${batch.excel.file-path:}") String filePath) {
        this.filePath = filePath;
    }

    @Override
    public T read() throws Exception {
        if (rowIterator == null) {
            initReader();
        }

        if (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            return mapRowToDto(row);
        }

        close();
        return null;
    }

    private void initReader() throws IOException {
        FileInputStream fis = new FileInputStream(new File(filePath));
        this.workbook = WorkbookFactory.create(fis);
        Sheet sheet = workbook.getSheetAt(0);
        this.rowIterator = sheet.rowIterator();
        
        // Skip header row if necessary
        if (this.rowIterator.hasNext()) {
            this.rowIterator.next();
        }
    }

    @SuppressWarnings("unchecked")
    private T mapRowToDto(Row row) {
        // TODO: Implement generic or specific row mapping logic
        return (T) row.toString(); 
    }

    private void close() throws IOException {
        if (workbook != null) {
            workbook.close();
        }
    }
}
