package com.loggingsystem.springjwtauth.ticket.dto;

import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class TicketRequestDTO {
    private Long assigned_user_id;
    private Long status_id;
    private String description;
    private String owner;
    private Long category_id;
    private Long priority_id;
    private List<CommentDTO> comments;

    public TicketRequestDTO(Long assigned_user_id, Long status_id, String description, String owner, Long category_id, Long priority_id) {
        this.assigned_user_id = assigned_user_id;
        this.status_id = status_id;
        this.description = description;
        this.owner = owner;
        this.category_id = category_id;
        this.priority_id = priority_id;
    }

}
