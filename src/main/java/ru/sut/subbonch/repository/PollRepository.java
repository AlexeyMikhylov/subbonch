package ru.sut.subbonch.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Poll;
import ru.sut.subbonch.entity.Post;

@Repository
public interface PollRepository extends JpaRepository<Poll, Integer> {

    Optional<Poll> findByPost(Post post);

    Optional<Poll> findByPostPostId(Integer postId);

    boolean existsByPostPostId(Integer postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Poll p where p.pollId = :pollId")
    Optional<Poll> findByIdForUpdate(@Param("pollId") Integer pollId);

    @Query("select p from Poll p join fetch p.post where p.post.postId in :postIds")
    List<Poll> findByPostPostIdIn(@Param("postIds") Collection<Integer> postIds);
}
