package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPhanQuyen is a Querydsl query type for PhanQuyen
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPhanQuyen extends EntityPathBase<PhanQuyen> {

    private static final long serialVersionUID = -1921541752L;

    public static final QPhanQuyen phanQuyen = new QPhanQuyen("phanQuyen");

    public final com.example.be.core.common.base.QBaseCodeNameEntity _super = new com.example.be.core.common.base.QBaseCodeNameEntity(this);

    //inherited
    public final StringPath id = _super.id;

    //inherited
    public final StringPath ma = _super.ma;

    public final StringPath moTa = createString("moTa");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final StringPath quyenHan = createString("quyenHan");

    //inherited
    public final StringPath ten = _super.ten;

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public QPhanQuyen(String variable) {
        super(PhanQuyen.class, forVariable(variable));
    }

    public QPhanQuyen(Path<? extends PhanQuyen> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPhanQuyen(PathMetadata metadata) {
        super(PhanQuyen.class, metadata);
    }

}

