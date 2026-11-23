package ru.sut.subbonch.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Poll;
import ru.sut.subbonch.entity.Post;

@Repository
public interface PollRepository extends JpaRepository<Poll, Integer> {

    Optional<Poll> findByPost(Post post);

    Optional<Poll> findByPostPostId(Integer postId);

    boolean existsByPostPostId(Integer postId);
}
