package mg.improve.todo.domain.mappers;

import lombok.RequiredArgsConstructor;
import mg.improve.todo.domain.entity.RefreshToken;
import mg.improve.todo.repository.entity.JRefreshToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenMapper {

	private final UserMapper userMapper;

	public RefreshToken toDomain(JRefreshToken jpa) {
		if (jpa == null) {
			return null;
		}
		var domain = new RefreshToken();
		domain.setId(jpa.getId());
		domain.setUser(userMapper.toReference(jpa.getUser()));
		domain.setValue(jpa.getValue());
		domain.setIssuedAt(jpa.getIssuedAt());
		domain.setExpiresAt(jpa.getExpiresAt());
		return domain;
	}

	public JRefreshToken toJpa(RefreshToken domain) {
		if (domain == null) {
			return null;
		}
		var jpa = new JRefreshToken();
		jpa.setId(domain.getId());
		jpa.setUser(userMapper.toReference(domain.getUser()));
		jpa.setValue(domain.getValue());
		jpa.setIssuedAt(domain.getIssuedAt());
		jpa.setExpiresAt(domain.getExpiresAt());
		return jpa;
	}
}
