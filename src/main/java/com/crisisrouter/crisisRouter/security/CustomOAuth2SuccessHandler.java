package com.crisisrouter.crisisRouter.security;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.crisisrouter.crisisRouter.model.entity.UserRole;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Object principal = authentication.getPrincipal();

        // 1. Check if the login is via OIDC (Google/Auth0)
        if (principal instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            String fullName = oidcUser.getFullName();

            // 2. Prepare Name Splitting Logic
            String firstName;
            String lastName;

            if (fullName != null && fullName.contains(" ")) {
                int lastSpaceIndex = fullName.lastIndexOf(" ");
                firstName = fullName.substring(0, lastSpaceIndex);
                lastName = fullName.substring(lastSpaceIndex + 1);
            } else {
                firstName = (fullName != null) ? fullName : "User";
                lastName = "";
            }

            // 3. Sync User with Database
            String finalFirstName = firstName;
            String finalLastName = lastName;

            userRepository.findByEmail(email).ifPresentOrElse(
                    existingUser -> {
                        // Update existing user's name if they changed it in Auth0
                        // (Optional: remove this if you want users to manage names manually)
                        existingUser.setFirstName(finalFirstName);
                        existingUser.setLastName(finalLastName);
                        userRepository.save(existingUser);
                    },
                    () -> {
                        // Create new user if they don't exist
                        User newUser = User.builder()
                                .email(email)
                                .firstName(finalFirstName)
                                .lastName(finalLastName)
                                .role(UserRole.VOLUNTEER) // Default role
                                .build();
                        userRepository.save(newUser);
                    }
            );
        }

        // 4. Redirect to Frontend (Vite)
        // Ensure this matches your frontend URL exactly
        getRedirectStrategy().sendRedirect(request, response, "http://localhost:5173/");
    }
}