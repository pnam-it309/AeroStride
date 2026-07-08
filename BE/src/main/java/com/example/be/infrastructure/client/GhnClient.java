package com.example.be.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class GhnClient {

    @Value("${ghn.api.url}")
    private String apiUrl;

    @Value("${ghn.api.token}")
    private String apiToken;

    @Value("${ghn.api.shopId}")
    private String shopId;

    @Value("${ghn.api.shopAddress:}")
    private String shopAddress;

    @Value("${ghn.api.fromDistrictName:}")
    private String fromDistrictName;

    @Value("${ghn.api.fromWardName:}")
    private String fromWardName;

    @Value("${ghn.api.fromProvinceName:}")
    private String fromProvinceName;

    @Value("${ghn.api.fromDistrictId:}")
    private String fromDistrictId;

    @Value("${ghn.api.fromWardCode:}")
    private String fromWardCode;

    @Value("${ghn.api.packageLength:20}")
    private Integer packageLength;

    @Value("${ghn.api.packageWidth:15}")
    private Integer packageWidth;

    @Value("${ghn.api.packageHeight:10}")
    private Integer packageHeight;

    private final RestTemplate restTemplate;
    private Integer resolvedFromDistrictId;
    private String resolvedFromWardCode;

    public GhnClient() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    public Map<String, Object> calculateShippingFee(Integer toDistrictId, String toWardCode, Integer weight, Integer insuranceValue) {
        String url = apiUrl + "/v2/shipping-order/fee";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", apiToken);
        headers.set("ShopId", shopId);
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("service_type_id", 2); // 2: E-commerce delivery
        body.put("to_district_id", toDistrictId);
        body.put("to_ward_code", toWardCode);
        body.put("weight", weight != null ? weight : 200); // Default 200g
        body.put("length", packageLength != null ? packageLength : 20);
        body.put("width", packageWidth != null ? packageWidth : 15);
        body.put("height", packageHeight != null ? packageHeight : 10);
        body.put("insurance_value", insuranceValue != null ? insuranceValue : 0);
        // Kho gửi mặc định là shop ở Trịnh Văn Bô; ưu tiên mã env, nếu thiếu thì resolve từ tên Hà Nội/Nam Từ Liêm/Xuân Phương.
        applySenderLocation(body);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Integer code = (Integer) responseBody.get("code");
                if (code != null && code == 200) {
                    return (Map<String, Object>) responseBody.get("data");
                } else {
                    log.error("GHN API Error: {}", responseBody.get("message"));
                }
            }
        } catch (Exception e) {
            log.error("Failed to calculate GHN shipping fee: {}", e.getMessage());
        }
        return null;
    }

    public Map<String, Object> getProvinces() {
        String url = apiUrl + "/master-data/province";
        return executeGet(url);
    }

    public Map<String, Object> getDistricts(Integer provinceId) {
        String url = apiUrl + "/master-data/district?province_id=" + provinceId;
        return executeGet(url);
    }

    public Map<String, Object> getWards(Integer districtId) {
        String url = apiUrl + "/master-data/ward?district_id=" + districtId;
        return executeGet(url);
    }

    private Map<String, Object> executeGet(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", apiToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer code = (Integer) response.getBody().get("code");
                if (code != null && code == 200) {
                    return response.getBody();
                } else {
                    log.error("GHN API Error: {}", response.getBody().get("message"));
                }
            }
        } catch (Exception e) {
            log.error("Failed to call GHN API {}: {}", url, e.getMessage());
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void applySenderLocation(Map<String, Object> body) {
        putOptionalInteger(body, "from_district_id", fromDistrictId);
        if (hasText(fromWardCode)) {
            body.put("from_ward_code", fromWardCode.trim());
        }
        if (body.containsKey("from_district_id") && body.containsKey("from_ward_code")) {
            return;
        }

        resolveSenderLocationFromNames();
        if (!body.containsKey("from_district_id") && resolvedFromDistrictId != null) {
            body.put("from_district_id", resolvedFromDistrictId);
        }
        if (!body.containsKey("from_ward_code") && hasText(resolvedFromWardCode)) {
            body.put("from_ward_code", resolvedFromWardCode);
        }
        if (!body.containsKey("from_district_id") || !body.containsKey("from_ward_code")) {
            log.warn("GHN sender code is missing for shop address [{}], province [{}], district [{}], ward [{}]. GHN will fallback to ShopId store address.",
                    shopAddress, fromProvinceName, fromDistrictName, fromWardName);
        }
    }

    @SuppressWarnings("unchecked")
    private void resolveSenderLocationFromNames() {
        if (resolvedFromDistrictId != null && hasText(resolvedFromWardCode)) {
            return;
        }
        if (!hasText(fromProvinceName) || !hasText(fromDistrictName) || !hasText(fromWardName)) {
            return;
        }

        Map<String, Object> provinces = getProvinces();
        Map<String, Object> province = findLocationByName(provinces, "ProvinceName", fromProvinceName);
        Object provinceId = province != null ? province.get("ProvinceID") : null;
        if (!(provinceId instanceof Number)) {
            return;
        }

        Map<String, Object> districts = getDistricts(((Number) provinceId).intValue());
        Map<String, Object> district = findLocationByName(districts, "DistrictName", fromDistrictName);
        Object districtId = district != null ? district.get("DistrictID") : null;
        if (!(districtId instanceof Number)) {
            return;
        }
        resolvedFromDistrictId = ((Number) districtId).intValue();

        Map<String, Object> wards = getWards(resolvedFromDistrictId);
        Map<String, Object> ward = findLocationByName(wards, "WardName", fromWardName);
        Object wardCode = ward != null ? ward.get("WardCode") : null;
        if (wardCode != null) {
            resolvedFromWardCode = String.valueOf(wardCode);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findLocationByName(Map<String, Object> response, String fieldName, String expectedName) {
        Object data = response != null ? response.get("data") : null;
        if (!(data instanceof List<?> locations)) {
            return null;
        }
        String expected = normalizeLocationName(expectedName);
        return locations.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .filter(item -> {
                    String actual = normalizeLocationName(Objects.toString(item.get(fieldName), ""));
                    return actual.equals(expected) || actual.contains(expected) || expected.contains(actual);
                })
                .findFirst()
                .orElse(null);
    }

    private String normalizeLocationName(String value) {
        String withoutAccent = Normalizer.normalize(Objects.toString(value, ""), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccent.toLowerCase()
                .replaceAll("\\b(tinh|thanh pho|quan|huyen|thi xa|phuong|xa|thi tran)\\b", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private void putOptionalInteger(Map<String, Object> body, String key, String value) {
        if (!hasText(value)) {
            return;
        }
        try {
            body.put(key, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            log.warn("Invalid GHN integer config {}={}", key, value);
        }
    }
}
