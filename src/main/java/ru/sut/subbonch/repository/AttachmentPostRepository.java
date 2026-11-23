package ru.sut.subbonch.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.AttachmentPost;
import ru.sut.subbonch.entity.Post;

@Repository
public interface AttachmentPostRepository extends JpaRepository<AttachmentPost, Integer> {

    List<AttachmentPost> findByPost(Post post);

    List<AttachmentPost> findByPostPostId(Integer postId);
}
