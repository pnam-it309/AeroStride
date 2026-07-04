package com.example.be.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@Component
public class GhnClient {

    @Value("${ghn.api.url}")
    private String apiUrl;

    @Value("${ghn.api.token}")
    private String apiToken;

    @Value("${ghn.api.shopId}")
    private String shopId;

    private final RestTemplate restTemplate = new RestTemplate();

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
        if (insuranceValue != null) {
            body.put("insurance_value", insuranceValue);
        }

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
}
