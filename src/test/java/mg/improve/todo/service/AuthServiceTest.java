package mg.improve.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import mg.improve.todo.config.AuthCookieFactory;
import mg.improve.todo.config.JwtService;
import mg.improve.todo.domain.dto.AuthResult;
import mg.improve.todo.domain.dto.request.LoginRequest;
import mg.improve.todo.domain.dto.RefreshResult;
import mg.improve.todo.domain.dto.request.RegisterRequest;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final UUID USER_ID = UUID.randomUUID();

	private static final String EMAIL = "user@example.com";

	private static final String PASSWORD = "password123";

	private static final String PASSWORD_HASH = "hash";

	private static final String REFRESH_VALUE = UUID.randomUUID().toString();

	private static final String RAW_REFRESH = "raw-refresh-token";

	private static final String ACCESS_TOKEN = "access-token";

	private static final String REFRESH_TOKEN = "refresh-token";

	private static final long ACCESS_TTL_MS = 900_000L;

	private static final long REFRESH_TTL_MS = 604_800_000L;

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private UserMapper userMapper;

	@Mock
	private RefreshTokenMapper refreshTokenMapper;

	@Mock
	private JwtService jwtService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RegisterValidator registerValidator;

	@Mock
	private LoginValidator loginValidator;

	@Mock
	private AuthCookieFactory authCookieFactory;

	@InjectMocks
	private AuthService authService;

	@Test
	void registerCreatesUserAndIssuesTokens() {
		RegisterRequest request = new RegisterRequest("  " + EMAIL + "  ", "  nick  ", PASSWORD);
		JUser jpa = jUser(USER_ID, EMAIL);
		User domain = user(USER_ID, EMAIL);

		given(userRepository.existsByEmail(EMAIL)).willReturn(false);
		given(passwordEncoder.encode(PASSWORD)).willReturn(PASSWORD_HASH);
		given(userMapper.toJpa(any(User.class))).willReturn(jpa);
		given(userRepository.save(jpa)).willReturn(jpa);
		given(userMapper.toDomain(jpa)).willReturn(domain);
		stubIssuedTokens();

		AuthResult result = authService.register(request);

		assertThat(result.user()).isSameAs(domain);
		assertThat(result.cookies()).hasSize(2);
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userMapper).toJpa(captor.capture());
		User captured = captor.getValue();
		assertThat(captured.getEmail()).isEqualTo(EMAIL);
		assertThat(captured.getUsername()).isEqualTo("nick");
		assertThat(captured.getPasswordHash()).isEqualTo(PASSWORD_HASH);
		verify(registerValidator).validate(request);
		verify(refreshTokenRepository).save(any(JRefreshToken.class));
	}

	@Test
	void registerThrowsWhenEmailAlreadyUsed() {
		RegisterRequest request = new RegisterRequest(EMAIL, "nick", PASSWORD);
		given(userRepository.existsByEmail(EMAIL)).willReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(EmailAlreadyUsedException.class);
		verify(userRepository, never()).save(any());
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void registerMapsDataIntegrityViolationToEmailAlreadyUsed() {
		RegisterRequest request = new RegisterRequest(EMAIL, "nick", PASSWORD);
		JUser jpa = jUser(USER_ID, EMAIL);

		given(userRepository.existsByEmail(EMAIL)).willReturn(false);
		given(passwordEncoder.encode(PASSWORD)).willReturn(PASSWORD_HASH);
		given(userMapper.toJpa(any(User.class))).willReturn(jpa);
		given(userRepository.save(jpa)).willThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(EmailAlreadyUsedException.class);
	}

	@Test
	void loginReturnsTokensOnValidCredentials() {
		LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
		JUser jpa = jUser(USER_ID, EMAIL);
		User domain = user(USER_ID, EMAIL);

		given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(jpa));
		given(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).willReturn(true);
		given(userMapper.toDomain(jpa)).willReturn(domain);
		stubIssuedTokens();

		AuthResult result = authService.login(request);

		assertThat(result.user()).isSameAs(domain);
		verify(loginValidator).validate(request);
	}

	@Test
	void loginThrowsWhenUserNotFound() {
		LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
		given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(InvalidCredentialsException.class);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	void loginThrowsWhenPasswordDoesNotMatch() {
		LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
		JUser jpa = jUser(USER_ID, EMAIL);
		given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(jpa));
		given(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).willReturn(false);

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(InvalidCredentialsException.class);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	void refreshWithoutRotationReusesRefreshToken() {
		JUser jpa = jUser(USER_ID, EMAIL);
		JRefreshToken stored = storedToken(jpa, Instant.now().plus(Duration.ofDays(30)));

		stubRefreshClaims(RAW_REFRESH);
		given(refreshTokenRepository.findByValue(REFRESH_VALUE)).willReturn(Optional.of(stored));
		given(userMapper.toDomain(jpa)).willReturn(user(USER_ID, EMAIL));
		stubAccessTokenAndCookies();
		given(jwtService.getRefreshExpiration()).willReturn(REFRESH_TTL_MS);

		RefreshResult result = authService.refresh(RAW_REFRESH);

		assertThat(result.body().accessToken()).isEqualTo(ACCESS_TOKEN);
		assertThat(result.body().refreshToken()).isEqualTo(RAW_REFRESH);
		assertThat(result.body().tokenType()).isEqualTo("Bearer");
		assertThat(result.cookies()).hasSize(2);
		verify(refreshTokenRepository, never()).delete(any());
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void refreshRotatesExpiringRefreshToken() {
		JUser jpa = jUser(USER_ID, EMAIL);
		JRefreshToken stored = storedToken(jpa, Instant.now().plus(Duration.ofHours(1)));

		stubRefreshClaims(RAW_REFRESH);
		given(refreshTokenRepository.findByValue(REFRESH_VALUE)).willReturn(Optional.of(stored));
		given(userMapper.toDomain(jpa)).willReturn(user(USER_ID, EMAIL));
		stubAccessTokenAndCookies();
		given(jwtService.getRefreshExpiration()).willReturn(REFRESH_TTL_MS);
		given(jwtService.createToken(eq(USER_ID), eq(false), anyMap())).willReturn("new-refresh-token");
		given(refreshTokenMapper.toJpa(any(RefreshToken.class))).willReturn(new JRefreshToken());

		RefreshResult result = authService.refresh(RAW_REFRESH);

		assertThat(result.body().refreshToken()).isEqualTo("new-refresh-token");
		verify(refreshTokenRepository).delete(stored);
		verify(refreshTokenRepository).save(any(JRefreshToken.class));
	}

	@Test
	void refreshRejectsBlankToken() {
		assertThatThrownBy(() -> authService.refresh("  "))
				.isInstanceOf(InvalidRefreshTokenException.class);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	void refreshRejectsTokenFailingVerification() {
		given(jwtService.verifyToken(RAW_REFRESH)).willReturn(false);

		assertThatThrownBy(() -> authService.refresh(RAW_REFRESH))
				.isInstanceOf(InvalidRefreshTokenException.class);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	void refreshRejectsAccessToken() {
		given(jwtService.verifyToken(RAW_REFRESH)).willReturn(true);
		given(jwtService.extractClaim(RAW_REFRESH, AuthService.TOKEN_TYPE_CLAIM))
				.willReturn(AuthService.ACCESS_TOKEN_TYPE);

		assertThatThrownBy(() -> authService.refresh(RAW_REFRESH))
				.isInstanceOf(InvalidRefreshTokenException.class);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	void refreshRejectsUnknownTokenValue() {
		stubRefreshClaims(RAW_REFRESH);
		given(refreshTokenRepository.findByValue(REFRESH_VALUE)).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.refresh(RAW_REFRESH))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void refreshRejectsTokenForDifferentUser() {
		JUser otherUser = jUser(UUID.randomUUID(), "other@example.com");
		JRefreshToken stored = storedToken(otherUser, Instant.now().plus(Duration.ofDays(30)));

		stubRefreshClaims(RAW_REFRESH);
		given(refreshTokenRepository.findByValue(REFRESH_VALUE)).willReturn(Optional.of(stored));

		assertThatThrownBy(() -> authService.refresh(RAW_REFRESH))
				.isInstanceOf(InvalidRefreshTokenException.class);
		verify(refreshTokenRepository, never()).delete(any());
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void getCurrentUserReturnsMappedUser() {
		JUser jpa = jUser(USER_ID, EMAIL);
		User domain = user(USER_ID, EMAIL);

		given(userRepository.findById(USER_ID)).willReturn(Optional.of(jpa));
		given(userMapper.toDomain(jpa)).willReturn(domain);

		assertThat(authService.getCurrentUser(USER_ID)).isSameAs(domain);
	}

	@Test
	void getCurrentUserThrowsWhenMissing() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.getCurrentUser(USER_ID))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void deleteCurrentUserDeletesUserAndClearsCookies() {
		JUser jpa = jUser(USER_ID, EMAIL);
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(jpa));
		given(authCookieFactory.clearAuthCookies()).willReturn(List.of(cookie("access_token"), cookie("refresh_token")));

		List<ResponseCookie> cookies = authService.deleteCurrentUser(USER_ID);

		assertThat(cookies).hasSize(2);
		verify(userRepository).delete(jpa);
	}

	@Test
	void deleteCurrentUserThrowsWhenMissing() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.deleteCurrentUser(USER_ID))
				.isInstanceOf(UserNotFoundException.class);
		verify(userRepository, never()).delete(any());
	}

	@Test
	void logoutDeletesAllUserRefreshTokensAndClearsCookies() {
		given(authCookieFactory.clearAuthCookies())
				.willReturn(List.of(cookie("access_token"), cookie("refresh_token")));

		List<ResponseCookie> cookies = authService.logout(USER_ID, null);

		assertThat(cookies).hasSize(2);
		verify(refreshTokenRepository).deleteByUser_Id(USER_ID);
	}

	@Test
	void logoutFallsBackToRefreshTokenUserWhenNoPrincipal() {
		given(jwtService.verifyToken(RAW_REFRESH)).willReturn(true);
		given(jwtService.extractClaim(RAW_REFRESH, AuthService.TOKEN_TYPE_CLAIM))
				.willReturn(AuthService.REFRESH_TOKEN_TYPE);
		given(jwtService.extractSubject(RAW_REFRESH)).willReturn(USER_ID.toString());
		given(authCookieFactory.clearAuthCookies())
				.willReturn(List.of(cookie("access_token"), cookie("refresh_token")));

		List<ResponseCookie> cookies = authService.logout(null, RAW_REFRESH);

		assertThat(cookies).hasSize(2);
		verify(refreshTokenRepository).deleteByUser_Id(USER_ID);
	}

	@Test
	void logoutWithoutIdentityOnlyClearsCookies() {
		given(authCookieFactory.clearAuthCookies())
				.willReturn(List.of(cookie("access_token"), cookie("refresh_token")));

		List<ResponseCookie> cookies = authService.logout(null, null);

		assertThat(cookies).hasSize(2);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	void logoutIgnoresInvalidRefreshToken() {
		given(jwtService.verifyToken(RAW_REFRESH)).willReturn(false);
		given(authCookieFactory.clearAuthCookies())
				.willReturn(List.of(cookie("access_token"), cookie("refresh_token")));

		List<ResponseCookie> cookies = authService.logout(null, RAW_REFRESH);

		assertThat(cookies).hasSize(2);
		verifyNoInteractions(refreshTokenRepository);
	}

	private void stubIssuedTokens() {
		given(jwtService.getAccessExpiration()).willReturn(ACCESS_TTL_MS);
		given(jwtService.getRefreshExpiration()).willReturn(REFRESH_TTL_MS);
		given(jwtService.createToken(eq(USER_ID), eq(true), anyMap())).willReturn(ACCESS_TOKEN);
		given(jwtService.createToken(eq(USER_ID), eq(false), anyMap())).willReturn(REFRESH_TOKEN);
		given(refreshTokenMapper.toJpa(any(RefreshToken.class))).willReturn(new JRefreshToken());
		stubCookies();
	}

	private void stubAccessTokenAndCookies() {
		given(jwtService.getAccessExpiration()).willReturn(ACCESS_TTL_MS);
		given(jwtService.createToken(eq(USER_ID), eq(true), anyMap())).willReturn(ACCESS_TOKEN);
		stubCookies();
	}

	private void stubCookies() {
		given(authCookieFactory.accessToken(anyString(), any(Duration.class))).willReturn(cookie("access_token"));
		given(authCookieFactory.refreshToken(anyString(), any(Duration.class))).willReturn(cookie("refresh_token"));
	}

	private void stubRefreshClaims(String rawToken) {
		given(jwtService.verifyToken(rawToken)).willReturn(true);
		given(jwtService.extractClaim(rawToken, AuthService.TOKEN_TYPE_CLAIM))
				.willReturn(AuthService.REFRESH_TOKEN_TYPE);
		given(jwtService.extractSubject(rawToken)).willReturn(USER_ID.toString());
		given(jwtService.extractClaim(rawToken, AuthService.REFRESH_TOKEN_VALUE_CLAIM))
				.willReturn(REFRESH_VALUE);
	}

	private JRefreshToken storedToken(JUser owner, Instant expiresAt) {
		var token = new JRefreshToken();
		token.setUser(owner);
		token.setValue(REFRESH_VALUE);
		token.setExpiresAt(expiresAt);
		return token;
	}

	private JUser jUser(UUID id, String email) {
		var jpa = new JUser();
		jpa.setId(id);
		jpa.setEmail(email);
		jpa.setUsername("nick");
		jpa.setPasswordHash(PASSWORD_HASH);
		return jpa;
	}

	private User user(UUID id, String email) {
		var domain = new User();
		domain.setId(id);
		domain.setEmail(email);
		domain.setUsername("nick");
		return domain;
	}

	private static ResponseCookie cookie(String name) {
		return ResponseCookie.from(name, "value").build();
	}
}
