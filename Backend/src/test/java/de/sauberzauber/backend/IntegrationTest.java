package de.sauberzauber.backend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/** Full application context against a Postgres Testcontainer, with users alex and sam and push enabled. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = { "app.users=alex,sam", "app.rp-id=localhost", "app.origin=http://localhost:8080",
		"app.reenroll=", "app.push.public-key=" + IntegrationTest.VAPID_PUBLIC_KEY,
		"app.push.private-key=" + IntegrationTest.VAPID_PRIVATE_KEY, "app.push.subject=mailto:test@example.com" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {

	/** Test-only VAPID key pair, so push notifications are enabled. */
	String VAPID_PUBLIC_KEY = "BFTZi0y61mbah-Q5GUP3v05UO7rHv52o9C6l275PBoDMYBfcq0WyALpaBsTgYm6hxQpk5320vPq0AIw4s4xiVls";

	String VAPID_PRIVATE_KEY = "yxyaeOvs0lTz-gGvIDLvtXuZZCfuosGR54Ge2MLw3pY";

}
