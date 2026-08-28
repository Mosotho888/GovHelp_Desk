package za.gov.helpdesk.category.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.category.dto.request.CreateCategoryRequest;
import za.gov.helpdesk.category.dto.request.UpdateCategoryRequest;
import za.gov.helpdesk.category.dto.response.CategoryResponse;
import za.gov.helpdesk.category.exception.InvalidCategoryOperationException;
import za.gov.helpdesk.category.mapper.CategoryMapper;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.repository.CategoryRepository;
import za.gov.helpdesk.category.service.CategoryQueryHelper;
import za.gov.helpdesk.category.service.CategoryService;
import za.gov.helpdesk.exception.DuplicateResourceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryQueryHelper categoryQuery;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree(final boolean activeOnly) {
        final List<Category> roots = categoryRepository.findByParentIsNullOrderByNameAsc();
        final Map<Long, List<Category>> childrenByParentId = categoryQuery.allGroupedByParentId();

        return roots.stream()
                .filter(root -> !activeOnly || root.isActive())
                .map(root -> buildTree(root, childrenByParentId, activeOnly))
                .toList();
    }

    private CategoryResponse buildTree(
            final Category category,
            final Map<Long, List<Category>> childrenByParentId,
            final boolean activeOnly) {

        final List<CategoryResponse> children =
                childrenByParentId.getOrDefault(category.getId(), List.of()).stream()
                        .filter(child -> !activeOnly || child.isActive())
                        .map(child -> buildTree(child, childrenByParentId, activeOnly))
                        .toList();

        final CategoryResponse response = categoryMapper.toFlatResponse(category);
        response.setChildren(children);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(final Long categoryId) {
        return categoryMapper.toFlatResponse(categoryQuery.findOrThrow(categoryId));
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(final CreateCategoryRequest request) {
        Category parent = null;
        short level = 0;
        String defaultDepartment = request.getDefaultDepartment();

        if (request.getParentId() != null) {
            parent = categoryQuery.findOrThrow(request.getParentId());
            if (parent.getLevel() >= Category.MAX_LEVEL) {
                throw new InvalidCategoryOperationException(
                        "Cannot nest a category under '"
                                + parent.getName()
                                + "': maximum category depth ("
                                + (Category.MAX_LEVEL + 1)
                                + " levels) has been reached");
            }
            level = (short) (parent.getLevel() + 1);
            if (defaultDepartment == null) {
                defaultDepartment = parent.getDefaultDepartment();
            }
        }

        validateUniqueNameAtLevel(parent != null ? parent.getId() : null, request.getName());

        final String slug = generateUniqueSlug(request.getName());

        final Category category =
                Category.builder()
                        .name(request.getName())
                        .slug(slug)
                        .parent(parent)
                        .level(level)
                        .defaultDepartment(defaultDepartment)
                        .active(true)
                        .build();

        return categoryMapper.toFlatResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(
            final Long categoryId, final UpdateCategoryRequest request) {
        final Category category = categoryQuery.findOrThrow(categoryId);

        if (request.getName() != null && !request.getName().equalsIgnoreCase(category.getName())) {
            final Long parentId =
                    category.getParent() != null ? category.getParent().getId() : null;
            validateUniqueNameAtLevel(parentId, request.getName());

            category.setName(request.getName());
            category.setSlug(generateUniqueSlug(request.getName())); // Keep slug in sync
        }

        if (request.getDefaultDepartment() != null) {
            category.setDefaultDepartment(request.getDefaultDepartment());
        }

        if (request.getActive() != null) {
            applyActiveFlag(category, request.getActive());
        }

        // Leveraging JPA Dirty Checking: save() is unnecessary
        return categoryMapper.toFlatResponse(category);
    }

    @Override
    @Transactional
    public void deactivateCategory(final Long categoryId) {
        final Category category = categoryQuery.findOrThrow(categoryId);
        applyActiveFlag(category, false);
    }

    private void applyActiveFlag(final Category category, final boolean active) {
        if (!active) {
            // Optimized query avoiding fetching and mapping full entities
            final boolean hasActiveChildren =
                    categoryRepository.existsByParentIdAndActiveTrue(category.getId());
            if (hasActiveChildren) {
                throw new InvalidCategoryOperationException(
                        "Cannot deactivate '"
                                + category.getName()
                                + "': it still has active subcategories. Deactivate those first.");
            }
        }
        category.setActive(active);
    }

    private void validateUniqueNameAtLevel(final Long parentId, final String name) {
        final boolean exists =
                parentId == null
                        ? categoryRepository.existsByParentIsNullAndNameIgnoreCase(name)
                        : categoryRepository.existsByParentIdAndNameIgnoreCase(parentId, name);

        if (exists) {
            throw new DuplicateResourceException(
                    "A category named '" + name + "' already exists at this level");
        }
    }

    private String generateUniqueSlug(final String name) {
        final String base =
                name.toLowerCase(Locale.ROOT)
                        .trim()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-|-$)", "");
        String candidate = base;
        int suffix = 2;
        while (categoryRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
