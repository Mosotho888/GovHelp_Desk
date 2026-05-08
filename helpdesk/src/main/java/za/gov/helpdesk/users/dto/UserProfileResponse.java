package za.gov.helpdesk.users.dto;

import za.gov.helpdesk.ticket.dto.AssignedTicketsDTO;
import za.gov.helpdesk.ticket.dto.SubmittedTicketsDTO;
import lombok.Data;
import za.gov.helpdesk.users.model.User;

import java.util.Collections;
import java.util.List;

@Data
public class UserProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private User.Role role;
    private List<AssignedTicketsDTO> assignedTickets;
    private List<SubmittedTicketsDTO> submittedTickets;

    public UserProfileResponse(User user){
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
    }

    public void setAssignedTicketsBasedOnRole(String role, List<AssignedTicketsDTO> tickets) {
        if (role.equals("ADMIN") || role.equals("TECHNICIAN")) {
            this.assignedTickets = tickets;
        } else {
            this.submittedTickets = Collections.emptyList(); // Empty for normal users
        }
    }

    public void setSubmittedTicketsBasedOnRole(String role, List<SubmittedTicketsDTO> tickets) {
        if (role.equals("USER")) {
            this.submittedTickets = tickets;
        } else {
            this.assignedTickets = Collections.emptyList(); // Empty for normal users
        }
    }
}
