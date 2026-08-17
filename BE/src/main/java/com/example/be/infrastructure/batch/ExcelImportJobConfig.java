package com.example.be.infrastructure.batch;

import com.example.be.entity.LichLamViec;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.batch.mapper.LichLamViecRowMapper;
import com.example.be.infrastructure.batch.mapper.PhieuGiamGiaRowMapper;
import com.example.be.infrastructure.constants.BatchConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ExcelImportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DatabaseItemWriter<Object> databaseItemWriter;
    private final ValidationItemProcessor<Object, Object> validationItemProcessor;

    // --- Voucher Import Batch Config ---

    @Bean
    @StepScope
    public ExcelItemReader<PhieuGiamGia> voucherExcelReader(
            @Value("#{jobParameters['" + BatchConstants.PARAM_FILE_PATH + "']}") String filePath,
            PhieuGiamGiaRowMapper rowMapper) {
        return new ExcelItemReader<>(filePath, rowMapper);
    }

    @Bean
    public Step voucherImportStep(
            ExcelItemReader<PhieuGiamGia> voucherExcelReader,
            DatabaseItemWriter<Object> writer) {
        return new StepBuilder(BatchConstants.VOUCHER_IMPORT_STEP, jobRepository)
                .<PhieuGiamGia, PhieuGiamGia>chunk(BatchConstants.BATCH_SIZE, transactionManager)
                .reader(voucherExcelReader)
                .writer(chunk -> {
                    for (PhieuGiamGia item : chunk) {
                        writer.write(new org.springframework.batch.item.Chunk<>(java.util.Collections.singletonList(item)));
                    }
                })
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    public Job voucherImportJob(Step voucherImportStep) {
        return new JobBuilder(BatchConstants.VOUCHER_IMPORT_JOB, jobRepository)
                .start(voucherImportStep)
                .build();
    }

    // --- Schedule Import Batch Config ---

    @Bean
    @StepScope
    public ExcelItemReader<LichLamViec> scheduleExcelReader(
            @Value("#{jobParameters['" + BatchConstants.PARAM_FILE_PATH + "']}") String filePath,
            LichLamViecRowMapper rowMapper) {
        return new ExcelItemReader<>(filePath, rowMapper);
    }

    @Bean
    public Step scheduleImportStep(
            ExcelItemReader<LichLamViec> scheduleExcelReader,
            DatabaseItemWriter<Object> writer) {
        return new StepBuilder(BatchConstants.SCHEDULE_IMPORT_STEP, jobRepository)
                .<LichLamViec, LichLamViec>chunk(BatchConstants.BATCH_SIZE, transactionManager)
                .reader(scheduleExcelReader)
                .writer(chunk -> {
                    for (LichLamViec item : chunk) {
                        writer.write(new org.springframework.batch.item.Chunk<>(java.util.Collections.singletonList(item)));
                    }
                })
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    public Job scheduleImportJob(Step scheduleImportStep) {
        return new JobBuilder(BatchConstants.SCHEDULE_IMPORT_JOB, jobRepository)
                .start(scheduleImportStep)
                .build();
    }
}
