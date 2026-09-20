package com.openSupports.demo.api.dto.department;

import lombok.Data;

@Data
public class DepartmentOverviewView {
    private Long id;
    private String name;
    private Boolean isDefault;
    private Boolean isPrivate;
    private int ticketCount;
    private int staffCount;

    public void setDefault(boolean value) { this.isDefault = value; }
    public void setPrivate(boolean value) { this.isPrivate = value; }
}
