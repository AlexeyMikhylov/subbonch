package ru.sut.subbonch.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Ballot;
import ru.sut.subbonch.entity.Poll;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.entity.Vote;

@Repository
public interface BallotRepository extends JpaRepository<Ballot, Integer> {

    boolean existsByUserAndVote(User user, Vote vote);

    long countByVote(Vote vote);

    /**
     * Голоса пользователя по конкретному опросу: у {@code Ballot} нет ссылки на
     * опрос, поэтому идём через {@code Vote.poll}.
     */
    List<Ballot> findByUserAndVotePoll(User user, Poll poll);

    /**
     * Голоса пользователя по набору опросов одним запросом (без N+1 при отображении ленты).
     */
    @Query("select b from Ballot b join fetch b.vote v join fetch v.poll p "
            + "where b.user = :user and p.pollId in :pollIds")
    List<Ballot> findByUserAndPollIds(@Param("user") User user, @Param("pollIds") Collection<Integer> pollIds);
}
