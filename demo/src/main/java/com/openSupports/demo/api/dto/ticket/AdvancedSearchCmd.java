package com.openSupports.demo.api.dto.ticket;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AdvancedSearchCmd {
    private String title;
    private String tags;
    private String ticketCode;
    private Boolean closed;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;
    private Long departmentId;
    private Long authorId;
    private Long ownerId;
    private Boolean assigned;
    private String query;
    private String orderBy;
    private Integer page = 1;
    private Integer pageSize = 10;
}
