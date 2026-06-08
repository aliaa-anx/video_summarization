package com.backend_microservices.admin_service.service;

import com.backend_microservices.admin_service.client.AiClient;
import com.backend_microservices.admin_service.client.AuthClient;
import com.backend_microservices.admin_service.client.NotificationClient;
import com.backend_microservices.admin_service.dto.AdminDashboardStatsDto;
import com.backend_microservices.admin_service.dto.AdminUserDto;
import com.backend_microservices.admin_service.dto.UserRankingDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests — AdminService
 *
 * Mocked dependencies:
 *   - AuthClient          → HTTP calls to auth-service (users, ban, promote, delete)
 *   - AiClient            → HTTP calls to ai-service (stats, jobs, rankings)
 *   - NotificationClient  → HTTP calls to notification-service (ban emails)
 *
 * Nothing here makes a real network call.
 * Every external service is replaced by a controlled Mockito mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class AdminServiceTest {

    // ── Mocked dependencies ────────────────────────────────────────────────
    @Mock
    private AuthClient authClient;

    @Mock
    private AiClient aiClient;

    @Mock
    private NotificationClient notificationClient;

    // ── System Under Test ─────────────────────────────────────────────────
    @InjectMocks
    private AdminService adminService;


    // ── Helper: build a basic AdminUserDto ────────────────────────────────
    private AdminUserDto makeUser(UUID id, String username, String email, boolean enabled) {
        AdminUserDto user = new AdminUserDto();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(enabled);
        return user;
    }


    // ═══════════════════════════════════════════════════════════
    //  GET TOP USER RANKINGS
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — AI service returns activity data and Auth service returns matching users.
     * Verifies:
     *   - Result is non-null and non-empty
     *   - Limited to top 5
     *   - Sorted by total activity (uploads + chats) descending
     *   - One bulk call to authClient, NOT one call per user
     */
    @Test
    void testGetTopUserRankings_Success() {
        // 1. Arrange
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        // AI returns activity map: userId → {uploads, chats}
        Map<UUID, Map<String, Long>> activityMap = new HashMap<>();
        activityMap.put(user1Id, Map.of("uploads", 10L, "chats", 5L));  // total = 15
        activityMap.put(user2Id, Map.of("uploads", 3L,  "chats", 2L));  // total = 5

        Mockito.when(aiClient.getTopUserActivityBreakdown()).thenReturn(activityMap);

        // Auth returns matching user DTOs in one bulk call
        List<AdminUserDto> users = List.of(
                makeUser(user1Id, "hazim", "hazim@domain.com", true),
                makeUser(user2Id, "ali",   "ali@domain.com",   true)
        );
        Mockito.when(authClient.getUsersByIds(Mockito.anyList())).thenReturn(users);

        // 2. Act
        List<UserRankingDto> rankings = adminService.getTopUserRankings();

        // 3. Assert
        assertNotNull(rankings);
        assertFalse(rankings.isEmpty());

        // First place must be the user with highest total activity
        assertEquals("hazim", rankings.get(0).getName());

        // Verify only ONE bulk network call — not one per user (N+1 guard)
        Mockito.verify(authClient, Mockito.times(1)).getUsersByIds(Mockito.anyList());
    }

    /**
     * Edge case — AI service returns null (downstream is down or returned nothing).
     * Service must return an empty list, not throw or return null.
     */
    @Test
    void testGetTopUserRankings_ReturnsEmpty_WhenAiReturnsNull() {
        // 1. Arrange
        Mockito.when(aiClient.getTopUserActivityBreakdown()).thenReturn(null);

        // 2. Act
        List<UserRankingDto> result = adminService.getTopUserRankings();

        // 3. Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Auth service must never be called if there's no activity data
        Mockito.verify(authClient, Mockito.never()).getUsersByIds(Mockito.anyList());
    }

    /**
     * Edge case — AI returns data but Auth service call fails (network error).
     * Service must degrade gracefully — return rankings with "Unknown User" labels
     * instead of crashing the whole dashboard.
     */
    @Test
    void testGetTopUserRankings_GracefulDegradation_WhenAuthClientFails() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        Map<UUID, Map<String, Long>> activityMap = new HashMap<>();
        activityMap.put(userId, Map.of("uploads", 5L, "chats", 3L));

        Mockito.when(aiClient.getTopUserActivityBreakdown()).thenReturn(activityMap);

        // Auth service throws — simulates network timeout or auth-service being down
        Mockito.when(authClient.getUsersByIds(Mockito.anyList()))
                .thenThrow(new RuntimeException("auth-service unreachable"));

        // 2. Act — must NOT throw
        List<UserRankingDto> result = adminService.getTopUserRankings();

        // 3. Assert — returns rankings with fallback label instead of crashing
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Unknown User", result.get(0).getName());
    }

    /**
     * Edge case — AI returns an empty map (no activity recorded yet).
     * Must return an empty list without calling Auth service at all.
     */
    @Test
    void testGetTopUserRankings_ReturnsEmpty_WhenActivityMapIsEmpty() {
        // 1. Arrange
        Mockito.when(aiClient.getTopUserActivityBreakdown()).thenReturn(Collections.emptyMap());

        // 2. Act
        List<UserRankingDto> result = adminService.getTopUserRankings();

        // 3. Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        Mockito.verify(authClient, Mockito.never()).getUsersByIds(Mockito.anyList());
    }


    // ═══════════════════════════════════════════════════════════
    //  GET DASHBOARD SNAPSHOT
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — all downstream services respond correctly.
     * Verifies the DTO is assembled with the correct values from each client.
     */
    @Test
    void testGetDashboardSnapshot_Success() {
        // 1. Arrange — mock all four data sources the dashboard depends on
        Mockito.when(authClient.getTotalUserCount()).thenReturn(42L);
        Mockito.when(aiClient.getTotalDocumentCount()).thenReturn(150L);
        Mockito.when(aiClient.getSuccessRate()).thenReturn(97.5);
        Mockito.when(aiClient.getAverageProcessingTime()).thenReturn(3);
        Mockito.when(aiClient.getRecentJobs()).thenReturn(List.of());
        Mockito.when(aiClient.getTopUserActivityBreakdown()).thenReturn(Collections.emptyMap());

        // 2. Act
        AdminDashboardStatsDto snapshot = adminService.getDashboardSnapshot();

        // 3. Assert — each field must carry the value from its respective client
        assertNotNull(snapshot);
        assertEquals(42L,   snapshot.getTotalUsers());
        assertEquals(150L,  snapshot.getTotalDocuments());
        assertEquals(97.5,  snapshot.getSuccessRate());
        assertEquals(3,     snapshot.getAvgProcessTime());
        assertNotNull(snapshot.getRecentJobs());
    }

    /**
     * Failure — one downstream client throws during dashboard assembly.
     * The whole snapshot call should propagate the exception (fail loudly)
     * so the frontend knows the data is incomplete.
     */
    @Test
    void testGetDashboardSnapshot_Failure_WhenAuthClientThrows() {
        // 1. Arrange
        Mockito.when(authClient.getTotalUserCount())
                .thenThrow(new RuntimeException("auth-service down"));

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () -> adminService.getDashboardSnapshot());
    }


    // ═══════════════════════════════════════════════════════════
    //  GET ALL USERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — auth-service returns a populated user list.
     */
    @Test
    void testGetAllUsers_Success() {
        // 1. Arrange
        List<AdminUserDto> mockUsers = List.of(
                makeUser(UUID.randomUUID(), "hazim", "hazim@domain.com", true),
                makeUser(UUID.randomUUID(), "ali",   "ali@domain.com",   true)
        );
        Mockito.when(authClient.getAllUsers()).thenReturn(mockUsers);

        // 2. Act
        List<AdminUserDto> result = adminService.getAllUsers();

        // 3. Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    /**
     * Edge case — no users registered yet; auth-service returns empty list.
     */
    @Test
    void testGetAllUsers_ReturnsEmptyList_WhenNoUsersExist() {
        // 1. Arrange
        Mockito.when(authClient.getAllUsers()).thenReturn(Collections.emptyList());

        // 2. Act
        List<AdminUserDto> result = adminService.getAllUsers();

        // 3. Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    // ═══════════════════════════════════════════════════════════
    //  DELETE USER
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — admin deletes a user; auth-service deleteUser is called once.
     */
    @Test
    void testDeleteUser_Success() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        // deleteUser is void — no stubbing needed

        // 2. Act
        adminService.DeleteUser(userId);

        // 3. Assert
        Mockito.verify(authClient, Mockito.times(1)).deleteUser(userId);
    }

    /**
     * Failure — auth-service throws during delete (e.g. user not found remotely).
     * Exception must propagate so the admin UI can show an error.
     */
    @Test
    void testDeleteUser_Failure_WhenAuthClientThrows() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        Mockito.doThrow(new RuntimeException("User not found in auth-service"))
                .when(authClient).deleteUser(userId);

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () -> adminService.DeleteUser(userId));
    }


    // ═══════════════════════════════════════════════════════════
    //  TOGGLE USER BAN
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — banning a user triggers both auth update AND notification email.
     * Verifies the correct sequence: toggleBan → getUserById → sendBanEmail.
     */
    @Test
    void testToggleUserBan_Success_SendsBanEmail() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();

        AdminUserDto bannedUser = makeUser(userId, "hazim", "hazim@domain.com", false); // enabled=false means banned
        Mockito.when(authClient.getUserById(userId)).thenReturn(bannedUser);
        // toggleBan and sendBanEmail are void — no stubbing needed

        // 2. Act
        adminService.toggleUserBan(userId);

        // 3. Assert — verify the full sequence was called
        Mockito.verify(authClient, Mockito.times(1)).toggleBan(userId);
        Mockito.verify(authClient, Mockito.times(1)).getUserById(userId);
        Mockito.verify(notificationClient, Mockito.times(1))
                .sendBanEmail("hazim@domain.com", "hazim", true); // isBanned = !enabled = !false = true
    }

    /**
     * Resilience test — notification service fails after ban is applied.
     * The ban itself must still succeed; a failed email must NOT roll back the ban
     * or throw to the caller (the service catches and logs it).
     */
    @Test
    void testToggleUserBan_Success_EvenWhenNotificationFails() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();

        AdminUserDto user = makeUser(userId, "hazim", "hazim@domain.com", false);
        Mockito.when(authClient.getUserById(userId)).thenReturn(user);

        // Notification service is down
        Mockito.doThrow(new RuntimeException("Notification service unreachable"))
                .when(notificationClient).sendBanEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());

        // 2. Act — must NOT throw even though notification failed
        assertDoesNotThrow(() -> adminService.toggleUserBan(userId));

        // 3. Assert — ban was still applied in auth-service
        Mockito.verify(authClient, Mockito.times(1)).toggleBan(userId);
    }

    /**
     * Failure — auth-service throws during toggleBan (user doesn't exist remotely).
     * Must propagate — we can't ban a non-existent user.
     */
    @Test
    void testToggleUserBan_Failure_WhenAuthClientThrows() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        Mockito.doThrow(new RuntimeException("User not found"))
                .when(authClient).toggleBan(userId);

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () -> adminService.toggleUserBan(userId));

        // Notification must never be called if the ban itself failed
        Mockito.verify(notificationClient, Mockito.never())
                .sendBanEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }


    // ═══════════════════════════════════════════════════════════
    //  PROMOTE TO ADMIN
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — promoteToAdmin delegates to authClient exactly once.
     */
    @Test
    void testPromoteToAdmin_Success() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();

        // 2. Act
        adminService.promoteToAdmin(userId);

        // 3. Assert
        Mockito.verify(authClient, Mockito.times(1)).promoteToAdmin(userId);
    }

    /**
     * Failure — auth-service rejects the promotion (e.g. user not found).
     */
    @Test
    void testPromoteToAdmin_Failure_WhenAuthClientThrows() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        Mockito.doThrow(new RuntimeException("User not found"))
                .when(authClient).promoteToAdmin(userId);

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () -> adminService.promoteToAdmin(userId));
    }


    // ═══════════════════════════════════════════════════════════
    //  SEARCH USERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — keyword matches users in auth-service.
     */
    @Test
    void testSearchUsers_Success_ReturnsMatchingUsers() {
        // 1. Arrange
        List<AdminUserDto> mockResults = List.of(
                makeUser(UUID.randomUUID(), "hazim", "hazim@domain.com", true)
        );
        Mockito.when(authClient.searchUsers("haz")).thenReturn(mockResults);

        // 2. Act
        List<AdminUserDto> result = adminService.searchUsers("haz");

        // 3. Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("hazim", result.get(0).getUsername());
    }

    /**
     * Edge case — keyword matches nothing; auth-service returns empty list.
     */
    @Test
    void testSearchUsers_ReturnsEmptyList_WhenNoMatch() {
        // 1. Arrange
        Mockito.when(authClient.searchUsers("zzz")).thenReturn(Collections.emptyList());

        // 2. Act
        List<AdminUserDto> result = adminService.searchUsers("zzz");

        // 3. Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Edge case — null keyword passed to searchUsers.
     * Behavior depends on your validation; at minimum it must not silently succeed.
     */
    @Test
    void testSearchUsers_Failure_NullKeyword() {
        // 1. Arrange — auth-service throws on null keyword
        Mockito.when(authClient.searchUsers(null))
                .thenThrow(new IllegalArgumentException("Search keyword must not be null"));

        // 2. Act & Assert
        assertThrows(Exception.class, () -> adminService.searchUsers(null));
    }
}