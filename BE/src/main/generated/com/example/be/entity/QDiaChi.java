package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDiaChi is a Querydsl query type for DiaChi
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDiaChi extends EntityPathBase<DiaChi> {

    private static final long serialVersionUID = -294290311L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDiaChi diaChi = new QDiaChi("diaChi");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final StringPath diaChiChiTiet = createString("diaChiChiTiet");

    //inherited
    public final StringPath id = _super.id;

    public final QKhachHang khachHang;

    public final BooleanPath laMacDinh = createBoolean("laMacDinh");

    public final StringPath maDiaChi = createString("maDiaChi");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final StringPath phuongXa = createString("phuongXa");

    public final StringPath sdtNguoiNhan = createString("sdtNguoiNhan");

    public final StringPath tenNguoiNhan = createString("tenNguoiNhan");

    public final StringPath thanhPho = createString("thanhPho");

    public final StringPath tinh = createString("tinh");

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public QDiaChi(String variable) {
        this(DiaChi.class, forVariable(variable), INITS);
    }

    public QDiaChi(Path<? extends DiaChi> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDiaChi(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDiaChi(PathMetadata metadata, PathInits inits) {
        this(DiaChi.class, metadata, inits);
    }

    public QDiaChi(Class<? extends DiaChi> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.khachHang = inits.isInitialized("khachHang") ? new QKhachHang(forProperty("khachHang"), inits.get("khachHang")) : null;
    }

}

