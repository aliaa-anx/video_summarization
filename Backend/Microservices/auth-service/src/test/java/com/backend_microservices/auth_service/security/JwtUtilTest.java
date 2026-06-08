package com.backend_microservices.auth_service.security;

import com.backend_microservices.auth_service.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests — JwtUtil
 *
 * Why ReflectionTestUtils?
 *   JwtUtil uses @Value("${jwt.secret}") and @Value("${jwt.expiration-ms}") —
 *   Spring injects those at runtime from application.properties.
 *   In a plain unit test there is no Spring context, so we use
 *   ReflectionTestUtils.setField() to inject test values directly into the
 *   private fields — no constructor change needed in production code.
 *
 * Why @InjectMocks instead of new JwtUtil()?
 *   JwtUtil has no constructor that accepts a String secret, so we let
 *   Mockito create the instance and then we set the fields via reflection.
 *
 * Security invariants tested:
 *   - A freshly generated token must be valid
 *   - A tampered token (wrong signature) must be REJECTED
 *   - A garbage / null string must be REJECTED
 *   - Username and userId extracted from a valid token match what was embedded
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil; // Mockito creates instance; we inject fields below

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Builds a Role entity the same way your production code does. */
    private Role makeRole(String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        return role;
    }

    @BeforeEach
    void setUp() {
        // Inject @Value fields that Spring would normally provide at runtime.
        // The secret must be ≥ 32 chars for HS256.
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-for-unit-testing-only-32chars!!");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs",
                3_600_000L); // 1 hour
    }


    // ═══════════════════════════════════════════════════════════
    //  TOKEN GENERATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — generateToken produces a non-null, non-empty JWT string
     * with the expected 3-segment structure (header.payload.signature).
     */
    @Test
    void testGenerateToken_Success_ReturnsValidJwtString() {
        // 1. Arrange
        UUID userId     = UUID.randomUUID();
        String username = "hazim";
        Set<Role> roles = Set.of(makeRole("ROLE_USER"));

        // 2. Act
        String token = jwtUtil.generateToken(userId, username, roles);

        // 3. Assert
        assertNotNull(token);
        assertFalse(token.isBlank());
        // A JWT always has exactly 3 dot-separated segments: header.payload.signature
        assertEquals(3, token.split("\\.").length);
    }

    /**
     * Edge case — generating a token with an empty roles set.
     * The roles claim becomes an empty string; token must still be produced.
     */
    @Test
    void testGenerateToken_Success_WithEmptyRoles() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();

        // 2. Act
        String token = jwtUtil.generateToken(userId, "hazim", Set.of());

        // 3. Assert
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    /**
     * Multiple roles — generateToken must handle a Set with several Role objects.
     * Roles are comma-joined inside the token; no exception should be thrown.
     */
    @Test
    void testGenerateToken_Success_WithMultipleRoles() {
        // 1. Arrange
        Set<Role> roles = Set.of(makeRole("ROLE_USER"), makeRole("ROLE_ADMIN"));

        // 2. Act
        String token = jwtUtil.generateToken(UUID.randomUUID(), "hazim", roles);

        // 3. Assert
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }



    /**
     * Edge case — null userId passed to generateToken.
     * UUID.toString() would NPE; service must not produce a token.
     */
    @Test
    void testGenerateToken_Failure_NullUserId() {
        // 2. Act & Assert
        assertThrows(Exception.class, () ->
                jwtUtil.generateToken(null, "hazim", Set.of(makeRole("ROLE_USER")))
        );
    }


    // ═══════════════════════════════════════════════════════════
    //  TOKEN VALIDATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — a freshly generated token must be valid.
     * Full round-trip: generate → validateToken → true.
     */
    @Test
    void testValidateToken_Success_FreshToken() {
        // 1. Arrange
        String token = jwtUtil.generateToken(
                UUID.randomUUID(), "hazim", Set.of(makeRole("ROLE_USER"))
        );

        // 2. Act
        boolean isValid = jwtUtil.validateToken(token);

        // 3. Assert
        assertTrue(isValid);
    }

    /**
     * Security test — a tampered token (signature segment replaced) must be REJECTED.
     * validateToken() catches JwtException internally and returns false — never throws.
     */
    @Test
    void testValidateToken_Failure_TamperedSignature() {
        // 1. Arrange — generate a real token then corrupt its signature segment
        String realToken = jwtUtil.generateToken(
                UUID.randomUUID(), "hazim", Set.of(makeRole("ROLE_USER"))
        );
        String[] parts        = realToken.split("\\.");
        String tamperedToken  = parts[0] + "." + parts[1] + ".invalidsignatureXYZ";

        // 2. Act
        boolean isValid = jwtUtil.validateToken(tamperedToken);

        // 3. Assert — your validateToken catches JwtException and returns false
        assertFalse(isValid);
    }

    /**
     * Security test — a completely random / garbage string must be REJECTED.
     * validateToken() must return false, not propagate an exception to the caller.
     */
    @Test
    void testValidateToken_Failure_GarbageString() {
        // 2. Act
        boolean isValid = jwtUtil.validateToken("not.a.jwt");

        // 3. Assert
        assertFalse(isValid);
    }

    /**
     * Edge case — null token string.
     * validateToken catches IllegalArgumentException internally and returns false.
     */
    @Test
    void testValidateToken_Failure_NullToken() {
        // 2. Act
        boolean isValid = jwtUtil.validateToken(null);

        // 3. Assert
        assertFalse(isValid);
    }


    // ═══════════════════════════════════════════════════════════
    //  USERNAME EXTRACTION
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — username extracted from a valid token matches what was embedded.
     * Validates the subject claim round-trip.
     */
    @Test
    void testExtractUsername_Success() {
        // 1. Arrange
        String expectedUsername = "hazim";
        String token = jwtUtil.generateToken(
                UUID.randomUUID(), expectedUsername, Set.of(makeRole("ROLE_USER"))
        );

        // 2. Act
        String extracted = jwtUtil.extractUsername(token);

        // 3. Assert
        assertEquals(expectedUsername, extracted);
    }

    /**
     * Failure — extracting a username from a tampered token must throw.
     * Returning any username from an invalid token would be a security hole.
     */
    @Test
    void testExtractUsername_Failure_InvalidToken() {
        // 2. Act & Assert
        assertThrows(Exception.class, () ->
                jwtUtil.extractUsername("invalid.token.here")
        );
    }


    // ═══════════════════════════════════════════════════════════
    //  USER ID EXTRACTION
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — userId extracted from a valid token matches the original UUID.
     * Validates the custom "userId" claim round-trip.
     */
    @Test
    void testExtractUserId_Success() {
        // 1. Arrange
        UUID expectedId = UUID.randomUUID();
        String token = jwtUtil.generateToken(
                expectedId, "hazim", Set.of(makeRole("ROLE_USER"))
        );

        // 2. Act
        UUID extractedId = jwtUtil.extractUserId(token);

        // 3. Assert
        assertEquals(expectedId, extractedId);
    }

    /**
     * Failure — extracting userId from a tampered token must throw.
     */
    @Test
    void testExtractUserId_Failure_InvalidToken() {
        // 2. Act & Assert
        assertThrows(Exception.class, () ->
                jwtUtil.extractUserId("tampered.token.xyz")
        );
    }
}