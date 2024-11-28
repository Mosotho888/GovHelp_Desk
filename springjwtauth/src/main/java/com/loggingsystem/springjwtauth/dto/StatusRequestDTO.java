package com.loggingsystem.springjwtauth.dto;

import com.loggingsystem.springjwtauth.model.Status;
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
