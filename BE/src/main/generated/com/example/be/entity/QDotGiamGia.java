package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDotGiamGia is a Querydsl query type for DotGiamGia
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDotGiamGia extends EntityPathBase<DotGiamGia> {

    private static final long serialVersionUID = -1973341127L;

    public static final QDotGiamGia dotGiamGia = new QDotGiamGia("dotGiamGia");

    public final com.example.be.core.common.base.QBaseCodeNameEntity _super = new com.example.be.core.common.base.QBaseCodeNameEntity(this);

    public final NumberPath<java.math.BigDecimal> dieuKienGiamGia = createNumber("dieuKienGiamGia", java.math.BigDecimal.class);

    //inherited
    public final StringPath id = _super.id;

    public final StringPath loaiGiamGia = createString("loaiGiamGia");

    //inherited
    public final StringPath ma = _super.ma;

    public final StringPath moTa = createString("moTa");

    public final NumberPath<Integer> mucUuTien = createNumber("mucUuTien", Integer.class);

    public final NumberPath<Long> ngayBatDau = createNumber("ngayBatDau", Long.class);

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    public final NumberPath<Long> ngayKetThuc = createNumber("ngayKetThuc", Long.class);

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final NumberPath<java.math.BigDecimal> soTienGiam = createNumber("soTienGiam", java.math.BigDecimal.class);

    //inherited
    public final StringPath ten = _super.ten;

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public QDotGiamGia(String variable) {
        super(DotGiamGia.class, forVariable(variable));
    }

    public QDotGiamGia(Path<? extends DotGiamGia> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDotGiamGia(PathMetadata metadata) {
        super(DotGiamGia.class, metadata);
    }

}

