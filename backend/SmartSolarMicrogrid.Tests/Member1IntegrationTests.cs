using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Controllers.member1;
using SmartSolarMicrogrid.API.DTOs.member1;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using Xunit;

namespace SmartSolarMicrogrid.Tests
{
    public class Member1IntegrationTests
    {
        private readonly MongoDbService _mongoDbService;
        private readonly ProsumerAuthController _authController;
        private readonly ProsumerProfileController _profileController;
        private readonly UserManagementController _userController;

        public Member1IntegrationTests()
        {
            _mongoDbService = TestSharedContext.MongoDbService;
            _authController = new ProsumerAuthController(TestSharedContext.MongoDbService, TestSharedContext.Configuration);
            _profileController = new ProsumerProfileController(TestSharedContext.MongoDbService);
            _userController = new UserManagementController(TestSharedContext.MongoDbService);
        }

        // INTEGRATION TEST 01: Register Prosumer & Login (Generates Valid JWT Token)
        [Fact]
        public async Task IT01_RegisterProsumer_And_Login_ReturnsTokenAndUserData()
        {
            // Arrange
            var randomSuffix = Guid.NewGuid().ToString("N")[..7].ToUpper();
            var nic = $"99{randomSuffix}V";
            var email = $"it_prosumer_{randomSuffix}@smartsolar.io";
            var password = "SecurePassword123!";

            var registerDto = new RegisterDto
            {
                NIC = nic,
                FullName = "Integration Test Prosumer",
                Email = email,
                Password = password,
                PhoneNumber = "0771234567",
                Address = "12 Integration Way, Colombo"
            };

            try
            {
                // Act 1: Register Prosumer
                var registerResult = await _authController.Register(registerDto);
                var okRegister = Assert.IsType<OkObjectResult>(registerResult);
                Assert.NotNull(okRegister.Value);

                // Act 2: Login as Registered Prosumer
                var loginDto = new LoginDto
                {
                    NIC = nic,
                    Password = password
                };

                var loginResult = await _authController.Login(loginDto);

                // Assert
                var okLogin = Assert.IsType<OkObjectResult>(loginResult);
                Assert.NotNull(okLogin.Value);

                var tokenProp = okLogin.Value.GetType().GetProperty("Token");
                Assert.NotNull(tokenProp);
                var token = tokenProp.GetValue(okLogin.Value) as string;
                Assert.False(string.IsNullOrWhiteSpace(token));

                var userProp = okLogin.Value.GetType().GetProperty("User");
                Assert.NotNull(userProp);
                var userObj = userProp.GetValue(okLogin.Value);
                Assert.NotNull(userObj);

                var nicProp = userObj.GetType().GetProperty("NIC");
                Assert.Equal(nic, nicProp?.GetValue(userObj)?.ToString());
            }
            finally
            {
                // Cleanup registered test user
                await _mongoDbService.Users.DeleteOneAsync(u => u.NIC == nic);
            }
        }

        // INTEGRATION TEST 02: Login with Invalid Password Returns 401 Unauthorized
        [Fact]
        public async Task IT02_Login_InvalidCredentials_ReturnsUnauthorized()
        {
            // Arrange
            var randomSuffix = Guid.NewGuid().ToString("N")[..7].ToUpper();
            var nic = $"99{randomSuffix}V";
            var email = $"it_unauth_{randomSuffix}@smartsolar.io";
            var password = "CorrectPassword123!";

            var registerDto = new RegisterDto
            {
                NIC = nic,
                FullName = "Unauthorized Test User",
                Email = email,
                Password = password
            };

            await _authController.Register(registerDto);

            try
            {
                // Act: Attempt login with incorrect password
                var invalidLoginDto = new LoginDto
                {
                    NIC = nic,
                    Password = "WrongPassword999!"
                };

                var actionResult = await _authController.Login(invalidLoginDto);

                // Assert
                var unauthResult = Assert.IsType<UnauthorizedObjectResult>(actionResult);
                Assert.NotNull(unauthResult.Value);
            }
            finally
            {
                await _mongoDbService.Users.DeleteOneAsync(u => u.NIC == nic);
            }
        }

