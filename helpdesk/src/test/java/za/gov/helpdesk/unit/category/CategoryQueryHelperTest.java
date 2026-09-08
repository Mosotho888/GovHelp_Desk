package za.gov.helpdesk.unit.category;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.category.dto.response.CategorySummaryResponse;
import za.gov.helpdesk.category.mapper.CategoryMapper;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.repository.CategoryRepository;
import za.gov.helpdesk.category.service.CategoryQueryHelper;
import za.gov.helpdesk.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryQueryHelper unit tests")
class CategoryQueryHelperTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;

    private CategoryQueryHelper helper;

    private Category hardware;
    private Category laptops;
    private Category screens;
    private Category software;

    @BeforeEach
    void setUp() {
        helper = new CategoryQueryHelper(categoryRepository, categoryMapper);

        hardware = Category.builder().id(1L).name("Hardware").level((short) 0).build();
        laptops =
                Category.builder().id(2L).name("Laptops").parent(hardware).level((short) 1).build();
        screens =
                Category.builder().id(3L).name("Screens").parent(laptops).level((short) 2).build();
        software = Category.builder().id(4L).name("Software").level((short) 0).build();
    }

    @Test
    @DisplayName("findOrThrow() returns the category when found")
    void findOrThrow_existingId_returnsCategory() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(hardware));

        assertThat(helper.findOrThrow(1L)).isEqualTo(hardware);
    }

    @Test
    @DisplayName("findOrThrow() throws ResourceNotFoundException for an unknown id")
    void findOrThrow_unknownId_throws() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> helper.findOrThrow(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("buildPath() joins a single top-level category with no separator")
    void buildPath_topLevelCategory_returnsNameOnly() {
        assertThat(helper.buildPath(hardware)).isEqualTo("Hardware");
    }

    @Test
    @DisplayName("buildPath() joins the full ancestor chain with \" > \" separators")
    void buildPath_deeplyNestedCategory_joinsFullChain() {
        assertThat(helper.buildPath(screens)).isEqualTo("Hardware > Laptops > Screens");
    }

    @Test
    @DisplayName("toSummary() returns null when given a null category")
    void toSummary_nullCategory_returnsNull() {
        assertThat(helper.toSummary(null)).isNull();
    }

    @Test
    @DisplayName("toSummary() maps a category and computes its full breadcrumb path")
    void toSummary_validCategory_mapsWithComputedPath() {
        final CategorySummaryResponse expected =
                CategorySummaryResponse.builder().id(3L).name("Screens").path("x").build();
        given(categoryMapper.toSummaryResponse(screens, "Hardware > Laptops > Screens"))
                .willReturn(expected);

        assertThat(helper.toSummary(screens)).isEqualTo(expected);
    }

    @Test
    @DisplayName("resolveWithDescendants() returns null (no filter) for a null category id")
    void resolveWithDescendants_nullId_returnsNull() {
        assertThat(helper.resolveWithDescendants(null)).isNull();
    }

    @Test
    @DisplayName("resolveWithDescendants() throws for an unknown category id")
    void resolveWithDescendants_unknownId_throws() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> helper.resolveWithDescendants(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("resolveWithDescendants() includes the category itself and all nested descendants")
    void resolveWithDescendants_parentWithChildren_returnsWholeSubtree() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(hardware));
        given(categoryRepository.findAll())
                .willReturn(List.of(hardware, laptops, screens, software));

        final Set<Long> result = helper.resolveWithDescendants(1L);

        assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName(
            "resolveWithDescendants() returns only the leaf id when the category has no children")
    void resolveWithDescendants_leafCategory_returnsOnlyItself() {
        given(categoryRepository.findById(3L)).willReturn(Optional.of(screens));
        given(categoryRepository.findAll())
                .willReturn(List.of(hardware, laptops, screens, software));

        final Set<Long> result = helper.resolveWithDescendants(3L);

        assertThat(result).containsExactly(3L);
    }

    @Test
    @DisplayName("allGroupedByParentId() groups every non-root category under its parent's id")
    void allGroupedByParentId_mixedTree_groupsCorrectly() {
        given(categoryRepository.findAll())
                .willReturn(List.of(hardware, laptops, screens, software));

        final var grouped = helper.allGroupedByParentId();

        assertThat(grouped).containsOnlyKeys(1L, 2L);
        assertThat(grouped.get(1L)).containsExactly(laptops);
        assertThat(grouped.get(2L)).containsExactly(screens);
    }
}
