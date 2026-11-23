package ru.sut.subbonch.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.AttachmentComment;
import ru.sut.subbonch.entity.Comment;

@Repository
public interface AttachmentCommentRepository extends JpaRepository<AttachmentComment, Integer> {

    List<AttachmentComment> findByComment(Comment comment);

    List<AttachmentComment> findByCommentCommentId(Integer commentId);
}
