package de.sauberzauber.backend.auth;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("app")
public record AppProperties(List<String> users, String rpId, String origin, String reenroll) {

	public AppProperties {
		users = (users == null) ? List.of()
				: users.stream().map(String::trim).filter(StringUtils::hasText).distinct().toList();
		if (users.size() != 2) {
			throw new IllegalArgumentException("APP_USERS must contain exactly two user names");
		}
		if (!StringUtils.hasText(rpId) || !StringUtils.hasText(origin)) {
			throw new IllegalArgumentException("APP_RP_ID and APP_ORIGIN must be set");
		}
		reenroll = StringUtils.hasText(reenroll) ? reenroll.trim() : null;
	}

}
