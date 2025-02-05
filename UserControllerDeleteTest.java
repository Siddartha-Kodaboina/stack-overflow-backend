package com.example.stackoverflow_auth_service.controller.user;

import com.example.stackoverflow_auth_service.base.BaseControllerTest;
import com.example.stackoverflow_auth_service.model.User;
import com.example.stackoverflow_auth_service.model.UserRole;
import com.example.stackoverflow_auth_service.util.TestConstants;
import com.example.stackoverflow_auth_service.util.TestDataBuilder;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ActiveProfiles("test")
@DisplayName("User Delete Operations")
class UserControllerDeleteTest extends BaseControllerTest {

    @MockBean
    private FirebaseAuth firebaseAuth;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private User moderatorUser;
    private String regularUserToken;
    private String moderatorToken;

    private void printDatabaseState() {
        log.info("Current Database State:");
        userRepository.findAll().forEach(user ->
                log.info("User: ID={}, Email={}, Role={}",
                        user.getId(), user.getEmail(), user.getRole()));
    }

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

        // Get admin token
        adminToken = getAuthToken(TestConstants.ADMIN_EMAIL, TestConstants.ADMIN_PASSWORD);
        regularUserToken = getAuthToken(TestConstants.USER_EMAIL, TestConstants.USER_PASSWORD);
        moderatorToken = getAuthToken(TestConstants.MODERATOR_EMAIL, TestConstants.MODERATOR_PASSWORD);
        log.info("Admin token obtained");

        // Verify database state
        printDatabaseState();

        // Mock Firebase delete operation
        doNothing().when(firebaseAuth).deleteUser(anyString());

        // or if you need to verify the call:
//        when(firebaseAuth.deleteUser(anyString())).thenReturn(void.class);
        // Add detailed logging
        log.info("Created Users in H2:");
        log.info("Admin User - ID: {}, Email: {}, FirebaseUID: {}",
                adminUser.getId(), adminUser.getEmail(), adminUser.getFirebaseUid());
        log.info("Regular User - ID: {}, Email: {}, FirebaseUID: {}",
                regularUser.getId(), regularUser.getEmail(), regularUser.getFirebaseUid());

        // Verify database content
        List<User> allUsers = userRepository.findAll();
        log.info("Total users in H2: {}", allUsers.size());
        allUsers.forEach(user ->
                log.info("User in DB - ID: {}, Email: {}, FirebaseUID: {}",
                        user.getId(), user.getEmail(), user.getFirebaseUid()));
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Admin should successfully delete a regular user")
        void adminShouldDeleteRegularUser() throws Exception {
            // Print initial state
            log.info("Initial database state:");
            userRepository.findAll().forEach(u ->
                    log.info("User: id={}, email={}, firebaseUid={}",
                            u.getId(), u.getEmail(), u.getFirebaseUid()));

            assertTrue(userRepository.existsById(regularUser.getId()));

            mockMvc.perform(delete("/api/v1/users/" + regularUser.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andDo(print())  // Print response details
                    .andExpect(status().isNoContent());

            assertFalse(userRepository.existsById(regularUser.getId()));
            log.info("Regular user successfully deleted");

            // Print final state
            log.info("Final database state:");
            userRepository.findAll().forEach(u ->
                    log.info("User: id={}, email={}", u.getId(), u.getEmail()));
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent user")
        void shouldReturn404WhenUserNotFound() throws Exception {
            Long nonExistentId = 999L;

            assertFalse(userRepository.existsById(nonExistentId));

            mockMvc.perform(delete("/api/v1/users/" + nonExistentId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("User not found with id: " + nonExistentId))
                    .andDo(print());  // This helps with debugging
        }

        @Test
        @DisplayName("Should return 401 when attempting deletion without authentication")
        void shouldReturn401WhenNoAuthToken() throws Exception {
            mockMvc.perform(delete("/api/v1/users/" + regularUser.getId()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Authentication required"))
                    .andExpect(jsonPath("$.path").value("/api/v1/users/" + regularUser.getId()))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return 403 when regular user attempts deletion")
        void shouldReturn403WhenRegularUserAttemptsDelete() throws Exception {
            mockMvc.perform(delete("/api/v1/users/" + regularUser.getId())
                            .header("Authorization", "Bearer " + regularUserToken))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.message").value("Access denied"))
                    .andExpect(jsonPath("$.path").value("/api/v1/users/" + regularUser.getId()));
        }

        @Test
        @DisplayName("Should return 403 when moderator attempts deletion")
        void shouldReturn403WhenModeratorAttemptsDelete() throws Exception {
            mockMvc.perform(delete("/api/v1/users/" + regularUser.getId())
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied"));
        }

        @Test
        @DisplayName("Should return 403 when attempting to delete an admin user")
        void shouldReturn403WhenAttemptingToDeleteAdmin() throws Exception {
            mockMvc.perform(delete("/api/v1/users/" + adminUser.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Cannot delete admin user"));
        }

//        @Test
//        @DisplayName("Should return 401 with expired token")
//        void shouldReturn401WithExpiredToken() throws Exception {
//            String expiredToken = "eyJhbGciOiJSUzI1NiJ9.expired.token";
//
//            mockMvc.perform(delete("/api/v1/users/" + regularUser.getId())
//                            .header("Authorization", "Bearer " + expiredToken))
//                    .andDo(print())
//                    .andExpect(status().isUnauthorized())
//                    .andExpect(jsonPath("$.message").value("Token has expired"));
//        }
    }
}
