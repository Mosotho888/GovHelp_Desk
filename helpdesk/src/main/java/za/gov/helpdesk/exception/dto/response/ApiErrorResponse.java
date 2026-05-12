package za.gov.helpdesk.exception.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApiErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    @Builder.Default
    private List<FieldError> details = List.of();

    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String issue;
    }
}
