package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGiaoDichThanhToan is a Querydsl query type for GiaoDichThanhToan
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGiaoDichThanhToan extends EntityPathBase<GiaoDichThanhToan> {

    private static final long serialVersionUID = 1211380132L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGiaoDichThanhToan giaoDichThanhToan = new QGiaoDichThanhToan("giaoDichThanhToan");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final StringPath duLieuPhanHoi = createString("duLieuPhanHoi");

    public final StringPath duLieuQr = createString("duLieuQr");

    public final StringPath duongDanThanhToan = createString("duongDanThanhToan");

    public final StringPath ghiChu = createString("ghiChu");

    public final QHoaDon hoaDon;

    //inherited
    public final StringPath id = _super.id;

    public final StringPath loaiGiaoDich = createString("loaiGiaoDich");

    public final StringPath maGiaoDichNgoai = createString("maGiaoDichNgoai");

    public final StringPath maGiaoDichThanhToan = createString("maGiaoDichThanhToan");

    public final StringPath maThamChieu = createString("maThamChieu");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final QPhuongThucThanhToan phuongThucThanhToan;

    public final NumberPath<java.math.BigDecimal> soTien = createNumber("soTien", java.math.BigDecimal.class);

    public final NumberPath<Long> thoiGianHetHan = createNumber("thoiGianHetHan", Long.class);

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public QGiaoDichThanhToan(String variable) {
        this(GiaoDichThanhToan.class, forVariable(variable), INITS);
    }

    public QGiaoDichThanhToan(Path<? extends GiaoDichThanhToan> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGiaoDichThanhToan(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGiaoDichThanhToan(PathMetadata metadata, PathInits inits) {
        this(GiaoDichThanhToan.class, metadata, inits);
    }

    public QGiaoDichThanhToan(Class<? extends GiaoDichThanhToan> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.hoaDon = inits.isInitialized("hoaDon") ? new QHoaDon(forProperty("hoaDon"), inits.get("hoaDon")) : null;
        this.phuongThucThanhToan = inits.isInitialized("phuongThucThanhToan") ? new QPhuongThucThanhToan(forProperty("phuongThucThanhToan")) : null;
    }

}

