package de.sauberzauber.backend.push;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import de.sauberzauber.backend.push.PushSubscriptionRepository.Subscription;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Web Push over HTTP (RFC 8030) with VAPID authentication (RFC 8292). */
@Component
public class WebPushSender implements PushSender {

	/** How long the push service keeps the message for an offline device. */
	static final Duration TTL = Duration.ofDays(1);

	private static final Duration TOKEN_LIFETIME = Duration.ofHours(12);

	private final PushProperties props;

	private final ECPrivateKey privateKey;

	private final Clock clock;

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	@Autowired
	public WebPushSender(PushProperties props) throws GeneralSecurityException {
		this(props, Clock.systemUTC());
	}

	WebPushSender(PushProperties props, Clock clock) throws GeneralSecurityException {
		this.props = props;
		this.clock = clock;
		if (props.enabled()) {
			this.privateKey = WebPushCrypto.privateKey(WebPushCrypto.decode(props.privateKey()));
			verifyKeyPair(WebPushCrypto.publicKey(WebPushCrypto.decode(props.publicKey())));
		}
		else {
			this.privateKey = null;
		}
	}

	@Override
	public CompletableFuture<Integer> send(Subscription subscription, String payload) {
		HttpRequest request;
		try {
			byte[] body = WebPushCrypto.encrypt(payload.getBytes(StandardCharsets.UTF_8), subscription.p256dh(),
					subscription.auth());
			URI endpoint = URI.create(subscription.endpoint());
			request = HttpRequest.newBuilder(endpoint)
				.timeout(Duration.ofSeconds(30))
				.header("TTL", Long.toString(TTL.toSeconds()))
				.header("Urgency", "normal")
				.header("Content-Encoding", "aes128gcm")
				.header("Content-Type", "application/octet-stream")
				.header("Authorization", "vapid t=" + vapidToken(endpoint) + ", k=" + this.props.publicKey())
				.POST(HttpRequest.BodyPublishers.ofByteArray(body))
				.build();
		}
		catch (GeneralSecurityException | RuntimeException ex) {
			return CompletableFuture.failedFuture(ex);
		}
		return this.http.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenApply(HttpResponse::statusCode);
	}

	/** Signed JWT for the push service's origin, as required by VAPID. */
	String vapidToken(URI endpoint) throws GeneralSecurityException {
		String audience = endpoint.getScheme() + "://" + endpoint.getRawAuthority();
		long expiry = this.clock.instant().plus(TOKEN_LIFETIME).getEpochSecond();
		String header = base64Json("{\"typ\":\"JWT\",\"alg\":\"ES256\"}");
		String claims = base64Json("{\"aud\":\"" + audience + "\",\"exp\":" + expiry + ",\"sub\":\""
				+ this.props.subject() + "\"}");
		String unsigned = header + "." + claims;
		return unsigned + "." + WebPushCrypto.encode(sign(unsigned.getBytes(StandardCharsets.US_ASCII)));
	}

	private byte[] sign(byte[] data) throws GeneralSecurityException {
		// ES256 in JWS needs the raw r || s form, not DER
		Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
		signature.initSign(this.privateKey);
		signature.update(data);
		return signature.sign();
	}

	/** Fails startup if the configured public key does not belong to the private key. */
	private void verifyKeyPair(ECPublicKey publicKey) throws GeneralSecurityException {
		byte[] probe = "vapid".getBytes(StandardCharsets.US_ASCII);
		Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
		verifier.initVerify(publicKey);
		verifier.update(probe);
		if (!verifier.verify(sign(probe))) {
			throw new IllegalArgumentException("APP_VAPID_PUBLIC_KEY does not match APP_VAPID_PRIVATE_KEY");
		}
	}

	private static String base64Json(String json) {
		return WebPushCrypto.encode(json.getBytes(StandardCharsets.UTF_8));
	}

}
