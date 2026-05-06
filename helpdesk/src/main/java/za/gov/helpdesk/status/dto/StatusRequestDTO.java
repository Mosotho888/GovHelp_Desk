package za.gov.helpdesk.status.dto;

import za.gov.helpdesk.status.model.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusRequestDTO {
    private Long status_id;
    public StatusRequestDTO(Status status) {
        this.status_id = status.getId();
    }
}
