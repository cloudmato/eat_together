package SWST.eat_together.comment.model;

import SWST.eat_together.comment.Comment;
import lombok.Data;

@Data
public class CommentDetailDTO extends Comment {
    private boolean isAuthor;
}
