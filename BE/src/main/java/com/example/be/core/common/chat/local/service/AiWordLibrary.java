package com.example.be.core.common.chat.local.service;

import com.example.be.entity.TuDongNghiaAi;
import com.example.be.repository.TuDongNghiaAiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AiWordLibrary {
    private final TuDongNghiaAiRepository aiSynonymRepository;
    
    // In-memory cache for synonyms to avoid DB queries per word
    private final Map<String, String> synonymCache = new ConcurrentHashMap<>();
    private volatile long lastCacheLoad = 0;
    private static final long CACHE_TTL = 30000; // 30 seconds

    private void ensureCacheLoaded() {
        long now = System.currentTimeMillis();
        if (synonymCache.isEmpty() || (now - lastCacheLoad > CACHE_TTL)) {
            synchronized (synonymCache) {
                if (synonymCache.isEmpty() || (now - lastCacheLoad > CACHE_TTL)) {
                    synonymCache.clear();
                    aiSynonymRepository.findAll().forEach(syn -> {
                        if (syn.getTuGoc() != null && syn.getTuChuanHoa() != null) {
                            synonymCache.put(syn.getTuGoc().toLowerCase().trim(), syn.getTuChuanHoa());
                        }
                    });
                    lastCacheLoad = now;
                }
            }
        }
    }

    public String normalize(String text) {
        if (text == null) return "";
        
        // 1. Chuẩn hóa sửa lỗi gõ Telex và từ không dấu bằng VietnameseLexicon
        String preNormalized = VietnameseLexicon.normalizeQuery(text);
        
        // 2. Loại bỏ các ký tự đặc biệt cơ bản
        String cleaned = preNormalized
                .replaceAll("[.,!?]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        
        if (cleaned.isEmpty()) return "";
        
        ensureCacheLoaded();
        
        // Word-by-word synonym replacement using high-performance cache map & optimized for loop
        String[] words = cleaned.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String canonical = synonymCache.get(word);
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(canonical != null ? canonical : word);
        }
        return sb.toString();
    }
}
