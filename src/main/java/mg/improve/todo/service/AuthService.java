package mg.improve.todo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mg.improve.todo.config.AuthCookieFactory;
import mg.improve.todo.config.JwtService;
import mg.improve.todo.domain.dto.AuthResponse;
import mg.improve.todo.domain.dto.AuthResult;
import mg.improve.todo.domain.dto.LoginRequest;
import mg.improve.todo.domain.dto.RefreshResult;
import mg.improve.todo.domain.dto.RegisterRequest;
import mg.improve.todo.domain.dto.UserResponse;
import mg.improve.todo.domain.entity.RefreshToken;
import mg.improve.todo.domain.entity.User;
import mg.improve.todo.domain.mappers.RefreshTokenMapper;
import mg.improve.todo.domain.mappers.UserMapper;
import mg.improve.todo.exception.EmailAlreadyUsedException;
import mg.improve.todo.exception.InvalidCredentialsException;
import mg.improve.todo.exception.InvalidRefreshTokenException;
import mg.improve.todo.exception.UserNotFoundException;
import mg.improve.todo.repository.RefreshTokenRepository;
import mg.improve.todo.repository.UserRepository;
import mg.improve.todo.repository.entity.JRefreshToken;
import mg.improve.todo.repository.entity.JUser;
import mg.improve.todo.validators.LoginValidator;
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

	private static final String TOKEN_TYPE_BEARER = "Bearer";

	private static final Duration REFRESH_ROTATION_THRESHOLD = Duration.ofHours(24);

	private static final String DUMMY_PASSWORD_HASH =
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	private final UserRepository userRepository;

	private final RefreshTokenRepository refreshTokenRepository;

	private final UserMapper userMapper;

	private final RefreshTokenMapper refreshTokenMapper;

	private final JwtService jwtService;

	private final PasswordEncoder passwordEncoder;

	private final RegisterValidator registerValidator;

	private final LoginValidator loginValidator;

	private final AuthCookieFactory authCookieFactory;

	@Transactional
	public AuthResult register(RegisterRequest request) {
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

		return issueTokens(userMapper.toDomain(saved));
	}

	@Transactional
	public AuthResult login(LoginRequest request) {
		loginValidator.validate(request);

		String email = request.email().trim();
		JUser jpa = userRepository.findByEmail(email).orElse(null);

		String passwordHash =
				(jpa != null && jpa.getPasswordHash() != null) ? jpa.getPasswordHash() : DUMMY_PASSWORD_HASH;
		boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);

		if (jpa == null || !passwordMatches) {
			throw new InvalidCredentialsException();
		}

		return issueTokens(userMapper.toDomain(jpa));
	}

	@Transactional
	public RefreshResult refresh(String rawRefreshToken) {
		UUID userId = extractRefreshTokenUser(rawRefreshToken);
		String value = extractRefreshTokenValue(rawRefreshToken);

		JRefreshToken stored = refreshTokenRepository.findByValue(value)
				.orElseThrow(InvalidRefreshTokenException::new);
		if (!stored.getUser().getId().equals(userId)) {
			throw new InvalidRefreshTokenException();
		}

		User user = userMapper.toDomain(stored.getUser());

		Duration accessTtl = Duration.ofMillis(jwtService.getAccessExpiration());
		Duration refreshTtl = Duration.ofMillis(jwtService.getRefreshExpiration());

		String accessToken = createAccessToken(user);

		String refreshToken;
		Instant refreshExpiresAt;
		if (stored.getExpiresAt().isBefore(Instant.now().plus(REFRESH_ROTATION_THRESHOLD))) {
			refreshTokenRepository.delete(stored);
			IssuedRefreshToken issued = createAndPersistRefreshToken(user, refreshTtl);
			refreshToken = issued.token();
			refreshExpiresAt = issued.expiresAt();
		}
		else {
			refreshToken = rawRefreshToken;
			refreshExpiresAt = stored.getExpiresAt();
		}

		List<ResponseCookie> cookies = List.of(
				authCookieFactory.accessToken(accessToken, accessTtl),
				authCookieFactory.refreshToken(
						refreshToken, Duration.between(Instant.now(), refreshExpiresAt)));

		var body = new AuthResponse(
				accessToken,
				refreshToken,
				TOKEN_TYPE_BEARER,
				accessTtl.getSeconds(),
				UserResponse.from(user));

		return new RefreshResult(body, cookies);
	}

	@Transactional(readOnly = true)
	public User getCurrentUser(UUID userId) {
		JUser jpa = userRepository.findById(userId)
				.orElseThrow(UserNotFoundException::new);
		return userMapper.toDomain(jpa);
	}

	@Transactional
	public List<ResponseCookie> deleteCurrentUser(UUID userId) {
		JUser jpa = userRepository.findById(userId)
				.orElseThrow(UserNotFoundException::new);
		userRepository.delete(jpa);
		return authCookieFactory.clearAuthCookies();
	}

	private UUID extractRefreshTokenUser(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()
				|| !jwtService.verifyToken(rawRefreshToken)) {
			throw new InvalidRefreshTokenException();
		}
		if (!REFRESH_TOKEN_TYPE.equals(jwtService.extractClaim(rawRefreshToken, TOKEN_TYPE_CLAIM))) {
			throw new InvalidRefreshTokenException();
		}
		try {
			return UUID.fromString(jwtService.extractSubject(rawRefreshToken));
		}
		catch (IllegalArgumentException | NullPointerException e) {
			throw new InvalidRefreshTokenException();
		}
	}

	private String extractRefreshTokenValue(String rawRefreshToken) {
		Object value = jwtService.extractClaim(rawRefreshToken, REFRESH_TOKEN_VALUE_CLAIM);
		if (value == null || value.toString().isBlank()) {
			throw new InvalidRefreshTokenException();
		}
		return value.toString();
	}

	private AuthResult issueTokens(User user) {
		Duration accessTtl = Duration.ofMillis(jwtService.getAccessExpiration());
		Duration refreshTtl = Duration.ofMillis(jwtService.getRefreshExpiration());

		String accessToken = createAccessToken(user);
		IssuedRefreshToken issuedRefresh = createAndPersistRefreshToken(user, refreshTtl);

		return new AuthResult(user, List.of(
				authCookieFactory.accessToken(accessToken, accessTtl),
				authCookieFactory.refreshToken(issuedRefresh.token(), refreshTtl)));
	}

	private String createAccessToken(User user) {
		return jwtService.createToken(
				user.getId(), true, Map.of(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE));
	}

	private IssuedRefreshToken createAndPersistRefreshToken(User user, Duration ttl) {
		UUID value = UUID.randomUUID();
		Instant expiresAt = Instant.now().plus(ttl);
		String token = jwtService.createToken(
				user.getId(), false, Map.of(
						TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE,
						REFRESH_TOKEN_VALUE_CLAIM, value.toString()));

		var persistedRefreshToken = new RefreshToken();
		persistedRefreshToken.setUser(user);
		persistedRefreshToken.setValue(value.toString());
		persistedRefreshToken.setExpiresAt(expiresAt);
		refreshTokenRepository.save(refreshTokenMapper.toJpa(persistedRefreshToken));

		return new IssuedRefreshToken(token, expiresAt);
	}

	private record IssuedRefreshToken(String token, Instant expiresAt) {
	}
}
