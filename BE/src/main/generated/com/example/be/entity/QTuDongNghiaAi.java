package com.example.be.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTuDongNghiaAi is a Querydsl query type for TuDongNghiaAi
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTuDongNghiaAi extends EntityPathBase<TuDongNghiaAi> {

    private static final long serialVersionUID = -537082567L;

    public static final QTuDongNghiaAi tuDongNghiaAi = new QTuDongNghiaAi("tuDongNghiaAi");

    public final StringPath id = createString("id");

    public final StringPath tuChuanHoa = createString("tuChuanHoa");

    public final StringPath tuGoc = createString("tuGoc");

    public QTuDongNghiaAi(String variable) {
        super(TuDongNghiaAi.class, forVariable(variable));
    }

    public QTuDongNghiaAi(Path<? extends TuDongNghiaAi> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTuDongNghiaAi(PathMetadata metadata) {
        super(TuDongNghiaAi.class, metadata);
    }

}

