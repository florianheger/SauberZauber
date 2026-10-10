package de.sauberzauber.backend.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

	public record MeResponse(String username) {
	}

	@GetMapping("/me")
	public MeResponse me(Authentication authentication) {
		return new MeResponse(authentication.getName());
	}

}
