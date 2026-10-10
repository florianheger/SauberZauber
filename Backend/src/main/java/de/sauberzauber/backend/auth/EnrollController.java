package de.sauberzauber.backend.auth;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Turns a valid one-time enrollment token into an authenticated session and sends the
 * user to Spring Security's passkey registration page.
 */
@Controller
public class EnrollController {

	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
		.getContextHolderStrategy();

	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	private final EnrollmentService enrollment;

	private final UserDetailsService userDetailsService;

	public EnrollController(EnrollmentService enrollment, UserDetailsService userDetailsService) {
		this.enrollment = enrollment;
		this.userDetailsService = userDetailsService;
	}

	@GetMapping("/enroll")
	public void enroll(@RequestParam(required = false) String token, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		Optional<String> username = this.enrollment.consume(token);
		if (username.isEmpty()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		UserDetails user = this.userDetailsService.loadUserByUsername(username.get());
		SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
		this.securityContextHolderStrategy.setContext(context);
		request.getSession(true);
		request.changeSessionId();
		this.securityContextRepository.saveContext(context, request, response);
		response.sendRedirect("/webauthn/register");
	}

}
