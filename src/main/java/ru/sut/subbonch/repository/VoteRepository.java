package ru.sut.subbonch.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Poll;
import ru.sut.subbonch.entity.Vote;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {

    List<Vote> findByPoll(Poll poll);

    List<Vote> findByPollPollId(Integer pollId);

    long countByPoll(Poll poll);
}
