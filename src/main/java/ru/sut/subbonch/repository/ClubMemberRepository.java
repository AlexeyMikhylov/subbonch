package ru.sut.subbonch.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Участники клуба для управления: user подгружается сразу, без N+1. */
    @EntityGraph(attributePaths = "user")
    Page<ClubMember> findByClub(Club club, Pageable pageable);

    List<ClubMember> findByClubAndUserRole(Club club, ClubRole userRole);

    /** Команда клуба (ADMIN/MODERATOR) с подгруженными пользователями, без N+1. */
    @EntityGraph(attributePaths = "user")
    List<ClubMember> findByClubAndUserRoleIn(Club club, Collection<ClubRole> roles);

    long countByClub(Club club);

    long countByUser(User user);

    /** «Мои клубы»: клубы сразу подгружаются, чтобы не ловить lazy вне транзакции. */
    @EntityGraph(attributePaths = "club")
    List<ClubMember> findByUser(User user);
}
