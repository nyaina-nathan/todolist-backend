package mg.improve.todo.config;

import java.time.Duration;
import java.util.List;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieFactory {

	public static final String ACCESS_TOKEN_COOKIE = "access_token";

	public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

	public ResponseCookie accessToken(String value, Duration ttl) {
		return build(ACCESS_TOKEN_COOKIE, value, ttl);
	}

	public ResponseCookie refreshToken(String value, Duration ttl) {
		return build(REFRESH_TOKEN_COOKIE, value, ttl);
	}

	public List<ResponseCookie> clearAuthCookies() {
		return List.of(accessToken("", Duration.ZERO), refreshToken("", Duration.ZERO));
	}

	private ResponseCookie build(String name, String value, Duration ttl) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path("/")
				.maxAge(ttl)
				.build();
	}
}
