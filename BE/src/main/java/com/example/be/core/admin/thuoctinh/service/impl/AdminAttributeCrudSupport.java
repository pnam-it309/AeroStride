package com.example.be.core.admin.thuoctinh.service.impl;

import com.example.be.core.admin.thuoctinh.model.request.AdminAttributeRequest;
import com.example.be.core.admin.thuoctinh.model.response.AdminAttributeResponse;
import com.example.be.core.admin.thuoctinh.repository.AdminAttributeCrudRepository;
import com.example.be.core.admin.thuoctinh.service.AdminAttributeManagementService;
import com.example.be.core.common.base.BaseCodeNameEntity;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.RestApiException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AdminAttributeCrudSupport<E extends BaseCodeNameEntity> implements AdminAttributeManagementService {

    private final AdminAttributeCrudRepository<E> repository;
    private final Supplier<E> entitySupplier;
    private final Function<E, String> extraValueGetter;
    private final BiConsumer<E, String> extraValueSetter;
    private final BiConsumer<E, Boolean> deletedSetter;
    private final String entityDisplayName;

    protected AdminAttributeCrudSupport(
            AdminAttributeCrudRepository<E> repository,
            Supplier<E> entitySupplier,
            Function<E, String> extraValueGetter,
            BiConsumer<E, String> extraValueSetter,
            BiConsumer<E, Boolean> deletedSetter,
            String entityDisplayName
    ) {
        this.repository = repository;
        this.entitySupplier = entitySupplier;
        this.extraValueGetter = extraValueGetter;
        this.extraValueSetter = extraValueSetter;
        this.deletedSetter = deletedSetter;
        this.entityDisplayName = entityDisplayName;
    }

    @Override
    public PageResponse<AdminAttributeResponse> search(String keyword, String trangThai, PageRequest pageRequest) {
        int page = Math.max(0, pageRequest.getPage());
        int size = pageRequest.getSize() > 0 ? pageRequest.getSize() : 10;
        String sortBy = StringUtils.hasText(pageRequest.getSortBy()) ? pageRequest.getSortBy() : "ngayTao";
        Sort.Direction direction = "asc".equalsIgnoreCase(pageRequest.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Page<E> result = repository.findAll(
                (root, query, criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.isNull(root.get("xoaMem")),
                            criteriaBuilder.isFalse(root.get("xoaMem"))
                    ));
                    predicates.add(criteriaBuilder.notEqual(root.get("trangThai"), TrangThai.DA_XOA));

                    TrangThai parsedTrangThai = parseTrangThai(trangThai);
                    if (parsedTrangThai != null) {
                        predicates.add(criteriaBuilder.equal(root.get("trangThai"), parsedTrangThai));
                    }

                    if (StringUtils.hasText(keyword)) {
                        String pattern = "%" + keyword.trim().toLowerCase() + "%";
                        predicates.add(criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("ma")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("ten")), pattern)
                        ));
                    }

                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                },
                org.springframework.data.domain.PageRequest.of(page, size, Sort.by(direction, sortBy))
        );

        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    public AdminAttributeResponse getById(String id) {
        return toResponse(findActiveEntity(id));
    }

    @Override
    @Transactional
    public AdminAttributeResponse create(AdminAttributeRequest request) {
        validateDuplicate(request, null);

        E entity = entitySupplier.get();
        applyData(entity, request);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public AdminAttributeResponse update(String id, AdminAttributeRequest request) {
        E entity = findActiveEntity(id);
        validateDuplicate(request, id);

        applyData(entity, request);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        E entity = findActiveEntity(id);
        entity.setTrangThai(TrangThai.DA_XOA);
        deletedSetter.accept(entity, true);
        repository.save(entity);
    }

    private void applyData(E entity, AdminAttributeRequest request) {
        entity.setMa(normalize(request.getMa()));
        entity.setTen(requireText(request.getTen(), "Ten " + entityDisplayName + " khong duoc de trong"));
        entity.setTrangThai(Optional.ofNullable(parseTrangThai(request.getTrangThai())).orElse(TrangThai.DANG_HOAT_DONG));
        extraValueSetter.accept(entity, normalize(request.getMoTa()));
        deletedSetter.accept(entity, false);
    }

    private void validateDuplicate(AdminAttributeRequest request, String excludeId) {
        if (StringUtils.hasText(request.getMa()) && repository.exists((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("xoaMem")),
                    criteriaBuilder.isFalse(root.get("xoaMem"))
            ));
            predicates.add(criteriaBuilder.notEqual(root.get("trangThai"), TrangThai.DA_XOA));
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("ma")), request.getMa().trim().toLowerCase()));
            if (StringUtils.hasText(excludeId)) {
                predicates.add(criteriaBuilder.notEqual(root.get("id"), excludeId));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        })) {
            throw new RestApiException("Ma " + entityDisplayName + " da ton tai");
        }

        if (repository.exists((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("xoaMem")),
                    criteriaBuilder.isFalse(root.get("xoaMem"))
            ));
            predicates.add(criteriaBuilder.notEqual(root.get("trangThai"), TrangThai.DA_XOA));
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("ten")), request.getTen().trim().toLowerCase()));
            if (StringUtils.hasText(excludeId)) {
                predicates.add(criteriaBuilder.notEqual(root.get("id"), excludeId));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        })) {
            throw new RestApiException("Ten " + entityDisplayName + " da ton tai");
        }
    }

    private E findActiveEntity(String id) {
        return repository.findOne((root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("id"), id),
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("xoaMem")),
                        criteriaBuilder.isFalse(root.get("xoaMem"))
                ),
                criteriaBuilder.notEqual(root.get("trangThai"), TrangThai.DA_XOA)
        )).orElseThrow(() -> new RestApiException("Khong tim thay " + entityDisplayName));
    }

    private TrangThai parseTrangThai(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return TrangThai.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new RestApiException("Trang thai khong hop le");
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String requireText(String value, String message) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw new RestApiException(message);
        }
        return normalized;
    }

    private AdminAttributeResponse toResponse(E entity) {
        return AdminAttributeResponse.builder()
                .id(entity.getId())
                .ma(entity.getMa())
                .ten(entity.getTen())
                .moTa(extraValueGetter.apply(entity))
                .trangThai(entity.getTrangThai() != null ? entity.getTrangThai().name() : null)
                .ngayTao(entity.getNgayTao())
                .ngayCapNhat(entity.getNgayCapNhat())
                .build();
    }
}
