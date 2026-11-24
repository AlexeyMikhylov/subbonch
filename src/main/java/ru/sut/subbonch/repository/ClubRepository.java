package ru.sut.subbonch.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.dto.ClubListItemResponse;
import ru.sut.subbonch.entity.Club;

@Repository
public interface ClubRepository extends JpaRepository<Club, Integer> {

    Optional<Club> findByClub(String club);

    Optional<Club> findByClubIdAndIsActiveTrueAndIsBlockedFalse(Integer clubId);

    boolean existsByClubIgnoreCase(String club);

    boolean existsByClubIgnoreCaseAndClubIdNot(String club, Integer clubId);

    /** Каталог: активные и незаблокированные клубы. */
    Page<Club> findByIsActiveTrueAndIsBlockedFalse(Pageable pageable);

    /** Поиск по названию (без учёта регистра). */
    Page<Club> findByClubContainingIgnoreCaseAndIsBlockedFalse(String query, Pageable pageable);

    List<Club> findByCreatedByUserId(Integer userId);

    /**
     * Каталог/поиск клубов с подсчётом участников одним запросом (без N+1).
     * {@code q} — уже нормализованный паттерн поиска в нижнем регистре вида {@code %...%},
     * или {@code null} для всего каталога.
     */
    @Query(
            value = """
                    select new ru.sut.subbonch.dto.ClubListItemResponse(
                        c.clubId, c.club, c.about, c.pictureUrl, count(m.id)
                    )
                    from Club c
                    left join ClubMember m on m.club = c
                    where c.isActive = true and c.isBlocked = false
                      and (:q is null
                           or lower(c.club) like :q escape '\\'
                           or lower(coalesce(c.about, '')) like :q escape '\\')
                    group by c.clubId, c.club, c.about, c.pictureUrl
                    """,
            countQuery = """
                    select count(c)
                    from Club c
                    where c.isActive = true and c.isBlocked = false
                      and (:q is null
                           or lower(c.club) like :q escape '\\'
                           or lower(coalesce(c.about, '')) like :q escape '\\')
                    """)
    Page<ClubListItemResponse> searchCatalog(@Param("q") String q, Pageable pageable);
}

