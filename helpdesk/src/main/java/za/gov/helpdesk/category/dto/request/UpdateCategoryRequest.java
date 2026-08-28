package za.gov.helpdesk.category.dto.request;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /** Null for a top level category, otherwise the id of the parent to nest under. */
    private Long parentId;

    @Size(max = 100, message = "Default department must not exceed 100 characters")
    private String defaultDepartment;

    private Boolean active;
}
