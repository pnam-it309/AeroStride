package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNhanVien is a Querydsl query type for NhanVien
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNhanVien extends EntityPathBase<NhanVien> {

    private static final long serialVersionUID = 54761556L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNhanVien nhanVien = new QNhanVien("nhanVien");

    public final com.example.be.core.common.base.QBaseCodeNameEntity _super = new com.example.be.core.common.base.QBaseCodeNameEntity(this);

    public final StringPath diaChiChiTiet = createString("diaChiChiTiet");

    public final StringPath email = createString("email");

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

    public final QPhanQuyen phanQuyen;

    public final StringPath phuongXa = createString("phuongXa");

    public final DateTimePath<java.time.LocalDateTime> resetRequestedAt = createDateTime("resetRequestedAt", java.time.LocalDateTime.class);

    public final EnumPath<NhanVien.ResetStatus> resetStatus = createEnum("resetStatus", NhanVien.ResetStatus.class);

    public final StringPath sdt = createString("sdt");

    //inherited
    public final StringPath ten = _super.ten;

    public final StringPath tenTaiKhoan = createString("tenTaiKhoan");

    public final StringPath thanhPho = createString("thanhPho");

    public final StringPath tinh = createString("tinh");

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final BooleanPath xoaMem = createBoolean("xoaMem");

    public QNhanVien(String variable) {
        this(NhanVien.class, forVariable(variable), INITS);
    }

    public QNhanVien(Path<? extends NhanVien> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNhanVien(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNhanVien(PathMetadata metadata, PathInits inits) {
        this(NhanVien.class, metadata, inits);
    }

    public QNhanVien(Class<? extends NhanVien> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.phanQuyen = inits.isInitialized("phanQuyen") ? new QPhanQuyen(forProperty("phanQuyen")) : null;
    }

}

