package za.gov.helpdesk.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.auth.model.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Most recent unused token for this email
    Optional<PasswordResetToken> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    // Invalidate all previous tokens for this email before issuing a new one
    @Modifying
    @Query(
            "UPDATE PasswordResetToken t SET t.used = true WHERE t.email = :email AND t.used ="
                    + " false")
    void invalidateAllByEmail(String email);

    // Cleanup
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredBefore(LocalDateTime cutoff);
}
