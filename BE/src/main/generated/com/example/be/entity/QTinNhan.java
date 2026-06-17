package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTinNhan is a Querydsl query type for TinNhan
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTinNhan extends EntityPathBase<TinNhan> {

    private static final long serialVersionUID = 794425295L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTinNhan tinNhan = new QTinNhan("tinNhan");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final QCuocHoiThoai cuocHoiThoai;

    //inherited
    public final StringPath id = _super.id;

    public final StringPath idNguoiGui = createString("idNguoiGui");

    public final StringPath loaiNguoiGui = createString("loaiNguoiGui");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final StringPath noiDung = createString("noiDung");

    public final StringPath tenNguoiGui = createString("tenNguoiGui");

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public QTinNhan(String variable) {
        this(TinNhan.class, forVariable(variable), INITS);
    }

    public QTinNhan(Path<? extends TinNhan> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTinNhan(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTinNhan(PathMetadata metadata, PathInits inits) {
        this(TinNhan.class, metadata, inits);
    }

    public QTinNhan(Class<? extends TinNhan> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.cuocHoiThoai = inits.isInitialized("cuocHoiThoai") ? new QCuocHoiThoai(forProperty("cuocHoiThoai"), inits.get("cuocHoiThoai")) : null;
    }

}

