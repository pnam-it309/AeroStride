package com.example.be.core.customer.tintuc.model.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTinTucResponse {
    private String id;
    private String title;
    private String category;
    private String image;
    private String date;
    private String author;
    private Integer likes;
    private String excerpt;
    private String content;
    private List<BinhLuanResponse> comments;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BinhLuanResponse {
        private String name;
        private String text;
        private String date;
    }
}
