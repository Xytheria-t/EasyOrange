package com.cartethyia.easyorange.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AuthUser} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("AuthUser Tests")
class AuthUserTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor with null roles should convert to empty set")
        void constructor_withNullRoles_convertsToEmptySet() {
            // Arrange
            Set<String> nullRoles = null;
            Set<String> permissions = Set.of("user:read");

            // Act
            AuthUser authUser = new AuthUser(1L, "testuser", nullRoles, permissions, System.currentTimeMillis());

            // Assert
            assertThat(authUser.roles()).isNotNull();
            assertThat(authUser.roles()).isEmpty();
        }

        @Test
        @DisplayName("Constructor with null permissions should convert to empty set")
        void constructor_withNullPermissions_convertsToEmptySet() {
            // Arrange
            Set<String> roles = Set.of("ROLE_USER");
            Set<String> nullPermissions = null;

            // Act
            AuthUser authUser = new AuthUser(1L, "testuser", roles, nullPermissions, System.currentTimeMillis());

            // Assert
            assertThat(authUser.permissions()).isNotNull();
            assertThat(authUser.permissions()).isEmpty();
        }

        @Test
        @DisplayName("Constructor with both null should convert both to empty sets")
        void constructor_withBothNull_convertsBothToEmptySets() {
            // Act
            AuthUser authUser = new AuthUser(1L, "testuser", null, null, System.currentTimeMillis());

            // Assert
            assertThat(authUser.roles()).isNotNull().isEmpty();
            assertThat(authUser.permissions()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Roles should be unmodifiable")
        void roles_areUnmodifiable() {
            // Arrange
            Set<String> mutableRoles = new HashSet<>();
            mutableRoles.add("ROLE_USER");
            AuthUser authUser = new AuthUser(1L, "testuser", mutableRoles, null, System.currentTimeMillis());

            // Act & Assert
            assertThatThrownBy(() -> authUser.roles().add("ROLE_ADMIN"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Permissions should be unmodifiable")
        void permissions_areUnmodifiable() {
            // Arrange
            Set<String> mutablePermissions = new HashSet<>();
            mutablePermissions.add("user:read");
            AuthUser authUser = new AuthUser(1L, "testuser", null, mutablePermissions, System.currentTimeMillis());

            // Act & Assert
            assertThatThrownBy(() -> authUser.permissions().add("user:write"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Original collection modification should not affect AuthUser")
        void originalCollectionModification_shouldNotAffectAuthUser() {
            // Arrange
            Set<String> mutableRoles = new HashSet<>();
            mutableRoles.add("ROLE_USER");
            AuthUser authUser = new AuthUser(1L, "testuser", mutableRoles, null, System.currentTimeMillis());

            // Act
            mutableRoles.add("ROLE_ADMIN");
            mutableRoles.clear();

            // Assert
            assertThat(authUser.roles()).containsOnly("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Builder with null roles should create empty set")
        void builder_withNullRoles_createsEmptySet() {
            // Act
            AuthUser authUser = AuthUser.builder()
                    .userId(1L)
                    .username("testuser")
                    .roles(null)
                    .permissions(Set.of("user:read"))
                    .loginTime(System.currentTimeMillis())
                    .build();

            // Assert
            assertThat(authUser.roles()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Builder with null permissions should create empty set")
        void builder_withNullPermissions_createsEmptySet() {
            // Act
            AuthUser authUser = AuthUser.builder()
                    .userId(1L)
                    .username("testuser")
                    .roles(Set.of("ROLE_USER"))
                    .permissions(null)
                    .loginTime(System.currentTimeMillis())
                    .build();

            // Assert
            assertThat(authUser.permissions()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Builder with valid data should create AuthUser correctly")
        void builder_withValidData_createsAuthUserCorrectly() {
            // Arrange
            Long userId = 1L;
            String username = "testuser";
            Set<String> roles = Set.of("ROLE_USER", "ROLE_ADMIN");
            Set<String> permissions = Set.of("user:read", "user:write");
            Long loginTime = System.currentTimeMillis();

            // Act
            AuthUser authUser = AuthUser.builder()
                    .userId(userId)
                    .username(username)
                    .roles(roles)
                    .permissions(permissions)
                    .loginTime(loginTime)
                    .build();

            // Assert
            assertThat(authUser.userId()).isEqualTo(userId);
            assertThat(authUser.username()).isEqualTo(username);
            assertThat(authUser.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
            assertThat(authUser.permissions()).containsExactlyInAnyOrder("user:read", "user:write");
            assertThat(authUser.loginTime()).isEqualTo(loginTime);
        }
    }

    @Nested
    @DisplayName("Record Component Tests")
    class RecordComponentTests {

        @Test
        @DisplayName("Equals should work correctly")
        void equals_shouldWorkCorrectly() {
            // Arrange
            Long loginTime = System.currentTimeMillis();
            AuthUser user1 = new AuthUser(1L, "testuser", Set.of("ROLE_USER"), Set.of("user:read"), loginTime);
            AuthUser user2 = new AuthUser(1L, "testuser", Set.of("ROLE_USER"), Set.of("user:read"), loginTime);
            AuthUser user3 = new AuthUser(2L, "testuser", Set.of("ROLE_USER"), Set.of("user:read"), loginTime);

            // Assert
            assertThat(user1).isEqualTo(user2);
            assertThat(user1).isNotEqualTo(user3);
        }

        @Test
        @DisplayName("HashCode should be consistent")
        void hashCode_shouldBeConsistent() {
            // Arrange
            Long loginTime = System.currentTimeMillis();
            AuthUser user1 = new AuthUser(1L, "testuser", Set.of("ROLE_USER"), Set.of("user:read"), loginTime);
            AuthUser user2 = new AuthUser(1L, "testuser", Set.of("ROLE_USER"), Set.of("user:read"), loginTime);

            // Assert
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("ToString should contain all fields")
        void toString_shouldContainAllFields() {
            // Arrange
            AuthUser authUser = new AuthUser(1L, "testuser", Set.of("ROLE_USER"), Set.of("user:read"), 1234567890L);

            // Act
            String result = authUser.toString();

            // Assert
            assertThat(result).contains("1", "testuser", "ROLE_USER", "user:read", "1234567890");
        }
    }
}
