package ru.sut.subbonch.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Poll;
import ru.sut.subbonch.entity.Vote;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {

    List<Vote> findByPoll(Poll poll);

    List<Vote> findByPollPollId(Integer pollId);

    long countByPoll(Poll poll);

    @Query("select v from Vote v join fetch v.poll where v.poll.pollId in :pollIds")
    List<Vote> findByPollPollIdIn(@Param("pollIds") Collection<Integer> pollIds);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Vote v set v.total = v.total + 1 where v.voteId = :voteId")
    int incrementTotal(@Param("voteId") Integer voteId);
}
