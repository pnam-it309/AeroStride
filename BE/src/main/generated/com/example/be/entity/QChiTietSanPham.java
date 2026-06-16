package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChiTietSanPham is a Querydsl query type for ChiTietSanPham
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChiTietSanPham extends EntityPathBase<ChiTietSanPham> {

    private static final long serialVersionUID = -1148351667L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChiTietSanPham chiTietSanPham = new QChiTietSanPham("chiTietSanPham");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final SetPath<AnhChiTietSanPham, QAnhChiTietSanPham> anhChiTietSanPhams = this.<AnhChiTietSanPham, QAnhChiTietSanPham>createSet("anhChiTietSanPhams", AnhChiTietSanPham.class, QAnhChiTietSanPham.class, PathInits.DIRECT2);

    public final SetPath<ChiTietDotGiamGia, QChiTietDotGiamGia> chiTietDotGiamGias = this.<ChiTietDotGiamGia, QChiTietDotGiamGia>createSet("chiTietDotGiamGias", ChiTietDotGiamGia.class, QChiTietDotGiamGia.class, PathInits.DIRECT2);

    public final NumberPath<java.math.BigDecimal> giaBan = createNumber("giaBan", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> giaNhap = createNumber("giaNhap", java.math.BigDecimal.class);

    public final SetPath<HoaDonChiTiet, QHoaDonChiTiet> hoaDonChiTiets = this.<HoaDonChiTiet, QHoaDonChiTiet>createSet("hoaDonChiTiets", HoaDonChiTiet.class, QHoaDonChiTiet.class, PathInits.DIRECT2);

    //inherited
    public final StringPath id = _super.id;

    public final QKichThuoc kichThuoc;

    public final StringPath maChiTietSanPham = createString("maChiTietSanPham");

    public final QMauSac mauSac;

    //inherited
    public final NumberPath<Long> ngayCapNhat = _super.ngayCapNhat;

    //inherited
    public final NumberPath<Long> ngayTao = _super.ngayTao;

    //inherited
    public final StringPath nguoiCapNhat = _super.nguoiCapNhat;

    //inherited
    public final StringPath nguoiTao = _super.nguoiTao;

    public final QSanPham sanPham;

    public final NumberPath<Integer> soLuong = createNumber("soLuong", Integer.class);

    //inherited
    public final EnumPath<com.example.be.infrastructure.constants.TrangThai> trangThai = _super.trangThai;

    public final BooleanPath xoaMem = createBoolean("xoaMem");

    public QChiTietSanPham(String variable) {
        this(ChiTietSanPham.class, forVariable(variable), INITS);
    }

    public QChiTietSanPham(Path<? extends ChiTietSanPham> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChiTietSanPham(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChiTietSanPham(PathMetadata metadata, PathInits inits) {
        this(ChiTietSanPham.class, metadata, inits);
    }

    public QChiTietSanPham(Class<? extends ChiTietSanPham> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.kichThuoc = inits.isInitialized("kichThuoc") ? new QKichThuoc(forProperty("kichThuoc")) : null;
        this.mauSac = inits.isInitialized("mauSac") ? new QMauSac(forProperty("mauSac")) : null;
        this.sanPham = inits.isInitialized("sanPham") ? new QSanPham(forProperty("sanPham"), inits.get("sanPham")) : null;
    }

}

