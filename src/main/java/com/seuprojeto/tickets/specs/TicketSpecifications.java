package com.seuprojeto.tickets.specs;

import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.enums.TicketStatus;
import com.seuprojeto.tickets.enums.TicketPriority;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Set;

public class TicketSpecifications {

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, builder) ->
                status == null ? null : builder.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, builder) ->
                priority == null ? null : builder.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> createdBy(Long userId) {
        return (root, query, builder) ->
                userId == null ? null : builder.equal(root.get("createdBy").get("id"), userId);
    }

    public static Specification<Ticket> assignedTo(Long agentId) {
        return (root, query, builder) ->
                agentId == null ? null : builder.equal(root.get("assignedTo").get("id"), agentId);
    }

    public static Specification<Ticket> department(Long departmentId) {
        return (root, query, builder) ->
                departmentId == null ? null : builder.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Ticket> departmentIn(Set<Long> departmentIds) {
        return (root, query, builder) ->
                (departmentIds == null || departmentIds.isEmpty()) ? null : root.get("department").get("id").in(departmentIds);
    }

    public static Specification<Ticket> createdAfter(LocalDateTime date) {
        return (root, query, builder) ->
                date == null ? null : builder.greaterThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Ticket> createdBefore(LocalDateTime date) {
        return (root, query, builder) ->
                date == null ? null : builder.lessThanOrEqualTo(root.get("createdAt"), date);
    }
}
