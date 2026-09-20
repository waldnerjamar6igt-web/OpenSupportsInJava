package com.openSupports.demo.Domain.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    private Long id;
    private String name;
    private Boolean isDefault;
    private Boolean isPrivate;
    private LocalDateTime createdAt;
    private Integer ticketCount;
    private Integer staffCount;
}
