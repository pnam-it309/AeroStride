package com.example.be.core.admin.nhanvien.repository;

;
import com.example.be.core.admin.nhanvien.model.request.ResetPasswordRequest;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// ResetPasswordRequestRepository.java
@Repository
public class ResetPasswordRequestRepository {

    private final List<ResetPasswordRequest> requests = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(1); // ✅ Tự tăng id

    public ResetPasswordRequest save(ResetPasswordRequest req) {
        if (req.getId() == null) {
            req.setId(idCounter.getAndIncrement()); // ✅ Gán id mới
            requests.add(req);
        } else {
            // Update request đã tồn tại
            for (int i = 0; i < requests.size(); i++) {
                if (requests.get(i).getId().equals(req.getId())) {
                    requests.set(i, req);
                    break;
                }
            }
        }
        return req;
    }

    public List<ResetPasswordRequest> findByStatus(ResetPasswordRequest.Status status) {
        return requests.stream()
            .filter(r -> r.getStatus() == status)
            .collect(java.util.stream.Collectors.toList());
    }

    public Optional<ResetPasswordRequest> findById(Integer id) {
        return requests.stream()
            .filter(r -> Objects.equals(r.getId(), id))
            .findFirst();
    }

    // ✅ Chặn spam
    public boolean existsByNhanVienIdAndStatus(Integer nhanVienId, ResetPasswordRequest.Status status) {
        return requests.stream()
            .anyMatch(r -> r.getNhanVien().getId().equals(nhanVienId)
                && r.getStatus() == status);
    }
}
