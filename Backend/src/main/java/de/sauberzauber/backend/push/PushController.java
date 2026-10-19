package de.sauberzauber.backend.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Registers the browsers that receive notifications for the signed-in user. */
@RestController
@RequestMapping("/api/push")
public class PushController {

	public record PublicKeyResponse(String publicKey) {
	}

	/** The JSON form of a browser {@code PushSubscription} ({@code subscription.toJSON()}). */
	public record SubscriptionRequest(@NotBlank @Pattern(regexp = "https://.+") String endpoint,
			@NotNull @Valid Keys keys) {

		public record Keys(@NotBlank String p256dh, @NotBlank String auth) {
		}

	}

	public record UnsubscribeRequest(@NotBlank String endpoint) {
	}

	private final PushProperties props;

	private final PushSubscriptionRepository subscriptions;

	public PushController(PushProperties props, PushSubscriptionRepository subscriptions) {
		this.props = props;
		this.subscriptions = subscriptions;
	}

	@GetMapping("/public-key")
	public PublicKeyResponse publicKey() {
		if (!this.props.enabled()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Push notifications are disabled");
		}
		return new PublicKeyResponse(this.props.publicKey());
	}

	@PostMapping("/subscriptions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void subscribe(Authentication authentication, @Valid @RequestBody SubscriptionRequest request) {
		this.subscriptions.save(authentication.getName(), new PushSubscriptionRepository.Subscription(
				request.endpoint(), request.keys().p256dh(), request.keys().auth()));
	}

	@DeleteMapping("/subscriptions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribe(Authentication authentication, @Valid @RequestBody UnsubscribeRequest request) {
		this.subscriptions.delete(authentication.getName(), request.endpoint());
	}

}
