package za.gov.helpdesk.category.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.category.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByParentIdAndActiveTrue(Long categoryId);

    List<Category> findByParentIsNullOrderByNameAsc();

    List<Category> findByParentIdOrderByNameAsc(Long parentId);

    /**
     * Used for sibling-uniqueness validation on create/rename. Spring Data translates a {@code
     * null} parentId into an {@code IS NULL} comparison, so this also correctly checks for
     * duplicate top level category names.
     */
    boolean existsByParentIdAndNameIgnoreCase(Long parentId, String name);

    boolean existsByParentIsNullAndNameIgnoreCase(String name);
}
