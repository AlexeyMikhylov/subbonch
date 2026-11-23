package ru.sut.subbonch.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    /** Лента клуба: только неудалённые посты, новые сверху. */
    Page<Post> findByClubAndIsDeletedFalse(Club club, Pageable pageable);

    Page<Post> findByClubClubIdAndIsDeletedFalse(Integer clubId, Pageable pageable);

    Optional<Post> findByPostIdAndIsDeletedFalse(Integer postId);

    Optional<Post> findByPostIdAndClubClubId(Integer postId, Integer clubId);

    long countByClubAndIsDeletedFalse(Club club);

    @Query("select p from Post p join fetch p.user join fetch p.club "
            + "where p.club.clubId = :clubId and p.isDeleted = false "
            + "order by p.createdAt desc")
    List<Post> findActiveByClubIdWithUser(@Param("clubId") Integer clubId);

    @Query("select p from Post p join fetch p.user join fetch p.club c "
            + "where p.isDeleted = false and c.isActive = true and c.isBlocked = false "
            + "order by p.createdAt desc")
    List<Post> findFeedForYou();

    @Query("select p from Post p join fetch p.user join fetch p.club c "
            + "where p.isDeleted = false and c.isActive = true and c.isBlocked = false "
            + "and exists (select 1 from ClubMember m where m.club = c and m.user.userId = :userId) "
            + "order by p.createdAt desc")
    List<Post> findFeedSubscriptions(@Param("userId") Integer userId);
}
