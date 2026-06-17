package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPhieuGiamGiaCaNhan is a Querydsl query type for PhieuGiamGiaCaNhan
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPhieuGiamGiaCaNhan extends EntityPathBase<PhieuGiamGiaCaNhan> {

    private static final long serialVersionUID = -1712147290L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPhieuGiamGiaCaNhan phieuGiamGiaCaNhan = new QPhieuGiamGiaCaNhan("phieuGiamGiaCaNhan");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final BooleanPath daSuDung = createBoolean("daSuDung");

    //inherited
    public final StringPath id = _super.id;

    public final QKhachHang khachHang;

    public final StringPath maPhieuGiamGiaCaNhan = createString("maPhieuGiamGiaCaNhan");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    public final NumberPath<Long> ngayNhan = createNumber("ngayNhan", Long.class);

    public final NumberPath<Long> ngaySuDung = createNumber("ngaySuDung", Long.class);

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final QPhieuGiamGia phieuGiamGia;

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final BooleanPath xoaMem = createBoolean("xoaMem");

    public QPhieuGiamGiaCaNhan(String variable) {
        this(PhieuGiamGiaCaNhan.class, forVariable(variable), INITS);
    }

    public QPhieuGiamGiaCaNhan(Path<? extends PhieuGiamGiaCaNhan> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPhieuGiamGiaCaNhan(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPhieuGiamGiaCaNhan(PathMetadata metadata, PathInits inits) {
        this(PhieuGiamGiaCaNhan.class, metadata, inits);
    }

    public QPhieuGiamGiaCaNhan(Class<? extends PhieuGiamGiaCaNhan> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.khachHang = inits.isInitialized("khachHang") ? new QKhachHang(forProperty("khachHang"), inits.get("khachHang")) : null;
        this.phieuGiamGia = inits.isInitialized("phieuGiamGia") ? new QPhieuGiamGia(forProperty("phieuGiamGia")) : null;
    }

}

