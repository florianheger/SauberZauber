package de.sauberzauber.backend.push;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import de.sauberzauber.backend.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WebPushSenderTest {

	private static final PushProperties PROPS = new PushProperties(IntegrationTest.VAPID_PUBLIC_KEY,
			IntegrationTest.VAPID_PRIVATE_KEY, "mailto:test@example.com");

	@Test
	void vapidTokenIsSignedForThePushServiceOrigin() throws Exception {
		Clock clock = Clock.fixed(Instant.ofEpochSecond(1_000_000), ZoneOffset.UTC);
		String token = new WebPushSender(PROPS, clock)
			.vapidToken(URI.create("https://web.push.apple.com/QGuQyavXutnMNfJd7Fs"));

		String[] parts = token.split("\\.");
		assertThat(new String(WebPushCrypto.decode(parts[0]), StandardCharsets.UTF_8))
			.isEqualTo("{\"typ\":\"JWT\",\"alg\":\"ES256\"}");
		assertThat(new String(WebPushCrypto.decode(parts[1]), StandardCharsets.UTF_8)).isEqualTo(
				"{\"aud\":\"https://web.push.apple.com\",\"exp\":1043200,\"sub\":\"mailto:test@example.com\"}");
		Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
		verifier.initVerify(WebPushCrypto.publicKey(WebPushCrypto.decode(IntegrationTest.VAPID_PUBLIC_KEY)));
		verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
		assertThat(verifier.verify(WebPushCrypto.decode(parts[2]))).isTrue();
	}

	@Test
	void mismatchedKeyPairIsRejected() {
		PushProperties props = new PushProperties(
				"BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8",
				IntegrationTest.VAPID_PRIVATE_KEY, "mailto:test@example.com");

		assertThatIllegalArgumentException().isThrownBy(() -> new WebPushSender(props));
	}

	@Test
	void subjectMustBeMailtoOrHttps() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PushProperties(IntegrationTest.VAPID_PUBLIC_KEY,
				IntegrationTest.VAPID_PRIVATE_KEY, "test@example.com"));
		assertThat(new PushProperties(null, null, null).enabled()).isFalse();
	}

}
