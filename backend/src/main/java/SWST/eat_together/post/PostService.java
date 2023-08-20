package SWST.eat_together.post;

import SWST.eat_together.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

    public void addPost(Member member, RegiPostDTO regiPost){
        Post post = new Post();

        post.setTitle(regiPost.getTitle());
        post.setContents(regiPost.getContents());
        post.setEmail(member.getEmail());

        Date currentDate = new Date(System.currentTimeMillis());
        post.setCreatedDate(formatter.format(currentDate));
        post.setNickname(member.getNickname());

        try {
            postRepository.save(post);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Post detail(long id, String email) {

        Post post=postRepository.findById(id);

        PostDetailDTO postDetail = new PostDetailDTO();

        postDetail.setId(post.getId());
        postDetail.setEmail(post.getEmail());
        postDetail.setNickname(post.getNickname());
        postDetail.setCreatedDate(post.getCreatedDate());
        postDetail.setTitle(post.getTitle());
        postDetail.setContents(post.getContents());

        postDetail.setAuthor(false);

        if (email != null && post.getEmail().equals(email)) {
            postDetail.setAuthor(true);
        }

        return postDetail;
    }

    public Integer delete(int id, String email) {
        Post post=postRepository.findById(id);

        if (!email.equals(post.getEmail()))
            return -1;
        postRepository.delete(post);

        return 0;
    }

    public Integer edit(int id, String email, String title, String contents) {
        Post post=postRepository.findById(id);

        if (!email.equals(post.getEmail()))
            return -1;

        post.setTitle(title);
        post.setContents(contents);
        postRepository.save(post);
        return 0;
    }
}