package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLichSuTrangThaiHoaDon is a Querydsl query type for LichSuTrangThaiHoaDon
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLichSuTrangThaiHoaDon extends EntityPathBase<LichSuTrangThaiHoaDon> {

    private static final long serialVersionUID = -151253044L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLichSuTrangThaiHoaDon lichSuTrangThaiHoaDon = new QLichSuTrangThaiHoaDon("lichSuTrangThaiHoaDon");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final StringPath ghiChu = createString("ghiChu");

    public final QHoaDon hoaDon;

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

    public final StringPath nguoiThucHien = createString("nguoiThucHien");

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final NumberPath<Integer> trangThaiCu = createNumber("trangThaiCu", Integer.class);

    public final NumberPath<Integer> trangThaiMoi = createNumber("trangThaiMoi", Integer.class);

    public QLichSuTrangThaiHoaDon(String variable) {
        this(LichSuTrangThaiHoaDon.class, forVariable(variable), INITS);
    }

    public QLichSuTrangThaiHoaDon(Path<? extends LichSuTrangThaiHoaDon> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLichSuTrangThaiHoaDon(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLichSuTrangThaiHoaDon(PathMetadata metadata, PathInits inits) {
        this(LichSuTrangThaiHoaDon.class, metadata, inits);
    }

    public QLichSuTrangThaiHoaDon(Class<? extends LichSuTrangThaiHoaDon> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.hoaDon = inits.isInitialized("hoaDon") ? new QHoaDon(forProperty("hoaDon"), inits.get("hoaDon")) : null;
    }

}

