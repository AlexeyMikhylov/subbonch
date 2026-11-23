package ru.sut.subbonch.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Club;

@Repository
public interface ClubRepository extends JpaRepository<Club, Integer> {

    Optional<Club> findByClub(String club);

    boolean existsByClubIgnoreCase(String club);

    /** Каталог: активные и незаблокированные клубы. */
    Page<Club> findByIsActiveTrueAndIsBlockedFalse(Pageable pageable);

    /** Поиск по названию (без учёта регистра). */
    Page<Club> findByClubContainingIgnoreCaseAndIsBlockedFalse(String query, Pageable pageable);

    List<Club> findByCreatedByUserId(Integer userId);
}
