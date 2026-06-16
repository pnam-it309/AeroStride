package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QHoaDonChiTiet is a Querydsl query type for HoaDonChiTiet
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHoaDonChiTiet extends EntityPathBase<HoaDonChiTiet> {

    private static final long serialVersionUID = -2138932562L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QHoaDonChiTiet hoaDonChiTiet = new QHoaDonChiTiet("hoaDonChiTiet");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final QChiTietSanPham chiTietSanPham;

    public final NumberPath<java.math.BigDecimal> donGia = createNumber("donGia", java.math.BigDecimal.class);

    public final QHoaDon hoaDon;

    //inherited
    public final StringPath id = _super.id;

    public final StringPath maHoaDonChiTiet = createString("maHoaDonChiTiet");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final NumberPath<Integer> soLuong = createNumber("soLuong", Integer.class);

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public QHoaDonChiTiet(String variable) {
        this(HoaDonChiTiet.class, forVariable(variable), INITS);
    }

    public QHoaDonChiTiet(Path<? extends HoaDonChiTiet> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QHoaDonChiTiet(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QHoaDonChiTiet(PathMetadata metadata, PathInits inits) {
        this(HoaDonChiTiet.class, metadata, inits);
    }

    public QHoaDonChiTiet(Class<? extends HoaDonChiTiet> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chiTietSanPham = inits.isInitialized("chiTietSanPham") ? new QChiTietSanPham(forProperty("chiTietSanPham"), inits.get("chiTietSanPham")) : null;
        this.hoaDon = inits.isInitialized("hoaDon") ? new QHoaDon(forProperty("hoaDon"), inits.get("hoaDon")) : null;
    }

}

