package za.gov.helpdesk.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.auth.model.RefreshToken;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user = :user AND t.revoked = false")
    void revokeAllByUser(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredBefore(LocalDateTime cutoff);
}
