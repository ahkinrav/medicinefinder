
package com.medfinder.web;

import com.medfinder.model.AppUser;
import com.medfinder.repo.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthController(AppUserRepository users) {
        this.users = users;
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @Size(max = 30) String phone,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerCustomer(
            @Valid @RequestBody RegisterRequest request) {

        return registerUser(request, AppUser.Role.CUSTOMER);
    }

    @PostMapping("/register-shop-owner")
    public ResponseEntity<Map<String, String>> registerShopOwner(
            @Valid @RequestBody RegisterRequest request) {

        return registerUser(request, AppUser.Role.SHOP_OWNER);
    }

    private ResponseEntity<Map<String, String>> registerUser(
            RegisterRequest request, AppUser.Role role) {

        String email = request.email().trim();

        if (users.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "This email is already registered. Please log in."
                    ));
        }

        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setRole(role);

        users.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Registration successful. Please log in.",
                        "redirectUrl", role == AppUser.Role.CUSTOMER
                                ? "/login.html"
                                : "/shop-owner-login.html"
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestParam(defaultValue = "CUSTOMER") String role) {

        AppUser.Role requestedRole;

        try {
            requestedRole = AppUser.Role.valueOf(
                    role.trim().toUpperCase()
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid account type."));
        }

        return users.findByEmailIgnoreCase(request.email().trim())
                .filter(user -> user.getRole() == requestedRole)
                .filter(user -> passwordEncoder.matches(
                        request.password(), user.getPasswordHash()))
                .map(user -> ResponseEntity.ok(Map.of(
                        "message", "Login successful.",
                        "role", user.getRole().name(),
                        "redirectUrl", "/index.html"
                )))
                .orElseGet(() -> ResponseEntity.status(
                                HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "message",
                                "Invalid email, password, or account type."
                        )));
    }
}
