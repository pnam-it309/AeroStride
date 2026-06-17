package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCaLam is a Querydsl query type for CaLam
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCaLam extends EntityPathBase<CaLam> {

    private static final long serialVersionUID = -841958327L;

    public static final QCaLam caLam = new QCaLam("caLam");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final TimePath<java.time.LocalTime> gioBatDau = createTime("gioBatDau", java.time.LocalTime.class);

    public final TimePath<java.time.LocalTime> gioKetThuc = createTime("gioKetThuc", java.time.LocalTime.class);

    //inherited
    public final StringPath id = _super.id;

    public final StringPath moTa = createString("moTa");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final StringPath tenCa = createString("tenCa");

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final BooleanPath xoaMem = createBoolean("xoaMem");

    public QCaLam(String variable) {
        super(CaLam.class, forVariable(variable));
    }

    public QCaLam(Path<? extends CaLam> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCaLam(PathMetadata metadata) {
        super(CaLam.class, metadata);
    }

}

