package com.example.be.core.common.base;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPrimaryEntity is a Querydsl query type for PrimaryEntity
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QPrimaryEntity extends EntityPathBase<PrimaryEntity> {

    private static final long serialVersionUID = -1090678090L;

    public static final QPrimaryEntity primaryEntity = new QPrimaryEntity("primaryEntity");

    public final QAuditEntity _super = new QAuditEntity(this);

    public final StringPath id = createString("id");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = createEnum("trangThai", com.example.be.infrastructure.constants.TrangThai.class);

    public QPrimaryEntity(String variable) {
        super(PrimaryEntity.class, forVariable(variable));
    }

    public QPrimaryEntity(Path<? extends PrimaryEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPrimaryEntity(PathMetadata metadata) {
        super(PrimaryEntity.class, metadata);
    }

}

