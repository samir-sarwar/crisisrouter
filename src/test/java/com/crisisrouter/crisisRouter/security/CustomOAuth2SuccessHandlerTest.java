package com.crisisrouter.crisisRouter.security;

import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.model.entity.UserRole;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2SuccessHandler handler;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;

    @Test
    void onAuthenticationSuccess_newUser_createsWithVolunteerRole() throws Exception {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("new@test.com");
        when(oidcUser.getFullName()).thenReturn("John Doe");
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@test.com");
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getRole()).isEqualTo(UserRole.VOLUNTEER);
    }

    @Test
    void onAuthenticationSuccess_existingUser_updatesName() throws Exception {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("existing@test.com");
        when(oidcUser.getFullName()).thenReturn("Updated Name");
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        User existingUser = TestDataFactory.createUser("existing@test.com", "Old", "Name");
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).save(existingUser);
        assertThat(existingUser.getFirstName()).isEqualTo("Updated");
        assertThat(existingUser.getLastName()).isEqualTo("Name");
    }

    @Test
    void onAuthenticationSuccess_singleName_firstNameOnlyLastNameEmpty() throws Exception {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("madonna@test.com");
        when(oidcUser.getFullName()).thenReturn("Madonna");
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(userRepository.findByEmail("madonna@test.com")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Madonna");
        assertThat(saved.getLastName()).isEmpty();
    }

    @Test
    void onAuthenticationSuccess_nullName_defaultsToUser() throws Exception {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("noname@test.com");
        when(oidcUser.getFullName()).thenReturn(null);
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(userRepository.findByEmail("noname@test.com")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("User");
        assertThat(saved.getLastName()).isEmpty();
    }

    @Test
    void onAuthenticationSuccess_multiPartName_splitsAtLastSpace() throws Exception {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("mj@test.com");
        when(oidcUser.getFullName()).thenReturn("Mary Jane Watson");
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(userRepository.findByEmail("mj@test.com")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Mary Jane");
        assertThat(saved.getLastName()).isEqualTo("Watson");
    }

    @Test
    void onAuthenticationSuccess_nonOidcPrincipal_skipsUserSync() throws Exception {
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }
}
