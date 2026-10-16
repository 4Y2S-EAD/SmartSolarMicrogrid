using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using SmartSolarMicrogrid.API.DTOs.member1;
using SmartSolarMicrogrid.API.Models;
using Xunit;

namespace SmartSolarMicrogrid.Tests
{
    public class Member1UnitTests
    {
        // ---------------------------------------------------------------------
        // Unit Test 1: Password Hashing & Verification Logic (BCrypt)
        // ---------------------------------------------------------------------
        [Fact]
        public void UT01_BCrypt_PasswordHashing_GeneratesValidHashAndVerifiesSuccessfully()
        {
            // Arrange
            string rawPassword = "SecurePassword123!";
            string wrongPassword = "WrongPassword456!";

            // Act
            string passwordHash = BCrypt.Net.BCrypt.HashPassword(rawPassword);
            bool isCorrectValid = BCrypt.Net.BCrypt.Verify(rawPassword, passwordHash);
            bool isWrongValid = BCrypt.Net.BCrypt.Verify(wrongPassword, passwordHash);

            // Assert
            Assert.False(string.IsNullOrWhiteSpace(passwordHash));
            Assert.NotEqual(rawPassword, passwordHash);
            Assert.True(isCorrectValid, "Valid password should verify successfully against the generated hash.");
            Assert.False(isWrongValid, "Incorrect password should fail verification.");
        }

        // ---------------------------------------------------------------------
        // Unit Test 2: User Entity Default Initialization
        // ---------------------------------------------------------------------
        [Fact]
        public void UT02_User_DefaultInitialization_SetsPendingStatusAndTimestamp()
        {
            // Arrange & Act
            var user = new User
            {
                NIC = "991234567V",
                FullName = "Test Prosumer",
                Email = "test@microgrid.io",
                PasswordHash = "dummyHash123"
            };

            // Assert
            Assert.Equal(AccountStatus.Pending, user.AccountStatus);
            Assert.False(user.IsApproved);
            Assert.False(user.IsDeactivationRequested);
            Assert.Null(user.DeactivationReason);
            Assert.True((DateTime.UtcNow - user.CreatedAt).TotalSeconds < 10, "CreatedAt should be initialized to current UTC time.");
        }

        // ---------------------------------------------------------------------
        // Unit Test 3: CreateUserDto Model Validation (Required Fields)
        // ---------------------------------------------------------------------
        [Fact]
        public void UT03_CreateUserDto_Validation_FailsWhenRequiredFieldsAreMissing()
        {
            // Arrange - Missing NIC and FullName
            var invalidDto = new CreateUserDto
            {
                Email = "valid@microgrid.io",
                Password = "Password123!",
                Role = "grid_operator"
            };

            var validationContext = new ValidationContext(invalidDto);
            var validationResults = new List<ValidationResult>();

            // Act
            bool isValid = Validator.TryValidateObject(invalidDto, validationContext, validationResults, validateAllProperties: true);

            // Assert
            Assert.False(isValid, "Validation should fail when required fields like NIC and FullName are missing.");
            Assert.Contains(validationResults, v => v.MemberNames.Contains(nameof(CreateUserDto.NIC)));
            Assert.Contains(validationResults, v => v.MemberNames.Contains(nameof(CreateUserDto.FullName)));
        }

        // ---------------------------------------------------------------------
        // Unit Test 4: RegisterDto Email Validation
        // ---------------------------------------------------------------------
        [Fact]
        public void UT04_RegisterDto_Validation_ValidatesEmailFormatAndMinPasswordLength()
        {
            // Arrange - Invalid Email format and password < 6 characters
            var invalidDto = new RegisterDto
            {
                NIC = "123456789V",
                FullName = "Kamal Perera",
                Email = "not-a-valid-email-address",
                Password = "123" // Below MinLength(6)
            };

            var context = new ValidationContext(invalidDto);
            var results = new List<ValidationResult>();

            // Act
            bool isValid = Validator.TryValidateObject(invalidDto, context, results, validateAllProperties: true);

            // Assert
            Assert.False(isValid, "Validation should fail for malformed email and short password.");
            Assert.Contains(results, r => r.MemberNames.Contains(nameof(RegisterDto.Email)));
            Assert.Contains(results, r => r.MemberNames.Contains(nameof(RegisterDto.Password)));
        }

        // ---------------------------------------------------------------------
        // Unit Test 5: Role Determination Logic for Grid Operator vs Backoffice
        // ---------------------------------------------------------------------
        [Theory]
        [InlineData("grid_operator", UserRole.GridOperator)]
        [InlineData("GRID_OPERATOR", UserRole.GridOperator)]
        [InlineData("backoffice", UserRole.Backoffice)]
        [InlineData("admin", UserRole.Backoffice)]
        public void UT05_UserRole_Mapping_CorrectlyAssignsGridOperatorAndBackoffice(string inputRole, UserRole expectedRole)
        {
            // Arrange & Act - Same mapping logic used in UserManagementController
            var determinedRole = inputRole.ToLower() == "grid_operator" 
                ? UserRole.GridOperator 
                : UserRole.Backoffice;

            // Assert
            Assert.Equal(expectedRole, determinedRole);
        }
    }
}
