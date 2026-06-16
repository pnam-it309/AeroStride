package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QKienThucAi is a Querydsl query type for KienThucAi
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QKienThucAi extends EntityPathBase<KienThucAi> {

    private static final long serialVersionUID = -1808073054L;

    public static final QKienThucAi kienThucAi = new QKienThucAi("kienThucAi");

    public final NumberPath<Integer> doUuTien = createNumber("doUuTien", Integer.class);

    public final StringPath id = createString("id");

    public final StringPath mauCauTraLoi = createString("mauCauTraLoi");

    public final StringPath mucDich = createString("mucDich");

    public final StringPath tuKhoa = createString("tuKhoa");

    public QKienThucAi(String variable) {
        super(KienThucAi.class, forVariable(variable));
    }

    public QKienThucAi(Path<? extends KienThucAi> path) {
        super(path.getType(), path.getMetadata());
    }

    public QKienThucAi(PathMetadata metadata) {
        super(KienThucAi.class, metadata);
    }

}

