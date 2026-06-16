package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QThuongHieu is a Querydsl query type for ThuongHieu
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QThuongHieu extends EntityPathBase<ThuongHieu> {

    private static final long serialVersionUID = 1975525833L;

    public static final QThuongHieu thuongHieu = new QThuongHieu("thuongHieu");

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

    public QThuongHieu(String variable) {
        super(ThuongHieu.class, forVariable(variable));
    }

    public QThuongHieu(Path<? extends ThuongHieu> path) {
        super(path.getType(), path.getMetadata());
    }

    public QThuongHieu(PathMetadata metadata) {
        super(ThuongHieu.class, metadata);
    }

}

