package ru.sut.subbonch.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Application;
import ru.sut.subbonch.entity.ApplicationStatus;
import ru.sut.subbonch.entity.User;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    @EntityGraph(attributePaths = "author")
    List<Application> findByAppStatusOrderByCreatedAtAsc(ApplicationStatus appStatus);

    List<Application> findByAuthorOrderByCreatedAtDesc(User author);

    /** Одна активная (PENDING) заявка на пользователя. */
    boolean existsByAuthorAndAppStatus(User author, ApplicationStatus appStatus);

    /** Название клуба из заявки не должно конфликтовать с уже существующим. */
    boolean existsByClubNameIgnoreCase(String clubName);
}
