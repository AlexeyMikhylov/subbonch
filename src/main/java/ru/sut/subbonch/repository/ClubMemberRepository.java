package ru.sut.subbonch.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.User;

@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Integer> {

    Optional<ClubMember> findByUserAndClub(User user, Club club);

    boolean existsByUserAndClub(User user, Club club);

    List<ClubMember> findByClub(Club club);

    List<ClubMember> findByClubAndUserRole(Club club, ClubRole userRole);

    long countByClub(Club club);

    long countByUser(User user);

    /** «Мои клубы»: клубы сразу подгружаются, чтобы не ловить lazy вне транзакции. */
    @EntityGraph(attributePaths = "club")
    List<ClubMember> findByUser(User user);
}
