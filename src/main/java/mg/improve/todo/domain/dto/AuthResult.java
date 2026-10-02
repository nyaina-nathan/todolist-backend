package mg.improve.todo.domain.dto;

import java.util.List;
import mg.improve.todo.domain.entity.User;
import org.springframework.http.ResponseCookie;

public record AuthResult(User user, List<ResponseCookie> cookies) {
}
