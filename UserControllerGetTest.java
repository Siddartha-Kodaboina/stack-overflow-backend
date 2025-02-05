package com.example.stackoverflow_auth_service.controller.user;


import com.example.stackoverflow_auth_service.base.BaseControllerTest;
import com.example.stackoverflow_auth_service.model.User;
import com.example.stackoverflow_auth_service.model.UserRole;
import com.example.stackoverflow_auth_service.util.TestConstants;
import com.example.stackoverflow_auth_service.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ActiveProfiles("test")
@DisplayName("User Get Operations")
class UserControllerGetTest extends BaseControllerTest {

    private User adminUser;
    private User regularUser;
    private User regularUser2;
    private User moderatorUser;
    private String adminToken;
    private String regularUserToken;
    private String regularUser2Token;
    private String moderatorToken;

    @BeforeEach
    void setUp() throws Exception {
        log.info("Setting up test data");

        // Clean database
        userRepository.deleteAll();
        log.info("Database cleaned");

        // Create admin user
        adminUser = userRepository.save(TestDataBuilder.createTestUser(
                TestConstants.ADMIN_EMAIL,
                TestConstants.ADMIN_UID,
                UserRole.ADMIN
        ));
        log.info("Admin user created with ID: {}", adminUser.getId());

        // Create regular user
        regularUser = userRepository.save(TestDataBuilder.createTestUser(
                TestConstants.USER_EMAIL,
                TestConstants.USER_UID,
                UserRole.USER
        ));
        log.info("Regular user created with ID: {}", regularUser.getId());

        moderatorUser = userRepository.save(TestDataBuilder.createTestUser(
                TestConstants.MODERATOR_EMAIL,
                TestConstants.MODERATOR_UID,
                UserRole.MODERATOR
        ));
        log.info("Moderator user created with ID: {}", moderatorUser.getId());

        regularUser2 = userRepository.save(TestDataBuilder.createTestUser(
                TestConstants.USER2_EMAIL,
                TestConstants.USER2_UID,
                UserRole.USER
        ));

        // Get tokens
        adminToken = getAuthToken(TestConstants.ADMIN_EMAIL, TestConstants.ADMIN_PASSWORD);
        moderatorToken = getAuthToken(TestConstants.MODERATOR_EMAIL, TestConstants.MODERATOR_PASSWORD);
        regularUserToken = getAuthToken(TestConstants.USER_EMAIL, TestConstants.USER_PASSWORD);
        regularUser2Token = getAuthToken(TestConstants.USER2_EMAIL, TestConstants.USER2_PASSWORD);

        // Log database state
        log.info("Test data setup complete. Database state:");
        userRepository.findAll().forEach(user ->
                log.info("User: ID={}, Email={}, Role={}",
                        user.getId(), user.getEmail(), user.getRole()));
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Admin should successfully retrieve a user")
        void adminShouldRetrieveUser() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + regularUser.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(regularUser.getId()))
                    .andExpect(jsonPath("$.email").value(regularUser.getEmail()))
                    .andExpect(jsonPath("$.username").value(regularUser.getUsername()))
                    .andExpect(jsonPath("$.role").value(regularUser.getRole().toString()));
        }

        @Test
        @DisplayName("Regular user should retrieve their own details")
        void userShouldRetrieveOwnDetails() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + regularUser.getId())
                            .header("Authorization", "Bearer " + regularUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(regularUser.getId()))
                    .andExpect(jsonPath("$.email").value(regularUser.getEmail()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void shouldReturn404ForNonExistentUser() throws Exception {
            // Given
            Long nonExistentId = TestConstants.NON_EXISTENT_USER_ID;

            // When & Then
            mockMvc.perform(get("/api/v1/users/" + nonExistentId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("User not found"))
                    .andExpect(jsonPath("$.path").value("/api/v1/users/" + nonExistentId))
                    .andExpect(jsonPath("$.timestamp").exists());

            // Verify logs
            log.info("Verified 404 response for non-existent user ID: {}", nonExistentId);
        }

        @Test
        @DisplayName("Should return 401 when no authentication provided")
        void shouldReturn401WhenNoAuth() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + regularUser.getId()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
        }

        @Test
        @DisplayName("Regular user should be able to view another user's profile")
        void regularUserShouldViewOtherUserProfile() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + regularUser2.getId())
                            .header("Authorization", "Bearer " + regularUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(regularUser2.getId()))
                    .andExpect(jsonPath("$.email").value(regularUser2.getEmail()))
                    .andExpect(jsonPath("$.username").value(regularUser2.getUsername()))
                    .andExpect(jsonPath("$.role").value("USER"));

            log.info("Regular user successfully viewed another user's profile");
        }

        @Test
        @DisplayName("Moderator should successfully access user profile")
        void moderatorShouldAccessUserProfile() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + regularUser.getId())
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(regularUser.getId()))
                    .andExpect(jsonPath("$.email").value(regularUser.getEmail()))
                    .andExpect(jsonPath("$.username").value(regularUser.getUsername()))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.createdAt").exists());

            log.info("Moderator successfully accessed user profile");
        }

        @Test
        @DisplayName("Admin should access complete user profile")
        void adminShouldAccessCompleteProfile() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + regularUser.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(regularUser.getId()))
                    .andExpect(jsonPath("$.email").value(regularUser.getEmail()))
                    .andExpect(jsonPath("$.username").value(regularUser.getUsername()))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.createdAt").exists());

            log.info("Admin successfully accessed complete user profile");
        }

        @Test
        @DisplayName("Should return consistent response format for all roles")
        void shouldReturnConsistentResponseFormat() throws Exception {
            // Test with regular user token
            mockMvc.perform(get("/api/v1/users/" + regularUser2.getId())
                            .header("Authorization", "Bearer " + regularUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.username").exists())
                    .andExpect(jsonPath("$.role").exists())
                    .andExpect(jsonPath("$.createdAt").exists());

            // Test with moderator token
            mockMvc.perform(get("/api/v1/users/" + regularUser2.getId())
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.username").exists())
                    .andExpect(jsonPath("$.role").exists())
                    .andExpect(jsonPath("$.createdAt").exists());

            // Test with admin token
            mockMvc.perform(get("/api/v1/users/" + regularUser2.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.username").exists())
                    .andExpect(jsonPath("$.role").exists())
                    .andExpect(jsonPath("$.createdAt").exists());

            log.info("Verified consistent response format across all roles");
        }
    }
}
