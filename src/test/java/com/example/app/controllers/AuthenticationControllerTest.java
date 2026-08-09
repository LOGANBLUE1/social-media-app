package com.example.app.controllers;

import com.example.app.dataAccess.RefreshTokenRepository;
import com.example.app.dataAccess.UserRepository;
import com.example.app.requests.UserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the login flow end to end -- security filter chain, controller, AuthService,
 * BCrypt and the database.
 *
 * Cleanup: @Transactional makes the framework roll the test transaction back once the test
 * method returns, so the rows written by /auth/signup never commit. @BeforeTransaction and
 * @AfterTransaction run outside that transaction, which is what lets them assert the
 * committed state is byte-for-byte identical before and after.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationControllerTest {

    private static final String USERNAME = "auth-flow-test-user";
    private static final String PASSWORD = "correct-horse-battery-staple";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private long usersBefore;
    private long refreshTokensBefore;

    @BeforeTransaction
    void captureCommittedState() {
        usersBefore = userRepository.count();
        refreshTokensBefore = refreshTokenRepository.count();
        // a leftover from a previously aborted run would make the signup 409 instead of 201
        assertThat(userRepository.findByUsername(USERNAME))
                .as("test user must not exist before the test")
                .isNull();
    }

    @AfterTransaction
    void verifyNothingWasLeftBehind() {
        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(refreshTokenRepository.count()).isEqualTo(refreshTokensBefore);
        assertThat(userRepository.findByUsername(USERNAME)).isNull();
    }

    @Test
    @DisplayName("correct password -> 200 with tokens in the envelope and no password echoed back")
    void loginSucceedsWithCorrectPassword() throws Exception {
        signup();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value(USERNAME))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    @DisplayName("wrong password -> 401 with the generic message and no data")
    void loginFailsWithWrongPassword() throws Exception {
        signup();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(USERNAME, "not-the-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                // BadCredentialsException("Bad credentials") is deliberately not echoed --
                // the message must not reveal which half of the pair was wrong
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private void signup() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(USERNAME, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private String credentials(String username, String password) throws Exception {
        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setPassword(password);
        return objectMapper.writeValueAsString(request);
    }
}
