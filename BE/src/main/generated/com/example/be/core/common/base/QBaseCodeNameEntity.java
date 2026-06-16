package com.example.be.core.common.base;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBaseCodeNameEntity is a Querydsl query type for BaseCodeNameEntity
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QBaseCodeNameEntity extends EntityPathBase<BaseCodeNameEntity> {

    private static final long serialVersionUID = -1214298693L;

    public static final QBaseCodeNameEntity baseCodeNameEntity = new QBaseCodeNameEntity("baseCodeNameEntity");

    public final QPrimaryEntity _super = new QPrimaryEntity(this);

    //inherited
    public final StringPath id = _super.id;

    public final StringPath ma = createString("ma");

    public final StringPath moTa = createString("moTa");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final StringPath ten = createString("ten");

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public QBaseCodeNameEntity(String variable) {
        super(BaseCodeNameEntity.class, forVariable(variable));
    }

    public QBaseCodeNameEntity(Path<? extends BaseCodeNameEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBaseCodeNameEntity(PathMetadata metadata) {
        super(BaseCodeNameEntity.class, metadata);
    }

}

