package mg.improve.todo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String ACCESS_TOKEN_COOKIE = "access_token";

	public static final String TOKEN_TYPE_CLAIM = "token_type";

	public static final String ACCESS_TOKEN_TYPE = "access";

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = resolveAccessToken(request);
			if (token != null) {
				authenticate(token, request);
			}
		}
		filterChain.doFilter(request, response);
	}

	private String resolveAccessToken(HttpServletRequest request) {
		String cookieToken = extractCookieToken(request);
		if (isValidAccessToken(cookieToken)) {
			return cookieToken;
		}
		String headerToken = extractHeaderToken(request);
		if (isValidAccessToken(headerToken)) {
			return headerToken;
		}
		return null;
	}

	private void authenticate(String token, HttpServletRequest request) {
		String subject = jwtService.extractSubject(token);
		if (subject == null) {
			return;
		}
		UUID userId;
		try {
			userId = UUID.fromString(subject);
		}
		catch (IllegalArgumentException e) {
			return;
		}
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(userId, null, List.of());
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private boolean isValidAccessToken(String token) {
		if (token == null || !jwtService.verifyToken(token)) {
			return false;
		}
		return ACCESS_TOKEN_TYPE.equals(jwtService.extractClaim(token, TOKEN_TYPE_CLAIM));
	}

	private String extractCookieToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private String extractHeaderToken(HttpServletRequest request) {
		String header = request.getHeader(AUTHORIZATION_HEADER);
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return header.substring(BEARER_PREFIX.length()).trim();
	}
}
