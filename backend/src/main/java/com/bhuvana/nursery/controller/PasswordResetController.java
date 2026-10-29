package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.service.ClientIpResolver;
import com.bhuvana.nursery.service.InvalidResetTokenException;
import com.bhuvana.nursery.service.PasswordResetRateLimitedException;
import com.bhuvana.nursery.service.PasswordResetService;
import com.bhuvana.nursery.service.PasswordResetUnavailableException;
import com.bhuvana.nursery.service.WeakPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public password reset endpoints.
 *
 * <p>Responses are deliberately generic: "forgot" always reports the same
 * outcome whether or not the address belongs to an administrator, which
 * prevents account enumeration. Tokens and hashes never appear in any
 * response.</p>
 */
@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordResetController {

    static final String FORGOT_GENERIC_MESSAGE =
            "If an administrator account exists for that email, a password reset " +
            "link has been sent. Check the inbox and spam folder.";

    static final String RATE_LIMITED_MESSAGE =
            "Too many requests. Please wait a few minutes and try again.";

    static final String UNAVAILABLE_MESSAGE =
            "Password reset is temporarily unavailable. Please try again later.";

    static final String INVALID_TOKEN_MESSAGE =
            "This reset link is invalid or has expired. Please request a new one.";

    private final PasswordResetService passwordResetService;
    private final ClientIpResolver clientIpResolver;

    public PasswordResetController(
            PasswordResetService passwordResetService,
            ClientIpResolver clientIpResolver
    ) {
        this.passwordResetService = passwordResetService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> forgot(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        try {
            passwordResetService.requestReset(
                    request.email(),
                    clientIpResolver.resolve(servletRequest)
            );
        } catch (PasswordResetRateLimitedException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", RATE_LIMITED_MESSAGE));
        } catch (PasswordResetUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", UNAVAILABLE_MESSAGE));
        }

        return ResponseEntity.accepted()
                .body(Map.of("message", FORGOT_GENERIC_MESSAGE));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(request.token(), request.password());

        return ResponseEntity.ok(Map.of(
                "message", "Password updated. You can now sign in."
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = "Please provide a valid email address.";

        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null && "password".equals(fieldError.getField())) {
            message = com.bhuvana.nursery.service.PasswordPolicy.RULES_MESSAGE;
        }

        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken() {
        return ResponseEntity.badRequest()
                .body(Map.of("message", INVALID_TOKEN_MESSAGE));
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<Map<String, String>> handleWeakPassword(
            WeakPasswordException exception
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", exception.getMessage()));
    }

    public record ForgotPasswordRequest(
            @NotBlank
            @Email
            @Size(max = 320)
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank
            @Size(max = 100)
            String token,
            @NotBlank
            @Size(max = 200)
            String password
    ) {}
}
