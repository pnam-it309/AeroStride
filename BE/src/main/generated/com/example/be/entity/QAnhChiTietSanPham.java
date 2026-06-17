package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAnhChiTietSanPham is a Querydsl query type for AnhChiTietSanPham
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAnhChiTietSanPham extends EntityPathBase<AnhChiTietSanPham> {

    private static final long serialVersionUID = -253144442L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAnhChiTietSanPham anhChiTietSanPham = new QAnhChiTietSanPham("anhChiTietSanPham");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final QChiTietSanPham chiTietSanPham;

    public final StringPath duongDanAnh = createString("duongDanAnh");

    public final BooleanPath hinhAnhDaiDien = createBoolean("hinhAnhDaiDien");

    //inherited
    public final StringPath id = _super.id;

    public final StringPath moTa = createString("moTa");

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

    public final BooleanPath xoaMem = createBoolean("xoaMem");

    public QAnhChiTietSanPham(String variable) {
        this(AnhChiTietSanPham.class, forVariable(variable), INITS);
    }

    public QAnhChiTietSanPham(Path<? extends AnhChiTietSanPham> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAnhChiTietSanPham(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAnhChiTietSanPham(PathMetadata metadata, PathInits inits) {
        this(AnhChiTietSanPham.class, metadata, inits);
    }

    public QAnhChiTietSanPham(Class<? extends AnhChiTietSanPham> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chiTietSanPham = inits.isInitialized("chiTietSanPham") ? new QChiTietSanPham(forProperty("chiTietSanPham"), inits.get("chiTietSanPham")) : null;
    }

}

