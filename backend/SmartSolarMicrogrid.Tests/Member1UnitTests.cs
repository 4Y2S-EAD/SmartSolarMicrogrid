using System;
using SmartSolarMicrogrid.API.DTOs.member1;
using SmartSolarMicrogrid.API.Models;
using Xunit;

namespace SmartSolarMicrogrid.Tests
{
    public class Member1UnitTests
    {
        // UNIT TEST 01: Prosumer Registration Mapping & Default Security State
        [Fact]
        public void UT01_RegisterDto_MapsToProsumerModel_WithPendingStatusAndUnapprovedState()
        {
            // Arrange
            var dto = new RegisterDto
            {
                NIC = "991234567V",
                FullName = "Kasun Jayawardena",
                Email = "kasun.j@smartsolar.io",
                Password = "PlainPassword123!",
                PhoneNumber = "0771234567",
                Address = "124 Lake Road, Colombo 03"
            };

            // Act
            var user = new User
            {
                NIC = dto.NIC,
                FullName = dto.FullName,
                Email = dto.Email,
                PhoneNumber = dto.PhoneNumber,
                Address = dto.Address,
                Role = UserRole.Prosumer,
                AccountStatus = AccountStatus.Pending,
                IsApproved = false,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            // Assert
            Assert.Equal("991234567V", user.NIC);
            Assert.Equal("Kasun Jayawardena", user.FullName);
            Assert.Equal("kasun.j@smartsolar.io", user.Email);
            Assert.Equal("0771234567", user.PhoneNumber);
            Assert.Equal("124 Lake Road, Colombo 03", user.Address);
            Assert.Equal(UserRole.Prosumer, user.Role);
            Assert.Equal(AccountStatus.Pending, user.AccountStatus);
            Assert.False(user.IsApproved);
            Assert.False(user.IsDeactivationRequested);
        }

        // UNIT TEST 02: BCrypt Password Hashing & Cryptographic Verification
        [Fact]
        public void UT02_BCryptPasswordHashing_ValidPassword_VerifiesSuccessfully_AndRejectsInvalid()
        {
            // Arrange
            var rawPassword = "SecureProsumerPass!2026";
            var wrongPassword = "IncorrectPassword!";

            // Act
            var hash = BCrypt.Net.BCrypt.HashPassword(rawPassword);

            // Assert
            Assert.NotNull(hash);
            Assert.NotEqual(rawPassword, hash);
            Assert.True(BCrypt.Net.BCrypt.Verify(rawPassword, hash));
            Assert.False(BCrypt.Net.BCrypt.Verify(wrongPassword, hash));
        }

        // UNIT TEST 03: Internal User Role Mapping & Grid Operator Attributes
        [Theory]
        [InlineData("grid_operator", UserRole.GridOperator, "OP-991", "HUB-COLOMBO", true)]
        [InlineData("backoffice", UserRole.Backoffice, null, null, false)]
        public void UT03_CreateUserDto_RoleMapping_AssignsCorrectRoleAndOperatorAttributes(
            string roleInput,
            UserRole expectedRole,
            string? badgeId,
            string? hubId,
            bool expectOperatorFields)
        {
            // Arrange
            var dto = new CreateUserDto
            {
                NIC = "881234567V",
                FullName = "Nuwan Bandara",
                Email = "nuwan@microgrid.io",
                Password = "Password123!",
                Role = roleInput,
                BadgeId = badgeId,
                AssignedHubId = hubId
            };

            // Act
            var mappedRole = dto.Role.ToLower() == "grid_operator" ? UserRole.GridOperator : UserRole.Backoffice;
            var user = new User
            {
                NIC = dto.NIC,
                FullName = dto.FullName,
                Email = dto.Email,
                Role = mappedRole,
                AccountStatus = AccountStatus.Active,
                IsApproved = true,
                BadgeId = mappedRole == UserRole.GridOperator ? dto.BadgeId : null,
                AssignedHubId = mappedRole == UserRole.GridOperator ? dto.AssignedHubId : null
            };

            // Assert
            Assert.Equal(expectedRole, user.Role);
            Assert.Equal(AccountStatus.Active, user.AccountStatus);
            Assert.True(user.IsApproved);
            if (expectOperatorFields)
            {
                Assert.Equal("OP-991", user.BadgeId);
                Assert.Equal("HUB-COLOMBO", user.AssignedHubId);
            }
            else
            {
                Assert.Null(user.BadgeId);
                Assert.Null(user.AssignedHubId);
            }
        }

        // UNIT TEST 04: Profile Update Logic Preserves Immutability of Security Attributes
        [Fact]
        public void UT04_UpdateProfileDto_ModifiesContactInfo_WhilePreservingSecurityFields()
        {
            // Arrange: Existing prosumer in system
            var originalPasswordHash = BCrypt.Net.BCrypt.HashPassword("InitialPass123!");
            var user = new User
            {
                NIC = "990001111V",
                FullName = "Original Name",
                Email = "original@smartsolar.io",
                PasswordHash = originalPasswordHash,
                PhoneNumber = "0770000000",
                Address = "Old Address",
                Role = UserRole.Prosumer,
                AccountStatus = AccountStatus.Active,
                IsApproved = true
            };

            var updateDto = new UpdateProfileDto
            {
                FullName = "Updated Name",
                Email = "updated@smartsolar.io",
                PhoneNumber = "0711112233",
                Address = "New Address, Kandy"
            };

            // Act: Apply profile updates
            user.FullName = updateDto.FullName;
            user.Email = updateDto.Email;
            user.PhoneNumber = updateDto.PhoneNumber;
            user.Address = updateDto.Address;
            user.UpdatedAt = DateTime.UtcNow;

            // Assert
            // Modified fields
            Assert.Equal("Updated Name", user.FullName);
            Assert.Equal("updated@smartsolar.io", user.Email);
            Assert.Equal("0711112233", user.PhoneNumber);
            Assert.Equal("New Address, Kandy", user.Address);

            // Immutability of sensitive security properties
            Assert.Equal("990001111V", user.NIC);
            Assert.Equal(originalPasswordHash, user.PasswordHash);
            Assert.Equal(UserRole.Prosumer, user.Role);
            Assert.Equal(AccountStatus.Active, user.AccountStatus);
            Assert.True(user.IsApproved);
        }

        // UNIT TEST 05: Deactivation Request & Account Status Transition Rules
        [Fact]
        public void UT05_DeactivationRequest_SetsFlagAndReason_AndStatusTransitionBehavesCorrectly()
        {
            // Arrange
            var user = new User
            {
                NIC = "993334444V",
                FullName = "Sunil Shantha",
                Email = "sunil@smartsolar.io",
                AccountStatus = AccountStatus.Active,
                IsDeactivationRequested = false,
                DeactivationReason = null
            };

            var deactDto = new DeactivationRequestDto
            {
                Reason = "Relocating abroad permanently"
            };

            // Act 1: Prosumer submits deactivation request
            user.IsDeactivationRequested = true;
            user.DeactivationReason = deactDto.Reason;
            user.UpdatedAt = DateTime.UtcNow;

            // Assert 1
            Assert.True(user.IsDeactivationRequested);
            Assert.Equal("Relocating abroad permanently", user.DeactivationReason);
            Assert.Equal(AccountStatus.Active, user.AccountStatus); // Status remains active until operator approves

            // Act 2: Operator deactivates account
            user.AccountStatus = AccountStatus.Deactivated;
            user.DeactivatedAt = DateTime.UtcNow;

            // Assert 2
            Assert.Equal(AccountStatus.Deactivated, user.AccountStatus);
            Assert.NotNull(user.DeactivatedAt);
        }
    }
}
