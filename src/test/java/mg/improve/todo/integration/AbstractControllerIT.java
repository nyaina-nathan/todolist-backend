package mg.improve.todo.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractControllerIT extends ConfIT {

	protected static final String DEFAULT_USERNAME = "nick";

	protected static final String DEFAULT_PASSWORD = "password123";

	@Autowired
	protected MockMvc mockMvc;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	protected String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

	protected AuthSession register(String email) throws Exception {
		return register(email, DEFAULT_USERNAME);
	}

	protected AuthSession register(String email, String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson(email, username, DEFAULT_PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn();
		return sessionFrom(result);
	}

	protected AuthSession login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(email, password)))
				.andExpect(status().isOk())
				.andReturn();
		return sessionFrom(result);
	}

	protected AuthSession sessionFrom(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		UUID userId = UUID.fromString(JsonPath.read(body, "$.id"));
		String accessToken = cookieValue(result, "access_token");
		String refreshToken = cookieValue(result, "refresh_token");
		return new AuthSession(userId, accessToken, refreshToken);
	}

	protected static String cookieValue(MvcResult result, String name) {
		Cookie cookie = result.getResponse().getCookie(name);
		if (cookie == null) {
			throw new IllegalStateException("Missing cookie: " + name);
		}
		return cookie.getValue();
	}

	protected static String registerJson(String email, String username, String password) {
		return "{\"email\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}"
				.formatted(email, username, password);
	}

	protected static String loginJson(String email, String password) {
		return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
	}

	protected record AuthSession(UUID userId, String accessToken, String refreshToken) {

		public Cookie accessCookie() {
			return new Cookie("access_token", accessToken);
		}

		public Cookie refreshCookie() {
			return new Cookie("refresh_token", refreshToken);
		}

		public String bearer() {
			return "Bearer " + accessToken;
		}
	}
}
