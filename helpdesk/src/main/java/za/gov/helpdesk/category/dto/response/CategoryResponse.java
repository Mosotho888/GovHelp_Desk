package za.gov.helpdesk.category.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private short level;
    private String defaultDepartment;
    private boolean active;

    @Builder.Default private List<CategoryResponse> children = List.of();
}
