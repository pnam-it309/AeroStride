package com.example.be.infrastructure.batch;

import com.example.be.core.common.constants.BatchConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ExcelImportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job excelImportJob(Step excelImportStep) {
        return new JobBuilder(BatchConstants.EXCEL_IMPORT_JOB, jobRepository)
                .start(excelImportStep)
                .build();
    }

    @Bean
    public Step excelImportStep(
            ExcelItemReader<Object> reader,
            ValidationItemProcessor<Object, Object> processor,
            DatabaseItemWriter<Object> writer) {
        return new StepBuilder(BatchConstants.EXCEL_IMPORT_STEP, jobRepository)
                .<Object, Object>chunk(BatchConstants.BATCH_SIZE, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
