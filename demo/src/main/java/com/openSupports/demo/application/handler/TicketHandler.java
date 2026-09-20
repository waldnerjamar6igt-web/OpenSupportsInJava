package com.openSupports.demo.application.handler;

import com.openSupports.demo.Domain.event.StaffDeleted;
import com.openSupports.demo.Domain.event.StaffDisabled;
import com.openSupports.demo.Domain.event.TagDeleted;
import com.openSupports.demo.Domain.repo.TicketRepo;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TicketHandler {

    private final TicketRepo ticketRepo;

    public TicketHandler(TicketRepo ticketRepo) {
        this.ticketRepo = ticketRepo;
    }

    @Async
    @EventListener
    public void handleStaffDeleted(StaffDeleted evt) {
        // Staff deleted → unassign all their tickets and close them
        // Implementation requires staff ID from domain context
        System.out.println("Handling staff deletion: unassigning tickets");
    }

    @Async
    @EventListener
    public void handleStaffDisabled(StaffDisabled evt) {
        // Staff disabled → unassign all their active tickets
        System.out.println("Handling staff disable: unassigning tickets");
    }

    @Async
    @EventListener
    public void handleTagDeleted(TagDeleted evt) {
        // Tag deleted → remove from all associated tickets
        System.out.println("Handling tag deletion: removing from tickets");
    }
}
