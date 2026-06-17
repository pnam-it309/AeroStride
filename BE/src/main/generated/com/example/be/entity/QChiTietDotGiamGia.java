package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChiTietDotGiamGia is a Querydsl query type for ChiTietDotGiamGia
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChiTietDotGiamGia extends EntityPathBase<ChiTietDotGiamGia> {

    private static final long serialVersionUID = 486207231L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChiTietDotGiamGia chiTietDotGiamGia = new QChiTietDotGiamGia("chiTietDotGiamGia");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final QChiTietSanPham chiTietSanPham;

    public final QDotGiamGia dotGiamGia;

    public final NumberPath<java.math.BigDecimal> giaTriGiam = createNumber("giaTriGiam", java.math.BigDecimal.class);

    //inherited
    public final StringPath id = _super.id;

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public QChiTietDotGiamGia(String variable) {
        this(ChiTietDotGiamGia.class, forVariable(variable), INITS);
    }

    public QChiTietDotGiamGia(Path<? extends ChiTietDotGiamGia> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChiTietDotGiamGia(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChiTietDotGiamGia(PathMetadata metadata, PathInits inits) {
        this(ChiTietDotGiamGia.class, metadata, inits);
    }

    public QChiTietDotGiamGia(Class<? extends ChiTietDotGiamGia> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chiTietSanPham = inits.isInitialized("chiTietSanPham") ? new QChiTietSanPham(forProperty("chiTietSanPham"), inits.get("chiTietSanPham")) : null;
        this.dotGiamGia = inits.isInitialized("dotGiamGia") ? new QDotGiamGia(forProperty("dotGiamGia")) : null;
    }

}

