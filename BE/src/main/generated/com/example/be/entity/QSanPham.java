package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSanPham is a Querydsl query type for SanPham
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSanPham extends EntityPathBase<SanPham> {

    private static final long serialVersionUID = -322052013L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSanPham sanPham = new QSanPham("sanPham");

    public final com.example.be.core.common.base.QBaseCodeNameEntity _super = new com.example.be.core.common.base.QBaseCodeNameEntity(this);

    public final QChatLieu chatLieu;

    public final ListPath<ChiTietSanPham, QChiTietSanPham> chiTietSanPhams = this.<ChiTietSanPham, QChiTietSanPham>createList("chiTietSanPhams", ChiTietSanPham.class, QChiTietSanPham.class, PathInits.DIRECT2);

    public final QCoGiay coGiay;

    public final QDanhMuc danhMuc;

    public final QDeGiay deGiay;

    public final EnumPath<com.example.be.infrastructure.constants.GioiTinhKhachHang> gioiTinhKhachHang = createEnum("gioiTinhKhachHang", com.example.be.infrastructure.constants.GioiTinhKhachHang.class);

    public final StringPath hinhAnh = createString("hinhAnh");

    //inherited
    public final StringPath id = _super.id;

    //inherited
    public final StringPath ma = _super.ma;

    //inherited
    public final StringPath moTa = _super.moTa;

    public final StringPath moTaChiTiet = createString("moTaChiTiet");

    public final QMucDichChay mucDichChay;

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    //inherited
    public final StringPath ten = _super.ten;

    public final QThuongHieu thuongHieu;

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final BooleanPath xoaMem = createBoolean("xoaMem");

    public final QXuatXu xuatXu;

    public QSanPham(String variable) {
        this(SanPham.class, forVariable(variable), INITS);
    }

    public QSanPham(Path<? extends SanPham> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSanPham(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSanPham(PathMetadata metadata, PathInits inits) {
        this(SanPham.class, metadata, inits);
    }

    public QSanPham(Class<? extends SanPham> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chatLieu = inits.isInitialized("chatLieu") ? new QChatLieu(forProperty("chatLieu")) : null;
        this.coGiay = inits.isInitialized("coGiay") ? new QCoGiay(forProperty("coGiay")) : null;
        this.danhMuc = inits.isInitialized("danhMuc") ? new QDanhMuc(forProperty("danhMuc")) : null;
        this.deGiay = inits.isInitialized("deGiay") ? new QDeGiay(forProperty("deGiay")) : null;
        this.mucDichChay = inits.isInitialized("mucDichChay") ? new QMucDichChay(forProperty("mucDichChay")) : null;
        this.thuongHieu = inits.isInitialized("thuongHieu") ? new QThuongHieu(forProperty("thuongHieu")) : null;
        this.xuatXu = inits.isInitialized("xuatXu") ? new QXuatXu(forProperty("xuatXu")) : null;
    }

}

