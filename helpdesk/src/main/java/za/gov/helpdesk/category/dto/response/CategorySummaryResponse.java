package za.gov.helpdesk.category.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Flat, ticket-facing view of a category: just enough to render and filter on, without the full
 * tree. {@link #path} is the human-readable breadcrumb, e.g. "Hardware > Laptop".
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySummaryResponse {

    private Long id;
    private String name;
    private String path;
}
