package mg.improve.todo.domain.dto;

import java.util.List;
import org.springframework.http.ResponseCookie;

public record RefreshResult(AuthResponse body, List<ResponseCookie> cookies) {
}
