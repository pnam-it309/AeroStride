package com.example.be.core.admin.thuoctinh.service.impl;

import com.example.be.core.admin.thuoctinh.model.request.AdminAttributeRequest;
import com.example.be.core.admin.thuoctinh.model.response.AdminAttributeResponse;
import com.example.be.core.admin.thuoctinh.repository.AdminAttributeCrudRepository;
import com.example.be.core.admin.thuoctinh.service.AdminAttributeManagementService;
import com.example.be.core.common.base.BaseCodeNameEntity;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.utils.CodeUtils;
import com.example.be.utils.SearchUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.CacheEvict;
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
    private final Function<AdminAttributeRequest, String> extraValueExtractor;
    private final BiConsumer<E, Boolean> deletedSetter;
    private final String entityDisplayName;

    protected AdminAttributeCrudSupport(
            AdminAttributeCrudRepository<E> repository,
            Supplier<E> entitySupplier,
            Function<E, String> extraValueGetter,
            BiConsumer<E, String> extraValueSetter,
            Function<AdminAttributeRequest, String> extraValueExtractor,
            BiConsumer<E, Boolean> deletedSetter,
            String entityDisplayName
    ) {
        this.repository = repository;
        this.entitySupplier = entitySupplier;
        this.extraValueGetter = extraValueGetter;
        this.extraValueSetter = extraValueSetter;
        this.extraValueExtractor = extraValueExtractor;
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
                        String cleanKeyword = keyword.trim();
                        if (cleanKeyword.length() > 100) {
                            cleanKeyword = cleanKeyword.substring(0, 100);
                        }
                        String pattern = "%" + cleanKeyword.toLowerCase() + "%";
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
    @CacheEvict(value = "productOptions", allEntries = true)
    public AdminAttributeResponse create(AdminAttributeRequest request) {
        String newTen = request.getTen() != null ? request.getTen().trim() : "";
        if (StringUtils.hasText(newTen)) {
            Optional<E> existingByName = repository.findOne((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("ten")), newTen.toLowerCase())
            );

            if (existingByName.isPresent()) {
                E existing = existingByName.get();
                if (existing.getTrangThai() != TrangThai.DANG_HOAT_DONG || Boolean.TRUE.equals(existing.getXoaMem())) {
                    existing.setTrangThai(TrangThai.DANG_HOAT_DONG);
                    deletedSetter.accept(existing, false);
                    if (StringUtils.hasText(request.getMoTa())) {
                        existing.setMoTa(normalize(request.getMoTa()));
                    }
                    extraValueSetter.accept(existing, normalize(extraValueExtractor.apply(request)));
                    return toResponse(repository.save(existing));
                } else {
                    return toResponse(existing);
                }
            }
        }

        validateDuplicate(request, null);

        E entity = entitySupplier.get();
        applyData(entity, request);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(value = "productOptions", allEntries = true)
    public AdminAttributeResponse update(String id, AdminAttributeRequest request) {
        E entity = findActiveEntity(id);
        validateDuplicate(request, entity);

        applyData(entity, request);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(value = "productOptions", allEntries = true)
    public void delete(String id) {
        E entity = findActiveEntity(id);
        entity.setTrangThai(TrangThai.DA_XOA);
        deletedSetter.accept(entity, true);
        repository.save(entity);
    }

    private void applyData(E entity, AdminAttributeRequest request) {
        String ma = normalize(request.getMa());
        if (!StringUtils.hasText(ma)) {
            ma = CodeUtils.generateRandom(entity.getClass());
        }
        entity.setMa(ma);
        entity.setTen(requireText(request.getTen(), "Ten " + entityDisplayName + " khong duoc de trong"));
        entity.setTrangThai(Optional.ofNullable(parseTrangThai(request.getTrangThai())).orElse(TrangThai.DANG_HOAT_DONG));
        
        String moTa = normalize(request.getMoTa());
        if (StringUtils.hasText(moTa) && moTa.length() > 255) {
            throw new BusinessException("Mo ta " + entityDisplayName + " khong duoc vuot qua 255 ky tu");
        }
        entity.setMoTa(moTa);
        extraValueSetter.accept(entity, normalize(extraValueExtractor.apply(request)));
        deletedSetter.accept(entity, false);
    }

    private void validateDuplicate(AdminAttributeRequest request, E existingEntity) {
        String newMa = normalize(request.getMa());
        if (StringUtils.hasText(newMa)) {
            boolean maChanged = existingEntity == null || !newMa.equalsIgnoreCase(existingEntity.getMa());
            if (maChanged && repository.exists((root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("xoaMem")),
                        criteriaBuilder.isFalse(root.get("xoaMem"))
                ));
                predicates.add(criteriaBuilder.notEqual(root.get("trangThai"), TrangThai.DA_XOA));
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("ma")), newMa.toLowerCase()));
                if (existingEntity != null) {
                    predicates.add(criteriaBuilder.notEqual(root.get("id"), existingEntity.getId()));
                }
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            })) {
                throw new BusinessException("Ma " + entityDisplayName + " da ton tai");
            }
        }

        String newTen = request.getTen() != null ? request.getTen().trim() : "";
        boolean tenChanged = existingEntity == null || !newTen.equalsIgnoreCase(existingEntity.getTen() != null ? existingEntity.getTen().trim() : "");
        if (tenChanged && repository.exists((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("xoaMem")),
                    criteriaBuilder.isFalse(root.get("xoaMem"))
                ));
            predicates.add(criteriaBuilder.notEqual(root.get("trangThai"), TrangThai.DA_XOA));
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("ten")), newTen.toLowerCase()));
            if (existingEntity != null) {
                predicates.add(criteriaBuilder.notEqual(root.get("id"), existingEntity.getId()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        })) {
            throw new BusinessException("Ten " + entityDisplayName + " da ton tai");
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
        )).orElseGet(() -> repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay " + entityDisplayName)));
    }

    private TrangThai parseTrangThai(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return TrangThai.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Trang thai khong hop le");
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
            throw new BusinessException(message);
        }
        return normalized;
    }

    private AdminAttributeResponse toResponse(E entity) {
        String extraValue = extraValueGetter.apply(entity);
        AdminAttributeResponse.AdminAttributeResponseBuilder builder = AdminAttributeResponse.builder()
                .id(entity.getId())
                .ma(entity.getMa())
                .ten(entity.getTen())
                .moTa(entity.getMoTa())
                .trangThai(entity.getTrangThai() != null ? entity.getTrangThai().name() : null)
                .ngayTao(entity.getNgayTao())
                .ngayCapNhat(entity.getNgayCapNhat());

        if (entity instanceof com.example.be.entity.MauSac) {
            builder.maMauHex(extraValue);
        } else if (entity instanceof com.example.be.entity.KichThuoc) {
            builder.giaTriKichThuoc(extraValue);
        }
        return builder.build();
    }
}
