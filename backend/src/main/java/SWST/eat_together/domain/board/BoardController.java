package SWST.eat_together.domain.board;

import SWST.eat_together.domain.post.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    @GetMapping("/{page}")
    public ResponseEntity<List<Post>> getBoard(@PathVariable("page") int page) {
        int pageSize = 10;
        int pageNumber = page - 1;

        Page<Post> postPage = boardService.getPostsByPage(pageNumber, pageSize);

        if (postPage.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Post> board = postPage.getContent();
        return ResponseEntity.ok(board);
    }

}