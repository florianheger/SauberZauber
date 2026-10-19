package de.sauberzauber.backend.push;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** VAPID key pair (base64url, uncompressed P-256 point and raw private scalar) and contact for Web Push. */
@ConfigurationProperties("app.push")
public record PushProperties(String publicKey, String privateKey, String subject) {

	public PushProperties {
		publicKey = StringUtils.hasText(publicKey) ? publicKey.trim() : null;
		privateKey = StringUtils.hasText(privateKey) ? privateKey.trim() : null;
		subject = StringUtils.hasText(subject) ? subject.trim() : null;
		if ((publicKey == null) != (privateKey == null)) {
			throw new IllegalArgumentException("APP_VAPID_PUBLIC_KEY and APP_VAPID_PRIVATE_KEY must be set together");
		}
		// Apple's push service rejects VAPID tokens without a real mailto: or https: contact
		if (publicKey != null
				&& (subject == null || !(subject.startsWith("mailto:") || subject.startsWith("https:")))) {
			throw new IllegalArgumentException("APP_VAPID_SUBJECT must be a mailto: or https: URL");
		}
	}

	public boolean enabled() {
		return this.publicKey != null;
	}

}
