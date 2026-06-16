package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QHoaDon is a Querydsl query type for HoaDon
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHoaDon extends EntityPathBase<HoaDon> {

    private static final long serialVersionUID = -174231398L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QHoaDon hoaDon = new QHoaDon("hoaDon");

    public final com.example.be.core.common.base.QAuditEntity _super = new com.example.be.core.common.base.QAuditEntity(this);

    public final StringPath diaChiNguoiNhan = createString("diaChiNguoiNhan");

    public final StringPath ghiChu = createString("ghiChu");

    public final StringPath id = createString("id");

    public final QKhachHang khachHang;

    public final SetPath<GiaoDichThanhToan, QGiaoDichThanhToan> listsGiaoDichThanhToan = this.<GiaoDichThanhToan, QGiaoDichThanhToan>createSet("listsGiaoDichThanhToan", GiaoDichThanhToan.class, QGiaoDichThanhToan.class, PathInits.DIRECT2);

    public final SetPath<HoaDonChiTiet, QHoaDonChiTiet> listsHoaDonChiTiet = this.<HoaDonChiTiet, QHoaDonChiTiet>createSet("listsHoaDonChiTiet", HoaDonChiTiet.class, QHoaDonChiTiet.class, PathInits.DIRECT2);

    public final SetPath<LichSuTrangThaiHoaDon, QLichSuTrangThaiHoaDon> listsLichSuHoaDon = this.<LichSuTrangThaiHoaDon, QLichSuTrangThaiHoaDon>createSet("listsLichSuHoaDon", LichSuTrangThaiHoaDon.class, QLichSuTrangThaiHoaDon.class, PathInits.DIRECT2);

    public final StringPath loaiDon = createString("loaiDon");

    public final StringPath maHoaDon = createString("maHoaDon");

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    public final NumberPath<Long> ngayDuKienNhan = createNumber("ngayDuKienNhan", Long.class);

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final QNhanVien nhanVien;

    public final QPhieuGiamGia phieuGiamGia;

    public final QPhieuGiamGiaCaNhan phieuGiamGiaCaNhan;

    public final NumberPath<java.math.BigDecimal> phiVanChuyen = createNumber("phiVanChuyen", java.math.BigDecimal.class);

    public final StringPath soDienThoaiNguoiNhan = createString("soDienThoaiNguoiNhan");

    public final NumberPath<java.math.BigDecimal> tienNguoiMua = createNumber("tienNguoiMua", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> tongTien = createNumber("tongTien", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> tongTienSauGiam = createNumber("tongTienSauGiam", java.math.BigDecimal.class);

    public final EnumPath<com.example.be.infrastructure.constants.OrderStatus> trangThai = createEnum("trangThai", com.example.be.infrastructure.constants.OrderStatus.class);

    public QHoaDon(String variable) {
        this(HoaDon.class, forVariable(variable), INITS);
    }

    public QHoaDon(Path<? extends HoaDon> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QHoaDon(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QHoaDon(PathMetadata metadata, PathInits inits) {
        this(HoaDon.class, metadata, inits);
    }

    public QHoaDon(Class<? extends HoaDon> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.khachHang = inits.isInitialized("khachHang") ? new QKhachHang(forProperty("khachHang"), inits.get("khachHang")) : null;
        this.nhanVien = inits.isInitialized("nhanVien") ? new QNhanVien(forProperty("nhanVien"), inits.get("nhanVien")) : null;
        this.phieuGiamGia = inits.isInitialized("phieuGiamGia") ? new QPhieuGiamGia(forProperty("phieuGiamGia")) : null;
        this.phieuGiamGiaCaNhan = inits.isInitialized("phieuGiamGiaCaNhan") ? new QPhieuGiamGiaCaNhan(forProperty("phieuGiamGiaCaNhan"), inits.get("phieuGiamGiaCaNhan")) : null;
    }

}

