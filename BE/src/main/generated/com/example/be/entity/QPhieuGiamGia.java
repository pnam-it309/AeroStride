package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPhieuGiamGia is a Querydsl query type for PhieuGiamGia
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPhieuGiamGia extends EntityPathBase<PhieuGiamGia> {

    private static final long serialVersionUID = 463315041L;

    public static final QPhieuGiamGia phieuGiamGia = new QPhieuGiamGia("phieuGiamGia");

    public final com.example.be.core.common.base.QBaseCodeNameEntity _super = new com.example.be.core.common.base.QBaseCodeNameEntity(this);

    public final NumberPath<java.math.BigDecimal> donHangToiThieu = createNumber("donHangToiThieu", java.math.BigDecimal.class);

    public final StringPath ghiChu = createString("ghiChu");

    public final NumberPath<java.math.BigDecimal> giamToiDa = createNumber("giamToiDa", java.math.BigDecimal.class);

    public final StringPath hinhThuc = createString("hinhThuc");

    //inherited
    public final StringPath id = _super.id;

    public final StringPath loaiPhieu = createString("loaiPhieu");

    //inherited
    public final StringPath ma = _super.ma;

    //inherited
    public final StringPath moTa = _super.moTa;

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

    public final NumberPath<Integer> phanTramGiamGia = createNumber("phanTramGiamGia", Integer.class);

    public final NumberPath<Integer> soLuong = createNumber("soLuong", Integer.class);

    public final NumberPath<java.math.BigDecimal> soTienGiam = createNumber("soTienGiam", java.math.BigDecimal.class);

    //inherited
    public final StringPath ten = _super.ten;

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public QPhieuGiamGia(String variable) {
        super(PhieuGiamGia.class, forVariable(variable));
    }

    public QPhieuGiamGia(Path<? extends PhieuGiamGia> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPhieuGiamGia(PathMetadata metadata) {
        super(PhieuGiamGia.class, metadata);
    }

}

