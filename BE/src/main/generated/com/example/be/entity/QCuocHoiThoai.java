package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCuocHoiThoai is a Querydsl query type for CuocHoiThoai
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCuocHoiThoai extends EntityPathBase<CuocHoiThoai> {

    private static final long serialVersionUID = -653225000L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCuocHoiThoai cuocHoiThoai = new QCuocHoiThoai("cuocHoiThoai");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final BooleanPath daChapNhan = createBoolean("daChapNhan");

    public final ListPath<TinNhan, QTinNhan> danhSachTinNhan = this.<TinNhan, QTinNhan>createList("danhSachTinNhan", TinNhan.class, QTinNhan.class, PathInits.DIRECT2);

    //inherited
    public final StringPath id = _super.id;

    public final QKhachHang khachHang;

    public final EnumPath<CuocHoiThoai.LoaiHoiThoai> loaiHoiThoai = createEnum("loaiHoiThoai", CuocHoiThoai.LoaiHoiThoai.class);

    public final StringPath maPhien = createString("maPhien");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final QNhanVien nhanVien;

    public final QNhanVien nhanVienNhan;

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final EnumPath<CuocHoiThoai.TrangThaiHoiThoai> trangThaiHoiThoai = createEnum("trangThaiHoiThoai", CuocHoiThoai.TrangThaiHoiThoai.class);

    public QCuocHoiThoai(String variable) {
        this(CuocHoiThoai.class, forVariable(variable), INITS);
    }

    public QCuocHoiThoai(Path<? extends CuocHoiThoai> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCuocHoiThoai(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCuocHoiThoai(PathMetadata metadata, PathInits inits) {
        this(CuocHoiThoai.class, metadata, inits);
    }

    public QCuocHoiThoai(Class<? extends CuocHoiThoai> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.khachHang = inits.isInitialized("khachHang") ? new QKhachHang(forProperty("khachHang"), inits.get("khachHang")) : null;
        this.nhanVien = inits.isInitialized("nhanVien") ? new QNhanVien(forProperty("nhanVien"), inits.get("nhanVien")) : null;
        this.nhanVienNhan = inits.isInitialized("nhanVienNhan") ? new QNhanVien(forProperty("nhanVienNhan"), inits.get("nhanVienNhan")) : null;
    }

}

