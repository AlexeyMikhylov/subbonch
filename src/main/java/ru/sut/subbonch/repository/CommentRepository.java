package ru.sut.subbonch.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sut.subbonch.entity.Comment;
import ru.sut.subbonch.entity.Post;
import ru.sut.subbonch.entity.User;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    /** Все неудалённые комментарии поста по времени создания. */
    List<Comment> findByPostAndIsDeletedFalseOrderByCreatedAtAsc(Post post);

    List<Comment> findByPostPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Integer postId);

    /** Неудалённые комментарии для набора постов: автор, пост и parent подгружаются батчем. */
    @EntityGraph(attributePaths = {"user", "post", "parent"})
    List<Comment> findByPostPostIdInAndIsDeletedFalseOrderByCreatedAtAsc(Collection<Integer> postIds);

    /** Корневые комментарии поста (верхний уровень дерева). */
    List<Comment> findByPostPostIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtAsc(Integer postId);

    long countByUserAndIsDeletedFalse(User user);
}
