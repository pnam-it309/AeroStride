package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QKhachHang is a Querydsl query type for KhachHang
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QKhachHang extends EntityPathBase<KhachHang> {

    private static final long serialVersionUID = -1769247094L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QKhachHang khachHang = new QKhachHang("khachHang");

    public final com.example.be.core.common.base.QBaseCodeNameEntity _super = new com.example.be.core.common.base.QBaseCodeNameEntity(this);

    public final QDiaChi diaChi;

    public final StringPath email = createString("email");

    public final StringPath ghiChu = createString("ghiChu");

    public final BooleanPath gioiTinh = createBoolean("gioiTinh");

    public final StringPath hinhAnh = createString("hinhAnh");

    //inherited
    public final StringPath id = _super.id;

    //inherited
    public final StringPath ma = _super.ma;

    public final StringPath matKhau = createString("matKhau");

    //inherited
    public final StringPath moTa = _super.moTa;

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    public final DatePath<java.time.LocalDate> ngaySinh = createDate("ngaySinh", java.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final StringPath sdt = createString("sdt");

    //inherited
    public final StringPath ten = _super.ten;

    public final StringPath tenTaiKhoan = createString("tenTaiKhoan");

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final BooleanPath xoaMem = createBoolean("xoaMem");

    public QKhachHang(String variable) {
        this(KhachHang.class, forVariable(variable), INITS);
    }

    public QKhachHang(Path<? extends KhachHang> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QKhachHang(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QKhachHang(PathMetadata metadata, PathInits inits) {
        this(KhachHang.class, metadata, inits);
    }

    public QKhachHang(Class<? extends KhachHang> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.diaChi = inits.isInitialized("diaChi") ? new QDiaChi(forProperty("diaChi"), inits.get("diaChi")) : null;
    }

}

