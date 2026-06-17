package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDanhMuc is a Querydsl query type for DanhMuc
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDanhMuc extends EntityPathBase<DanhMuc> {

    private static final long serialVersionUID = -749015693L;

    public static final QDanhMuc danhMuc = new QDanhMuc("danhMuc");

    public final com.example.be.core.common.base.QBaseCodeNameEntity _super = new com.example.be.core.common.base.QBaseCodeNameEntity(this);

    //inherited
    public final StringPath id = _super.id;

    //inherited
    public final StringPath ma = _super.ma;

    //inherited
    public final StringPath moTa = _super.moTa;

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    //inherited
    public final StringPath ten = _super.ten;

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final BooleanPath xoaMem = createBoolean("xoaMem");

    public QDanhMuc(String variable) {
        super(DanhMuc.class, forVariable(variable));
    }

    public QDanhMuc(Path<? extends DanhMuc> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDanhMuc(PathMetadata metadata) {
        super(DanhMuc.class, metadata);
    }

}

