package com.example.be.core.common.base;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAuditEntity is a Querydsl query type for AuditEntity
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QAuditEntity extends EntityPathBase<AuditEntity> {

    private static final long serialVersionUID = 44331535L;

    public static final QAuditEntity auditEntity = new QAuditEntity("auditEntity");

    public final NumberPath<Long> ngayCapNhat = createNumber("ngayCapNhat", Long.class);

    public final NumberPath<Long> ngayTao = createNumber("ngayTao", Long.class);

    public final StringPath nguoiCapNhat = createString("nguoiCapNhat");

    public final StringPath nguoiTao = createString("nguoiTao");

    public QAuditEntity(String variable) {
        super(AuditEntity.class, forVariable(variable));
    }

    public QAuditEntity(Path<? extends AuditEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAuditEntity(PathMetadata metadata) {
        super(AuditEntity.class, metadata);
    }

}

