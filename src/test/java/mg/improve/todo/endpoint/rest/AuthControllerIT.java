package mg.improve.todo.endpoint.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mg.improve.todo.integration.AbstractControllerIT;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthControllerIT extends AbstractControllerIT {

	@Test
	void registerCreatesUserAndSetsAuthCookies() throws Exception {
		String email = uniqueEmail();

		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson(email, DEFAULT_USERNAME, DEFAULT_PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.username").value(DEFAULT_USERNAME))
				.andExpect(cookie().exists("access_token"))
				.andExpect(cookie().exists("refresh_token"))
				.andExpect(cookie().httpOnly("access_token", true));
	}

	@Test
	void registerRejectsDuplicateEmail() throws Exception {
		String email = uniqueEmail();
		register(email);

		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson(email, DEFAULT_USERNAME, DEFAULT_PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(409))
				.andExpect(jsonPath("$.event").doesNotExist());
	}

	@Test
	void registerRejectsInvalidPayload() throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"not-an-email\",\"username\":\"\",\"password\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.details").isArray());
	}

	@Test
	void loginReturnsCookiesForValidCredentials() throws Exception {
		String email = uniqueEmail();
		register(email);

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(email, DEFAULT_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(cookie().exists("access_token"))
				.andExpect(cookie().exists("refresh_token"));
	}

	@Test
	void loginRejectsWrongPassword() throws Exception {
		String email = uniqueEmail();
		register(email);

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(email, "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(401));
	}

	@Test
	void loginRejectsUnknownEmail() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(uniqueEmail(), DEFAULT_PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(401));
	}

	@Test
	void meReturnsCurrentUserWithAccessCookie() throws Exception {
		AuthSession session = register(uniqueEmail());

		mockMvc.perform(get("/auth/me").cookie(session.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(session.userId().toString()))
				.andExpect(jsonPath("$.username").value(DEFAULT_USERNAME));
	}

	@Test
	void meReturnsCurrentUserWithBearerToken() throws Exception {
		AuthSession session = register(uniqueEmail());

		mockMvc.perform(get("/auth/me").header("Authorization", session.bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(session.userId().toString()));
	}

	@Test
	void meRejectsUnauthenticatedRequest() throws Exception {
		mockMvc.perform(get("/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Missing or invalid credentials"));
	}

	@Test
	void refreshIssuesNewTokensFromRefreshCookie() throws Exception {
		AuthSession session = register(uniqueEmail());

		mockMvc.perform(post("/auth/refresh").cookie(session.refreshCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(cookie().exists("access_token"))
				.andExpect(cookie().exists("refresh_token"));
	}

	@Test
	void refreshRejectsMissingCookie() throws Exception {
		mockMvc.perform(post("/auth/refresh"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(401));
	}

	@Test
	void logoutClearsCookiesAndRevokesRefreshToken() throws Exception {
		AuthSession session = register(uniqueEmail());

		mockMvc.perform(post("/auth/logout")
						.cookie(session.accessCookie(), session.refreshCookie()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("access_token", 0))
				.andExpect(cookie().maxAge("refresh_token", 0));

		mockMvc.perform(post("/auth/refresh").cookie(session.refreshCookie()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesRefreshTokenWithoutAccessToken() throws Exception {
		AuthSession session = register(uniqueEmail());

		mockMvc.perform(post("/auth/logout").cookie(session.refreshCookie()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("refresh_token", 0));

		mockMvc.perform(post("/auth/refresh").cookie(session.refreshCookie()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutWithoutCredentialsIsIdempotent() throws Exception {
		mockMvc.perform(post("/auth/logout"))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("access_token", 0))
				.andExpect(cookie().maxAge("refresh_token", 0));
	}

	@Test
	void logoutRevokesAllSessionsOfUser() throws Exception {
		String email = uniqueEmail();
		AuthSession first = register(email);
		AuthSession second = login(email, DEFAULT_PASSWORD);

		mockMvc.perform(post("/auth/logout").cookie(first.accessCookie()))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/auth/refresh").cookie(first.refreshCookie()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/auth/refresh").cookie(second.refreshCookie()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deleteMeClearsCookiesAndRemovesUser() throws Exception {
		AuthSession session = register(uniqueEmail());

		mockMvc.perform(delete("/auth/me").cookie(session.accessCookie()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("access_token", 0))
				.andExpect(cookie().maxAge("refresh_token", 0));

		mockMvc.perform(get("/auth/me").cookie(session.accessCookie()))
				.andExpect(status().isUnauthorized());
	}
}
