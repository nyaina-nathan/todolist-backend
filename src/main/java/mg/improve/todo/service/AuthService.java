package mg.improve.todo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mg.improve.todo.config.JwtService;
import mg.improve.todo.domain.dto.RegisterRequest;
import mg.improve.todo.domain.dto.RegistrationResult;
import mg.improve.todo.domain.entity.RefreshToken;
import mg.improve.todo.domain.entity.User;
import mg.improve.todo.domain.mappers.RefreshTokenMapper;
import mg.improve.todo.domain.mappers.UserMapper;
import mg.improve.todo.exception.EmailAlreadyUsedException;
import mg.improve.todo.repository.RefreshTokenRepository;
import mg.improve.todo.repository.UserRepository;
import mg.improve.todo.repository.entity.JUser;
import mg.improve.todo.validators.RegisterValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class AuthService {

	public static final String TOKEN_TYPE_CLAIM = "token_type";

	public static final String ACCESS_TOKEN_TYPE = "access";

	public static final String REFRESH_TOKEN_TYPE = "refresh";

	public static final String REFRESH_TOKEN_VALUE_CLAIM = "value";

	public static final String ACCESS_TOKEN_COOKIE = "access_token";

	public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

	private final UserRepository userRepository;

	private final RefreshTokenRepository refreshTokenRepository;

	private final UserMapper userMapper;

	private final RefreshTokenMapper refreshTokenMapper;

	private final JwtService jwtService;

	private final PasswordEncoder passwordEncoder;

	private final RegisterValidator registerValidator;


	@Transactional
	public RegistrationResult register(RegisterRequest request) {
		registerValidator.validate(request);

		String email = request.email().trim();
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyUsedException(email);
		}

		var user = new User();
		user.setEmail(email);
		user.setUsername(request.username().trim());
		user.setPasswordHash(passwordEncoder.encode(request.password()));

		JUser saved;
		try {
			saved = userRepository.save(userMapper.toJpa(user));
		}
		catch (DataIntegrityViolationException e) {
			throw new EmailAlreadyUsedException(email);
		}

		User registered = userMapper.toDomain(saved);

		Duration accessTtl = Duration.ofMillis(jwtService.getAccessExpiration());
		Duration refreshTtl = Duration.ofMillis(jwtService.getRefreshExpiration());

		String accessToken = jwtService.createToken(
				registered.getId(), true, Map.of(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE));

		UUID refreshTokenValue = UUID.randomUUID();
		String refreshToken = jwtService.createToken(
				registered.getId(), false, Map.of(
						TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE,
						REFRESH_TOKEN_VALUE_CLAIM, refreshTokenValue.toString()));

		var persistedRefreshToken = new RefreshToken();
		persistedRefreshToken.setUser(registered);
		persistedRefreshToken.setValue(refreshTokenValue.toString());
		persistedRefreshToken.setExpiresAt(Instant.now().plus(refreshTtl));
		refreshTokenRepository.save(refreshTokenMapper.toJpa(persistedRefreshToken));

		return new RegistrationResult(registered, List.of(
				buildCookie(ACCESS_TOKEN_COOKIE, accessToken, accessTtl),
				buildCookie(REFRESH_TOKEN_COOKIE, refreshToken, refreshTtl)));
	}

	private ResponseCookie buildCookie(String name, String value, Duration ttl) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path("/")
				.maxAge(ttl)
				.build();
	}
}
