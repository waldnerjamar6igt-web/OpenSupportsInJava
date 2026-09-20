package com.openSupports.demo.api.dto.staff;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class SearchStaffCmd {
    private String emailKeyword;
    private Long departmentId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registerDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registerDateTo;
    private Integer level;
    private String sortBy = "sign_up_time";
    private Integer page = 1;
    private Integer pageSize = 10;
}
