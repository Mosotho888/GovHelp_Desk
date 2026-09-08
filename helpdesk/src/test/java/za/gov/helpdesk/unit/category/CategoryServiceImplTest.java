package za.gov.helpdesk.unit.category;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.category.dto.request.CreateCategoryRequest;
import za.gov.helpdesk.category.dto.request.UpdateCategoryRequest;
import za.gov.helpdesk.category.dto.response.CategoryResponse;
import za.gov.helpdesk.category.exception.InvalidCategoryOperationException;
import za.gov.helpdesk.category.mapper.CategoryMapper;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.repository.CategoryRepository;
import za.gov.helpdesk.category.service.CategoryQueryHelper;
import za.gov.helpdesk.category.service.impl.CategoryServiceImpl;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl unit tests")
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryQueryHelper categoryQuery;
    @Mock private CategoryMapper categoryMapper;
    @Captor private ArgumentCaptor<Category> categoryCaptor;

    private CategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoryRepository, categoryQuery, categoryMapper);
        // toFlatResponse is invoked on nearly every path; return a fresh response reflecting the id
        // passed in so tree-building/child assignment can be observed without over-specifying
        // stubs.
        // lenient(): several tests below throw before ever reaching the mapper call.
        lenient()
                .when(categoryMapper.toFlatResponse(any(Category.class)))
                .thenAnswer(
                        invocation -> {
                            final Category c = invocation.getArgument(0);
                            return CategoryResponse.builder()
                                    .id(c.getId())
                                    .name(c.getName())
                                    .build();
                        });
    }

    // ---- getCategoryTree ----

    @Test
    @DisplayName("getCategoryTree() nests children under their parent recursively")
    void getCategoryTree_nestedCategories_buildsFullTree() {
        final Category root = Category.builder().id(1L).name("Hardware").active(true).build();
        final Category child = Category.builder().id(2L).name("Laptops").active(true).build();
        final Category grandchild = Category.builder().id(3L).name("Screens").active(true).build();

        given(categoryRepository.findByParentIsNullOrderByNameAsc()).willReturn(List.of(root));
        given(categoryQuery.allGroupedByParentId())
                .willReturn(Map.of(1L, List.of(child), 2L, List.of(grandchild)));

        final List<CategoryResponse> tree = service.getCategoryTree(false);

        assertThat(tree).hasSize(1);
        assertThat(tree.getFirst().getId()).isEqualTo(1L);
        assertThat(tree.getFirst().getChildren()).hasSize(1);
        assertThat(tree.getFirst().getChildren().getFirst().getId()).isEqualTo(2L);
        assertThat(tree.getFirst().getChildren().getFirst().getChildren()).hasSize(1);
        assertThat(tree.getFirst().getChildren().getFirst().getChildren().getFirst().getId())
                .isEqualTo(3L);
    }

    @Test
    @DisplayName("getCategoryTree(activeOnly=true) excludes inactive roots and inactive children")
    void getCategoryTree_activeOnly_excludesInactiveNodes() {
        final Category activeRoot = Category.builder().id(1L).name("Hardware").active(true).build();
        final Category inactiveRoot =
                Category.builder().id(2L).name("Deprecated").active(false).build();
        final Category activeChild = Category.builder().id(3L).name("Laptops").active(true).build();
        final Category inactiveChild =
                Category.builder().id(4L).name("OldStock").active(false).build();

        given(categoryRepository.findByParentIsNullOrderByNameAsc())
                .willReturn(List.of(activeRoot, inactiveRoot));
        given(categoryQuery.allGroupedByParentId())
                .willReturn(Map.of(1L, List.of(activeChild, inactiveChild)));

        final List<CategoryResponse> tree = service.getCategoryTree(true);

        assertThat(tree).extracting(CategoryResponse::getId).containsExactly(1L);
        assertThat(tree.getFirst().getChildren())
                .extracting(CategoryResponse::getId)
                .containsExactly(3L);
    }

    // ---- getCategoryById ----

    @Test
    @DisplayName("getCategoryById() delegates lookup and mapping")
    void getCategoryById_existingId_returnsMappedResponse() {
        final Category category = Category.builder().id(5L).name("Networking").build();
        given(categoryQuery.findOrThrow(5L)).willReturn(category);

        final CategoryResponse response = service.getCategoryById(5L);

        assertThat(response.getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getCategoryById() propagates ResourceNotFoundException for an unknown id")
    void getCategoryById_unknownId_throws() {
        given(categoryQuery.findOrThrow(999L))
                .willThrow(new ResourceNotFoundException("Category", 999L));

        assertThatThrownBy(() -> service.getCategoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- createCategory ----

    @Test
    @DisplayName("createCategory() creates a top-level category with a slugified name")
    void createCategory_topLevel_createsWithGeneratedSlug() {
        final CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Network Issues");
        given(categoryRepository.existsByParentIsNullAndNameIgnoreCase("Network Issues"))
                .willReturn(false);
        given(categoryRepository.existsBySlug("network-issues")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        service.createCategory(request);

        then(categoryRepository).should(times(1)).save(categoryCaptor.capture());
        final Category saved = categoryCaptor.getValue();
        assertThat(saved.getSlug()).isEqualTo("network-issues");
        assertThat(saved.getLevel()).isZero();
        assertThat(saved.getParent()).isNull();
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName(
            "createCategory() nests under a parent and inherits its default department when unset")
    void createCategory_underParent_inheritsDepartmentWhenNotSpecified() {
        final Category parent =
                Category.builder()
                        .id(1L)
                        .name("Hardware")
                        .level((short) 0)
                        .defaultDepartment("IT Support")
                        .build();
        final CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Laptops");
        request.setParentId(1L);

        given(categoryQuery.findOrThrow(1L)).willReturn(parent);
        given(categoryRepository.existsByParentIdAndNameIgnoreCase(1L, "Laptops"))
                .willReturn(false);
        given(categoryRepository.existsBySlug("laptops")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        service.createCategory(request);

        then(categoryRepository).should(times(1)).save(categoryCaptor.capture());
        final Category saved = categoryCaptor.getValue();
        assertThat(saved.getLevel()).isEqualTo((short) 1);
        assertThat(saved.getParent()).isEqualTo(parent);
        assertThat(saved.getDefaultDepartment()).isEqualTo("IT Support");
    }

    @Test
    @DisplayName(
            "createCategory() uses the request's own department over the parent's when specified")
    void createCategory_underParent_prefersOwnDepartmentOverParent() {
        final Category parent =
                Category.builder()
                        .id(1L)
                        .name("Hardware")
                        .level((short) 0)
                        .defaultDepartment("IT Support")
                        .build();
        final CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Laptops");
        request.setParentId(1L);
        request.setDefaultDepartment("Field Services");

        given(categoryQuery.findOrThrow(1L)).willReturn(parent);
        given(categoryRepository.existsByParentIdAndNameIgnoreCase(1L, "Laptops"))
                .willReturn(false);
        given(categoryRepository.existsBySlug("laptops")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        service.createCategory(request);

        then(categoryRepository).should(times(1)).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getDefaultDepartment()).isEqualTo("Field Services");
    }

    @Test
    @DisplayName("createCategory() rejects nesting beyond the maximum category depth")
    void createCategory_exceedsMaxDepth_throws() {
        final Category tooDeepParent =
                Category.builder().id(9L).name("Screens").level((short) Category.MAX_LEVEL).build();
        final CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Cracked Screens");
        request.setParentId(9L);

        given(categoryQuery.findOrThrow(9L)).willReturn(tooDeepParent);

        assertThatThrownBy(() -> service.createCategory(request))
                .isInstanceOf(InvalidCategoryOperationException.class)
                .hasMessageContaining("maximum category depth");

        then(categoryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createCategory() rejects a duplicate name at the same tree level")
    void createCategory_duplicateNameAtLevel_throws() {
        final CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Hardware");
        given(categoryRepository.existsByParentIsNullAndNameIgnoreCase("Hardware"))
                .willReturn(true);

        assertThatThrownBy(() -> service.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class);

        then(categoryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createCategory() appends a numeric suffix when the generated slug already exists")
    void createCategory_slugCollision_appendsSuffix() {
        final CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Hardware");
        given(categoryRepository.existsByParentIsNullAndNameIgnoreCase("Hardware"))
                .willReturn(false);
        given(categoryRepository.existsBySlug("hardware")).willReturn(true);
        given(categoryRepository.existsBySlug("hardware-2")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        service.createCategory(request);

        then(categoryRepository).should(times(1)).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getSlug()).isEqualTo("hardware-2");
    }

    // ---- updateCategory ----

    @Test
    @DisplayName("updateCategory() renames and regenerates the slug when the name changes")
    void updateCategory_nameChanged_updatesNameAndSlug() {
        final Category category =
                Category.builder().id(1L).name("Old Name").slug("old-name").build();
        final UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("New Name");

        given(categoryQuery.findOrThrow(1L)).willReturn(category);
        given(categoryRepository.existsByParentIsNullAndNameIgnoreCase("New Name"))
                .willReturn(false);
        given(categoryRepository.existsBySlug("new-name")).willReturn(false);

        service.updateCategory(1L, request);

        assertThat(category.getName()).isEqualTo("New Name");
        assertThat(category.getSlug()).isEqualTo("new-name");
        then(categoryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("updateCategory() is a no-op on the name when it's unchanged (case-insensitive)")
    void updateCategory_sameNameDifferentCase_skipsUniquenessCheck() {
        final Category category =
                Category.builder().id(1L).name("Hardware").slug("hardware").build();
        final UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("HARDWARE");

        given(categoryQuery.findOrThrow(1L)).willReturn(category);

        service.updateCategory(1L, request);

        assertThat(category.getSlug()).isEqualTo("hardware");
        then(categoryRepository).should(never()).existsByParentIsNullAndNameIgnoreCase(anyString());
    }

    @Test
    @DisplayName("updateCategory() rejects renaming to a name already used at the same level")
    void updateCategory_renameToDuplicateName_throws() {
        final Category category =
                Category.builder().id(1L).name("Old Name").slug("old-name").build();
        final UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Taken Name");

        given(categoryQuery.findOrThrow(1L)).willReturn(category);
        given(categoryRepository.existsByParentIsNullAndNameIgnoreCase("Taken Name"))
                .willReturn(true);

        assertThatThrownBy(() -> service.updateCategory(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(category.getName()).isEqualTo("Old Name");
    }

    @Test
    @DisplayName("updateCategory() updates the default department when provided")
    void updateCategory_departmentProvided_updatesDepartment() {
        final Category category = Category.builder().id(1L).name("Hardware").build();
        final UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setDefaultDepartment("Field Services");

        given(categoryQuery.findOrThrow(1L)).willReturn(category);

        service.updateCategory(1L, request);

        assertThat(category.getDefaultDepartment()).isEqualTo("Field Services");
    }

    @Test
    @DisplayName("updateCategory() deactivates a leaf category when active=false is requested")
    void updateCategory_deactivateLeaf_succeeds() {
        final Category category = Category.builder().id(1L).name("Hardware").active(true).build();
        final UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setActive(false);

        given(categoryQuery.findOrThrow(1L)).willReturn(category);
        given(categoryRepository.existsByParentIdAndActiveTrue(1L)).willReturn(false);

        service.updateCategory(1L, request);

        assertThat(category.isActive()).isFalse();
    }

    @Test
    @DisplayName("updateCategory() refuses to deactivate a category with active subcategories")
    void updateCategory_deactivateWithActiveChildren_throws() {
        final Category category = Category.builder().id(1L).name("Hardware").active(true).build();
        final UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setActive(false);

        given(categoryQuery.findOrThrow(1L)).willReturn(category);
        given(categoryRepository.existsByParentIdAndActiveTrue(1L)).willReturn(true);

        assertThatThrownBy(() -> service.updateCategory(1L, request))
                .isInstanceOf(InvalidCategoryOperationException.class)
                .hasMessageContaining("active subcategories");
        assertThat(category.isActive()).isTrue();
    }

    // ---- deactivateCategory ----

    @Test
    @DisplayName("deactivateCategory() sets active=false on a leaf category")
    void deactivateCategory_leaf_setsInactive() {
        final Category category = Category.builder().id(1L).name("Hardware").active(true).build();
        given(categoryQuery.findOrThrow(1L)).willReturn(category);
        given(categoryRepository.existsByParentIdAndActiveTrue(1L)).willReturn(false);

        service.deactivateCategory(1L);

        assertThat(category.isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivateCategory() refuses to deactivate a category with active subcategories")
    void deactivateCategory_withActiveChildren_throws() {
        final Category category = Category.builder().id(1L).name("Hardware").active(true).build();
        given(categoryQuery.findOrThrow(1L)).willReturn(category);
        given(categoryRepository.existsByParentIdAndActiveTrue(1L)).willReturn(true);

        assertThatThrownBy(() -> service.deactivateCategory(1L))
                .isInstanceOf(InvalidCategoryOperationException.class);
        assertThat(category.isActive()).isTrue();
    }
}
