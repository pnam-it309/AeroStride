package com.example.be.core.admin.lichlamviec.model.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutoScheduleRequest {
    private String startDate; // yyyy-MM-dd
    private String endDate;   // yyyy-MM-dd
}
