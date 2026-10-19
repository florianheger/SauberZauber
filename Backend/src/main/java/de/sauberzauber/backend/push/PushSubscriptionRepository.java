package de.sauberzauber.backend.push;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PushSubscriptionRepository {

	/** A browser's push subscription: endpoint URL plus its base64url encryption keys. */
	public record Subscription(String endpoint, String p256dh, String auth) {
	}

	private final JdbcClient jdbc;

	public PushSubscriptionRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Stores the subscription for the user; a device that was subscribed for the other user moves over. */
	public boolean save(String username, Subscription subscription) {
		return this.jdbc.sql("""
				insert into push_subscription (user_id, endpoint, p256dh, auth)
				select id, ?, ?, ? from app_user where username = ?
				on conflict (endpoint) do update
				set user_id = excluded.user_id, p256dh = excluded.p256dh, auth = excluded.auth
				""")
			.params(subscription.endpoint(), subscription.p256dh(), subscription.auth(), username)
			.update() == 1;
	}

	public void delete(String username, String endpoint) {
		this.jdbc.sql("""
				delete from push_subscription
				where endpoint = ? and user_id = (select id from app_user where username = ?)
				""")
			.params(endpoint, username)
			.update();
	}

	public void deleteByEndpoint(String endpoint) {
		this.jdbc.sql("delete from push_subscription where endpoint = ?").param(endpoint).update();
	}

	public List<Subscription> findByUsername(String username) {
		return this.jdbc.sql("""
				select s.endpoint, s.p256dh, s.auth from push_subscription s
				join app_user u on u.id = s.user_id where u.username = ?
				""")
			.param(username)
			.query(Subscription.class)
			.list();
	}

}
