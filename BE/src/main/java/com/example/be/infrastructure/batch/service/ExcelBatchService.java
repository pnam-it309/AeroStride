package com.example.be.infrastructure.batch.service;

import com.example.be.infrastructure.constants.BatchConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelBatchService {

    private final JobLauncher jobLauncher;
    private final Job voucherImportJob;
    private final Job scheduleImportJob;

    public ExcelBatchResult runVoucherImportJob(MultipartFile file) {
        return executeExcelBatchJob(voucherImportJob, file, "Voucher");
    }

    public ExcelBatchResult runScheduleImportJob(MultipartFile file) {
        return executeExcelBatchJob(scheduleImportJob, file, "Schedule");
    }

    private ExcelBatchResult executeExcelBatchJob(Job job, MultipartFile file, String jobType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp Excel không được để trống!");
        }

        File tempFile = null;
        long startTime = System.currentTimeMillis();

        try {
            // 1. Lưu file tạm thời
            Path tempDir = Files.createTempDirectory("aerostride_batch_");
            tempFile = tempDir.resolve("upload_" + System.currentTimeMillis() + ".xlsx").toFile();

            try (InputStream is = file.getInputStream(); FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // 2. Thiết lập JobParameters
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString(BatchConstants.PARAM_FILE_PATH, tempFile.getAbsolutePath())
                    .addString(BatchConstants.PARAM_JOB_TYPE, jobType)
                    .addLong(BatchConstants.PARAM_TIMESTAMP, System.currentTimeMillis())
                    .toJobParameters();

            // 3. Khởi chạy Job qua JobLauncher
            log.info("Starting Spring Batch Excel import job: {} for file: {}", job.getName(), file.getOriginalFilename());
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            // 4. Tổng hợp số liệu thực thi từ các StepExecution
            long readCount = 0;
            long writeCount = 0;
            long skipCount = 0;

            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                readCount += stepExecution.getReadCount();
                writeCount += stepExecution.getWriteCount();
                skipCount += stepExecution.getSkipCount();
            }

            long duration = System.currentTimeMillis() - startTime;
            String status = jobExecution.getStatus().name();

            log.info("Finished Spring Batch Job [{}] with status: {}. Read: {}, Written: {}, Skipped: {}, Duration: {}ms",
                    job.getName(), status, readCount, writeCount, skipCount, duration);

            return ExcelBatchResult.builder()
                    .jobExecutionId(jobExecution.getId())
                    .status(status)
                    .readCount(readCount)
                    .writeCount(writeCount)
                    .skipCount(skipCount)
                    .durationMs(duration)
                    .message("Nhập dữ liệu thành công " + writeCount + " bản ghi (Bỏ qua/lỗi: " + skipCount + ")")
                    .build();

        } catch (Exception e) {
            log.error("Error executing Excel Spring Batch Job [{}]: {}", job.getName(), e.getMessage(), e);
            throw new RuntimeException("Lỗi thực thi Spring Batch Excel: " + e.getMessage(), e);
        } finally {
            // 5. Dọn dẹp tệp tạm
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                    if (tempFile.getParentFile() != null) {
                        Files.deleteIfExists(tempFile.getParentFile().toPath());
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
