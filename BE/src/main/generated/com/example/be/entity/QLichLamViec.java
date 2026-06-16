package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLichLamViec is a Querydsl query type for LichLamViec
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLichLamViec extends EntityPathBase<LichLamViec> {

    private static final long serialVersionUID = 1017104374L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLichLamViec lichLamViec = new QLichLamViec("lichLamViec");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final QCaLam caLam;

    //inherited
    public final StringPath id = _super.id;

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    public final DatePath<java.time.LocalDate> ngayLam = createDate("ngayLam", java.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final QNhanVien nhanVien;

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final EnumPath<LichLamViec.TrangThaiLichLamViec> trangThaiLich = createEnum("trangThaiLich", LichLamViec.TrangThaiLichLamViec.class);

    public QLichLamViec(String variable) {
        this(LichLamViec.class, forVariable(variable), INITS);
    }

    public QLichLamViec(Path<? extends LichLamViec> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLichLamViec(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLichLamViec(PathMetadata metadata, PathInits inits) {
        this(LichLamViec.class, metadata, inits);
    }

    public QLichLamViec(Class<? extends LichLamViec> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.caLam = inits.isInitialized("caLam") ? new QCaLam(forProperty("caLam")) : null;
        this.nhanVien = inits.isInitialized("nhanVien") ? new QNhanVien(forProperty("nhanVien"), inits.get("nhanVien")) : null;
    }

}

