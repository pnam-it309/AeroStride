package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLichSuHoatDong is a Querydsl query type for LichSuHoatDong
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLichSuHoatDong extends EntityPathBase<LichSuHoatDong> {

    private static final long serialVersionUID = 1129443283L;

    public static final QLichSuHoatDong lichSuHoatDong = new QLichSuHoatDong("lichSuHoatDong");

    public final com.example.be.core.common.base.QPrimaryEntity _super = new com.example.be.core.common.base.QPrimaryEntity(this);

    public final StringPath doiTuong = createString("doiTuong");

    public final StringPath hanhDong = createString("hanhDong");

    //inherited
    public final StringPath id = _super.id;

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

    public QLichSuHoatDong(String variable) {
        super(LichSuHoatDong.class, forVariable(variable));
    }

    public QLichSuHoatDong(Path<? extends LichSuHoatDong> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLichSuHoatDong(PathMetadata metadata) {
        super(LichSuHoatDong.class, metadata);
    }

}

