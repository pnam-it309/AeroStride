package com.example.be.infrastructure.constants;

public final class BatchConstants {
    private BatchConstants() {}

    public static final String EXCEL_IMPORT_JOB = "excelImportJob";
    public static final String EXCEL_IMPORT_STEP = "excelImportStep";
    public static final String VOUCHER_IMPORT_JOB = "voucherImportJob";
    public static final String VOUCHER_IMPORT_STEP = "voucherImportStep";
    public static final String SCHEDULE_IMPORT_JOB = "scheduleImportJob";
    public static final String SCHEDULE_IMPORT_STEP = "scheduleImportStep";

    public static final String PARAM_FILE_PATH = "filePath";
    public static final String PARAM_JOB_TYPE = "jobType";
    public static final String PARAM_TIMESTAMP = "timestamp";

    public static final int BATCH_SIZE = 50;
}
