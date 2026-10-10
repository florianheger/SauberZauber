package de.sauberzauber.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {

	public record HelloResponse(String message) {
	}

	@GetMapping("/hello")
	public HelloResponse hello() {
		return new HelloResponse("Hallo von SauberZauber!");
	}

}
