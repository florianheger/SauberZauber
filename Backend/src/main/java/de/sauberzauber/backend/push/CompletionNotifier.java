package de.sauberzauber.backend.push;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import de.sauberzauber.backend.auth.AppProperties;
import de.sauberzauber.backend.push.PushSubscriptionRepository.Subscription;
import de.sauberzauber.backend.user.UserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;

/** Tells the other household member when a task was marked done. */
@Component
public class CompletionNotifier {

	private static final Logger logger = LoggerFactory.getLogger(CompletionNotifier.class);

	private final AppProperties app;

	private final PushProperties push;

	private final PushSubscriptionRepository subscriptions;

	private final PushSender sender;

	private final JsonMapper json;

	public CompletionNotifier(AppProperties app, PushProperties push, PushSubscriptionRepository subscriptions,
			PushSender sender, JsonMapper json) {
		this.app = app;
		this.push = push;
		this.subscriptions = subscriptions;
		this.sender = sender;
		this.json = json;
	}

	/**
	 * Notifies the user who did not press the button: {@code actor} marked {@code taskTitle} done for
	 * {@code doer} (themself or the other user). Sending happens in the background and never fails the caller.
	 */
	public void completionAdded(String actor, String doer, String taskTitle, long completionId) {
		if (!this.push.enabled()) {
			return;
		}
		try {
			String recipient = this.app.users().stream().filter((user) -> !user.equals(actor)).findFirst().orElse(null);
			if (recipient == null) {
				return;
			}
			String name = UserController.displayName(actor);
			String text = doer.equals(actor) ? name + " hat " + taskTitle + " erledigt"
					: name + " hat " + taskTitle + " für dich eingetragen";
			String payload = payload(text, completionId);
			for (Subscription subscription : this.subscriptions.findByUsername(recipient)) {
				this.sender.send(subscription, payload)
					.whenComplete((status, ex) -> handleResult(subscription, status, ex));
			}
		}
		catch (RuntimeException ex) {
			logger.warn("Could not send push notification", ex);
		}
	}

	private void handleResult(Subscription subscription, Integer status, Throwable ex) {
		if (ex != null) {
			logger.warn("Push to {} failed: {}", host(subscription), ex.toString());
		}
		else if (status == 404 || status == 410) {
			// the browser unsubscribed or the app was uninstalled
			this.subscriptions.deleteByEndpoint(subscription.endpoint());
		}
		else if (status < 200 || status >= 300) {
			logger.warn("Push to {} was rejected with HTTP {}", host(subscription), status);
		}
	}

	/**
	 * Angular's service worker shows {@code notification} and handles the click via {@code data.onActionClick}.
	 * {@code web_push} and {@code navigate} make it a Declarative Web Push message, which Safari 18.4+ can show
	 * even without the service worker; other browsers ignore them.
	 */
	private String payload(String text, long completionId) {
		Map<String, Object> notification = new LinkedHashMap<>();
		notification.put("title", text);
		notification.put("lang", "de");
		notification.put("icon", "/icons/icon-192.png");
		notification.put("tag", "completion-" + completionId);
		notification.put("navigate", this.app.origin() + "/");
		notification.put("data", Map.of("onActionClick",
				Map.of("default", Map.of("operation", "navigateLastFocusedOrOpen", "url", "/"))));
		return this.json.writeValueAsString(Map.of("web_push", 8030, "notification", notification));
	}

	private static String host(Subscription subscription) {
		return URI.create(subscription.endpoint()).getHost();
	}

}
