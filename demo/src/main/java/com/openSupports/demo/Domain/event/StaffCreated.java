package com.openSupports.demo.Domain.event;

public class StaffCreated extends Event {
    public final Integer level;
    public final Long departmentId;
    public StaffCreated(Integer level, Long departmentId) {
        this.level = level;
        this.departmentId = departmentId;
    }
}
