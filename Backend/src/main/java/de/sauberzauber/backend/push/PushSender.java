package de.sauberzauber.backend.push;

import java.util.concurrent.CompletableFuture;

import de.sauberzauber.backend.push.PushSubscriptionRepository.Subscription;

/** Delivers a message to a browser's push service. */
public interface PushSender {

	/** Sends {@code payload} (JSON) and completes with the push service's HTTP status code. */
	CompletableFuture<Integer> send(Subscription subscription, String payload);

}