        // INTEGRATION TEST 03: GET Prosumer Profile & PUT Update Profile
        [Fact]
        public async Task IT03_GetProfile_And_UpdateProfile_PersistsChangesInDatabase()
        {
            // Arrange
            var randomSuffix = Guid.NewGuid().ToString("N")[..7].ToUpper();
            var nic = $"99{randomSuffix}V";
            var email = $"it_profile_{randomSuffix}@smartsolar.io";
            var password = "ProfilePassword123!";

            var registerDto = new RegisterDto
            {
                NIC = nic,
                FullName = "Initial Profile Name",
                Email = email,
                Password = password,
                PhoneNumber = "0710000000",
                Address = "Old Address"
            };

            await _authController.Register(registerDto);

            try
            {
                // Act 1: GET Profile
                var getResult = await _profileController.GetProfile(nic);
                var okGet = Assert.IsType<OkObjectResult>(getResult);
                Assert.NotNull(okGet.Value);
                var nicProp = okGet.Value.GetType().GetProperty("NIC");
                Assert.Equal(nic, nicProp?.GetValue(okGet.Value)?.ToString());

                // Act 2: PUT Update Profile
                var updateDto = new UpdateProfileDto
                {
                    FullName = "Updated Profile Name",
                    Email = $"updated_{email}",
                    PhoneNumber = "0778889999",
                    Address = "New Renewable Park, Malabe"
                };

                var updateResult = await _profileController.UpdateProfile(nic, updateDto);
                Assert.IsType<OkObjectResult>(updateResult);

                // Act 3: Verify directly in MongoDB persistence
                var updatedUser = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();
                Assert.NotNull(updatedUser);
                Assert.Equal("Updated Profile Name", updatedUser.FullName);
                Assert.Equal($"updated_{email}", updatedUser.Email);
                Assert.Equal("0778889999", updatedUser.PhoneNumber);
                Assert.Equal("New Renewable Park, Malabe", updatedUser.Address);
            }
            finally
            {
                await _mongoDbService.Users.DeleteOneAsync(u => u.NIC == nic);
            }
        }

        // INTEGRATION TEST 04: POST Deactivation Request Saves Reason & Flag in DB
        [Fact]
        public async Task IT04_RequestDeactivation_UpdatesUserDeactivationStatusAndReason()
        {
            // Arrange
            var randomSuffix = Guid.NewGuid().ToString("N")[..7].ToUpper();
            var nic = $"99{randomSuffix}V";
            var email = $"it_deact_{randomSuffix}@smartsolar.io";
            var password = "DeactPassword123!";

            var registerDto = new RegisterDto
            {
                NIC = nic,
                FullName = "Deactivation Candidate",
                Email = email,
                Password = password
            };

            await _authController.Register(registerDto);

            try
            {
                // Act: Submit Deactivation Request
                var deactDto = new DeactivationRequestDto
                {
                    Reason = "Relocating abroad permanently"
                };

                var deactResult = await _profileController.RequestDeactivation(nic, deactDto);
                Assert.IsType<OkObjectResult>(deactResult);

                // Assert: Check database fields
                var userInDb = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();
                Assert.NotNull(userInDb);
                Assert.True(userInDb.IsDeactivationRequested);
                Assert.Equal("Relocating abroad permanently", userInDb.DeactivationReason);
            }
            finally
            {
                await _mongoDbService.Users.DeleteOneAsync(u => u.NIC == nic);
            }
        }

        // INTEGRATION TEST 05: Internal User Lifecycle (Create, Get, Update Status, Delete)
        [Fact]
        public async Task IT05_InternalUserLifecycle_CreateGetUpdateDelete_ExecutesSuccessfully()
        {
            // Arrange
            var randomSuffix = Guid.NewGuid().ToString("N")[..7].ToUpper();
            var opNic = $"88{randomSuffix}V";
            var opEmail = $"operator_{randomSuffix}@smartsolar.io";

            var createDto = new CreateUserDto
            {
                NIC = opNic,
                FullName = "Operator Nuwan",
                Email = opEmail,
                Password = "OperatorSecretPass123!",
                Role = "grid_operator",
                BadgeId = $"BADGE-{randomSuffix}",
                AssignedHubId = "HUB-COLOMBO"
            };

            // Act 1: Create Grid Operator
            var createResult = await _userController.CreateUser(createDto);
            Assert.IsType<OkObjectResult>(createResult);

            // Act 2: Verify user appears in GetUsers list
            var getResult = await _userController.GetUsers();
            var okGet = Assert.IsType<OkObjectResult>(getResult);
            var usersEnumerable = okGet.Value as System.Collections.IEnumerable;
            Assert.NotNull(usersEnumerable);

            var found = false;
            foreach (var item in usersEnumerable)
            {
                var nicProp = item.GetType().GetProperty("nic");
                if (nicProp?.GetValue(item)?.ToString() == opNic)
                {
                    found = true;
                    break;
                }
            }
            Assert.True(found, $"Newly created operator '{opNic}' was not found in the users list.");

            // Act 3: Update User Status to Deactivated
            var statusUpdateDto = new UpdateUserStatusDto
            {
                Status = "deactivated"
            };
            var statusResult = await _userController.UpdateUserStatus(opNic, statusUpdateDto);
            Assert.IsType<OkObjectResult>(statusResult);

            var dbUser = await _mongoDbService.Users.Find(u => u.NIC == opNic).FirstOrDefaultAsync();
            Assert.NotNull(dbUser);
            Assert.Equal(AccountStatus.Deactivated, dbUser.AccountStatus);

            // Act 4: Delete Grid Operator
            var deleteResult = await _userController.DeleteUser(opNic);
            Assert.IsType<OkObjectResult>(deleteResult);

            // Assert: Verify removed from DB
            var deletedUser = await _mongoDbService.Users.Find(u => u.NIC == opNic).FirstOrDefaultAsync();
            Assert.Null(deletedUser);
        }
    }
}
