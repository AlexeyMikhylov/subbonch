package ru.sut.subbonch.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /** Email хранится в нижнем регистре, поэтому достаточно точного совпадения. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
