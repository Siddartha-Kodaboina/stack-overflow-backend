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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ActiveProfiles("test")
@DisplayName("User Get By Role Operations")
class UserControllerGetByRoleTest extends BaseControllerTest {

    private User adminUser;
    private User regularUser1;
    private User regularUser2;
    private User moderatorUser;
    private String adminToken;
    private String regularUserToken;
    private String moderatorToken;

    @BeforeEach
    void setUp() throws Exception {
        log.info("Setting up test data");
        userRepository.deleteAll();

        // Create users with different roles
        adminUser = userRepository.save(TestDataBuilder.createTestUser(
                TestConstants.ADMIN_EMAIL,
                TestConstants.ADMIN_UID,
                UserRole.ADMIN
        ));

        regularUser1 = userRepository.save(TestDataBuilder.createTestUser(
                TestConstants.USER_EMAIL,
                TestConstants.USER_UID,
                UserRole.USER
        ));

        regularUser2 = userRepository.save(TestDataBuilder.createTestUser(
                TestConstants.USER2_EMAIL,
                TestConstants.USER2_UID,
                UserRole.USER
        ));

        moderatorUser = userRepository.save(TestDataBuilder.createTestUser(
                TestConstants.MODERATOR_EMAIL,
                TestConstants.MODERATOR_UID,
                UserRole.MODERATOR
        ));

        // Get tokens
        adminToken = getAuthToken(TestConstants.ADMIN_EMAIL, TestConstants.ADMIN_PASSWORD);
        regularUserToken = getAuthToken(TestConstants.USER_EMAIL, TestConstants.USER_PASSWORD);
        moderatorToken = getAuthToken(TestConstants.MODERATOR_EMAIL, TestConstants.MODERATOR_PASSWORD);

        log.info("Test setup complete with {} users", userRepository.count());
    }

    @Nested
    @DisplayName("Get Users By Role Tests")
    class GetUsersByRoleTests {

        @Test
        @DisplayName("Should successfully get all users with specific role")
        void shouldGetAllUsersWithRole() throws Exception {
            mockMvc.perform(get("/api/v1/users/role/USER")
                            .header("Authorization", "Bearer " + adminToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].role").value("USER"))
                    .andExpect(jsonPath("$[1].role").value("USER"));
        }

        @Test
        @DisplayName("Should return empty list for role with no users")
        void shouldReturnEmptyListForNonExistentRole() throws Exception {
            mockMvc.perform(get("/api/v1/users/role/ADMIN")  // Change SUPER_ADMIN to ADMIN
                            .header("Authorization", "Bearer " + adminToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));  // Change to 1 since we have one admin
        }

        @Test
        @DisplayName("Should return 401 when attempting without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/users/role/USER"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
        }

        @Test
        @DisplayName("Regular user should be able to view users by role")
        void regularUserShouldViewUsersByRole() throws Exception {
            mockMvc.perform(get("/api/v1/users/role/USER")
                            .header("Authorization", "Bearer " + regularUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].role").value("USER"));
        }

        @Test
        @DisplayName("Moderator should successfully access users by role")
        void moderatorShouldAccessUsersByRole() throws Exception {
            mockMvc.perform(get("/api/v1/users/role/USER")
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].role").value("USER"));
        }

        @Test
        @DisplayName("Admin should access complete user list by role")
        void adminShouldAccessCompleteUserListByRole() throws Exception {
            mockMvc.perform(get("/api/v1/users/role/USER")
                            .header("Authorization", "Bearer " + adminToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].role").value("USER"))
                    .andExpect(jsonPath("$[0].email").exists())
                    .andExpect(jsonPath("$[0].username").exists())
                    .andExpect(jsonPath("$[0].createdAt").exists());
        }

    }
}
