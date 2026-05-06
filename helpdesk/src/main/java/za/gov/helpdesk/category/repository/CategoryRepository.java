package za.gov.helpdesk.category.repository;

import za.gov.helpdesk.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CategoryRepository extends JpaRepository<Category, Long>, PagingAndSortingRepository<Category, Long> {

    boolean existsByName(String name);

}
