package mg.improve.todo.domain.dto;

import java.util.List;

import mg.improve.todo.domain.dto.response.AuthResponse;

import org.springframework.http.ResponseCookie;

public record RefreshResult(AuthResponse body, List<ResponseCookie> cookies) {
}
