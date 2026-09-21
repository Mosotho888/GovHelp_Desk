package za.gov.helpdesk.knowledgebase.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;

@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    Optional<KnowledgeArticle> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query(
            """
            SELECT DISTINCT a FROM KnowledgeArticle a LEFT JOIN a.tags t
            WHERE (:status IS NULL OR a.status = :status)
              AND (:type IS NULL OR a.type = :type)
              AND (:categoryId IS NULL OR a.category.id = :categoryId)
              AND (:tag IS NULL OR t = :tag)
            """)
    Page<KnowledgeArticle> findWithFilters(
            @Param("status") KnowledgeArticle.ArticleStatus status,
            @Param("type") KnowledgeArticle.ArticleType type,
            @Param("categoryId") Long categoryId,
            @Param("tag") String tag,
            Pageable pageable);

    /**
     * Full-text search over title (highest weight), summary, and content, using the generated
     * {@code search_vector} column from {@code V10__create_knowledge_base.sql}. Results are ranked
     * by {@code ts_rank} rather than returned in an arbitrary or purely chronological order, which
     * is what makes this a real search rather than a filtered list.
     */
    @Query(
            value =
                    """
                    SELECT * FROM knowledge_articles a
                    WHERE (:status IS NULL OR a.status = :status)
                      AND a.search_vector @@ plainto_tsquery('english', :query)
                    ORDER BY ts_rank(a.search_vector, plainto_tsquery('english', :query)) DESC
                    """,
            countQuery =
                    """
                    SELECT count(*) FROM knowledge_articles a
                    WHERE (:status IS NULL OR a.status = :status)
                      AND a.search_vector @@ plainto_tsquery('english', :query)
                    """,
            nativeQuery = true)
    Page<KnowledgeArticle> search(
            @Param("query") String query, @Param("status") String status, Pageable pageable);

    @Modifying
    @Query("UPDATE KnowledgeArticle a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
