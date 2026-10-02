package mg.improve.todo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mg.improve.todo.config.JwtService;
import mg.improve.todo.domain.dto.AuthResult;
import mg.improve.todo.domain.dto.LoginRequest;
import mg.improve.todo.domain.dto.RegisterRequest;
import mg.improve.todo.domain.entity.RefreshToken;
import mg.improve.todo.domain.entity.User;
import mg.improve.todo.domain.mappers.RefreshTokenMapper;
import mg.improve.todo.domain.mappers.UserMapper;
import mg.improve.todo.exception.EmailAlreadyUsedException;
import mg.improve.todo.exception.InvalidCredentialsException;
import mg.improve.todo.exception.UserNotFoundException;
import mg.improve.todo.repository.RefreshTokenRepository;
import mg.improve.todo.repository.UserRepository;
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

	public static final String ACCESS_TOKEN_COOKIE = "access_token";

	public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

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
		return List.of(
				buildCookie(ACCESS_TOKEN_COOKIE, "", Duration.ZERO),
				buildCookie(REFRESH_TOKEN_COOKIE, "", Duration.ZERO));
	}

	private AuthResult issueTokens(User user) {
		Duration accessTtl = Duration.ofMillis(jwtService.getAccessExpiration());
		Duration refreshTtl = Duration.ofMillis(jwtService.getRefreshExpiration());

		String accessToken = jwtService.createToken(
				user.getId(), true, Map.of(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE));

		UUID refreshTokenValue = UUID.randomUUID();
		String refreshToken = jwtService.createToken(
				user.getId(), false, Map.of(
						TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE,
						REFRESH_TOKEN_VALUE_CLAIM, refreshTokenValue.toString()));

		var persistedRefreshToken = new RefreshToken();
		persistedRefreshToken.setUser(user);
		persistedRefreshToken.setValue(refreshTokenValue.toString());
		persistedRefreshToken.setExpiresAt(Instant.now().plus(refreshTtl));
		refreshTokenRepository.save(refreshTokenMapper.toJpa(persistedRefreshToken));

		return new AuthResult(user, List.of(
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
