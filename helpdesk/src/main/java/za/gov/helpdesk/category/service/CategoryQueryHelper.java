package za.gov.helpdesk.category.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.category.dto.response.CategorySummaryResponse;
import za.gov.helpdesk.category.mapper.CategoryMapper;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.repository.CategoryRepository;
import za.gov.helpdesk.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Read-only helper for category lookups shared across the ticket and category services. The
 * category tree is small (tens of nodes for a municipal help desk, not thousands), so rather than
 * writing recursive SQL, it is loaded in full and walked in memory - simpler to read and just as
 * fast at this scale.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryHelper {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Finds a {@link Category} by its unique identifier or throws an exception if not found.
     *
     * @param categoryId the unique identifier of the category to retrieve
     * @return the found {@link Category} entity
     * @throws ResourceNotFoundException if no category exists with the specified ID
     */
    public Category findOrThrow(final Long categoryId) {
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    /**
     * Builds the "Grandparent > Parent > Category" breadcrumb for a category by walking the {@code
     * parent} chain.
     */
    public String buildPath(final Category category) {
        final Deque<String> segments = new ArrayDeque<>();
        Category current = category;
        while (current != null) {
            segments.addFirst(current.getName());
            current = current.getParent();
        }
        return String.join(" > ", segments);
    }

    /**
     * Maps a {@link Category} entity to a {@link CategorySummaryResponse} dto.
     *
     * <p>Evaluates the full hierarchical path for the given category before mapping. Returns {@code
     * null} if the provided category is {@code null}.
     *
     * @param category the {@link Category} entity to map, may be {@code null}
     * @return the mapped {@link CategorySummaryResponse}, or {@code null} if the input category is
     *     {@code null}
     */
    public CategorySummaryResponse toSummary(final Category category) {
        if (category == null) {
            return null;
        }
        return categoryMapper.toSummaryResponse(category, buildPath(category));
    }

    /**
     * Resolves a category id plus every id beneath it in the tree. Used so that filtering tickets
     * by a parent category (e.g. "Hardware") also returns tickets filed against its subcategories,
     * without callers having to know the tree shape.
     *
     * @param categoryId the root of the subtree to resolve, or null to skip category filtering
     *     entirely
     * @return null if categoryId is null (meaning "no filter"), otherwise the category id and all
     *     of its descendant ids
     */
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public Set<Long> resolveWithDescendants(final Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        // Throws ResourceNotFoundException if the requested category doesn't exist, rather than
        // silently returning zero tickets for a typo or stale categoryId.
        findOrThrow(categoryId);

        final Map<Long, List<Category>> childrenByParentId =
                categoryRepository.findAll().stream()
                        .filter(c -> c.getParent() != null)
                        .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        final Set<Long> result = new HashSet<>();
        final Deque<Long> toVisit = new ArrayDeque<>();
        toVisit.push(categoryId);

        while (!toVisit.isEmpty()) {
            final Long id = toVisit.pop();
            if (result.add(id)) {
                childrenByParentId
                        .getOrDefault(id, List.of())
                        .forEach(c -> toVisit.push(c.getId()));
            }
        }
        return result;
    }

    /**
     * Groups all categories by parent id in a single query, for building the full tree without N+1
     * lookups.
     */
    public Map<Long, List<Category>> allGroupedByParentId() {
        return categoryRepository.findAll().stream()
                .filter(c -> c.getParent() != null)
                .collect(
                        Collectors.groupingBy(
                                c -> c.getParent().getId(), HashMap::new, Collectors.toList()));
    }
}
