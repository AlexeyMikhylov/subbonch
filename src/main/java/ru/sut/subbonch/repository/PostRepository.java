package ru.sut.subbonch.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    /** Лента клуба: только неудалённые посты, новые сверху. */
    Page<Post> findByClubAndIsDeletedFalse(Club club, Pageable pageable);

    Page<Post> findByClubClubIdAndIsDeletedFalse(Integer clubId, Pageable pageable);

    Optional<Post> findByPostIdAndIsDeletedFalse(Integer postId);

    long countByClubAndIsDeletedFalse(Club club);
}
