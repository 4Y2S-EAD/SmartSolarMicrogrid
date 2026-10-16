using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Controllers;
using SmartSolarMicrogrid.API.Controllers.member1;
using SmartSolarMicrogrid.API.DTOs.member1;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using Xunit;

namespace SmartSolarMicrogrid.Tests
{
    public class Member1IntegrationTests : IAsyncLifetime
    {
        private readonly MongoDbService _mongoDbService;
        private readonly IConfiguration _configuration;
        private readonly List<string> _createdTestNics = new();

        public Member1IntegrationTests()
        {
            var connectionString = Environment.GetEnvironmentVariable("MONGODB_CONNECTION_STRING")
                ?? "mongodb+srv://nerandadilhara_db_user:C1U79LkKZApQzeVq@cluster0.nac5zjh.mongodb.net/?appName=Cluster0";
            var databaseName = Environment.GetEnvironmentVariable("MONGODB_DATABASE_NAME") ?? "SolarMicrogridDB";

            _configuration = new ConfigurationBuilder()
                .AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["JwtSettings:Secret"] = "SuperSecretKeyThatIsAtLeast32BytesLongForJWTAuthentication1234!!",
                    ["JwtSettings:Issuer"] = "SmartSolarMicrogrid",
                    ["JwtSettings:Audience"] = "SmartSolarMicrogridUI",
                    ["JwtSettings:ExpiryMinutes"] = "1440",
                    ["MongoDbSettings:ConnectionString"] = connectionString,
                    ["MongoDbSettings:DatabaseName"] = databaseName
                })
                .Build();

            _mongoDbService = new MongoDbService(_configuration);
        }

        public Task InitializeAsync() => Task.CompletedTask;

        // Cleanup created test documents from MongoDB after test run
        public async Task DisposeAsync()
        {
            foreach (var nic in _createdTestNics)
            {
                await _mongoDbService.Users.DeleteOneAsync(u => u.NIC == nic);
            }
        }

        // ---------------------------------------------------------------------
        // Integration Test 1: Prosumer Registration & Database Persistence
        // ---------------------------------------------------------------------
        [Fact]
        public async Task IT01_RegisterProsumer_SavesUserToDatabaseWithPendingStatus()
        {
            // Arrange
            var controller = new ProsumerAuthController(_mongoDbService, _configuration);
            string testNic = "INT_PRO_" + DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            _createdTestNics.Add(testNic);

            var registerDto = new RegisterDto
            {
                NIC = testNic,
                FullName = "Integration Test Prosumer",
                Email = $"prosumer_{testNic}@microgrid.io",
                Password = "Password123!",
                PhoneNumber = "0771234567",
                Address = "45 Solar Way, Colombo"
            };

            // Act
            var result = await controller.Register(registerDto);

            // Assert HTTP response
            var okResult = Assert.IsType<OkObjectResult>(result);
            Assert.NotNull(okResult.Value);

            // Assert Database state directly in MongoDB
            var savedUser = await _mongoDbService.Users.Find(u => u.NIC == testNic).FirstOrDefaultAsync();
            Assert.NotNull(savedUser);
            Assert.Equal("Integration Test Prosumer", savedUser.FullName);
            Assert.Equal(UserRole.Prosumer, savedUser.Role);
            Assert.Equal(AccountStatus.Pending, savedUser.AccountStatus);
            Assert.False(savedUser.IsApproved);
        }

        // ---------------------------------------------------------------------
        // Integration Test 2: User Login & JWT Token Generation
        // ---------------------------------------------------------------------
        [Fact]
        public async Task IT02_AuthenticateUser_VerifiesCredentialsAndReturnsJwtToken()
        {
            // Arrange - Create an active user directly
            var authController = new AuthController(_mongoDbService, _configuration);
            string testNic = "INT_ADM_" + DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            string email = $"admin_{testNic}@microgrid.io";
            string password = "Password123!";
            _createdTestNics.Add(testNic);

            var registerReq = new AuthController.RegisterRequest
            {
                NIC = testNic,
                FullName = "Admin Test User",
                Email = email,
                Password = password,
                Role = UserRole.Backoffice
            };
            await authController.Register(registerReq);

            // Act - Perform login
            var loginReq = new AuthController.LoginRequest
            {
                Email = email,
                Password = password
            };
            var result = await authController.Login(loginReq);

            // Assert
            var okResult = Assert.IsType<OkObjectResult>(result);
            Assert.NotNull(okResult.Value);

            // Verify JWT Token string is present
            dynamic val = okResult.Value;
            var tokenProperty = okResult.Value.GetType().GetProperty("token");
            Assert.NotNull(tokenProperty);
            string? token = tokenProperty.GetValue(okResult.Value) as string;
            Assert.False(string.IsNullOrWhiteSpace(token));
        }

        // ---------------------------------------------------------------------
        // Integration Test 3: Create Internal Operator User via UserManagement
        // ---------------------------------------------------------------------
        [Fact]
        public async Task IT03_CreateInternalOperator_AddsGridOperatorWithAssignedHub()
        {
            // Arrange
            var userController = new UserManagementController(_mongoDbService);
            string testNic = "INT_OP_" + DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            _createdTestNics.Add(testNic);

            var createDto = new CreateUserDto
            {
                NIC = testNic,
                FullName = "Samantha Grid Operator",
                Email = $"operator_{testNic}@microgrid.io",
                Password = "Password123!",
                Role = "grid_operator",
                BadgeId = "BADGE-999",
                AssignedHubId = "HUB-WEST"
            };

            // Act
            var result = await userController.CreateUser(createDto);

            // Assert
            Assert.IsType<OkObjectResult>(result);

            // Verify in MongoDB
            var savedOperator = await _mongoDbService.Users.Find(u => u.NIC == testNic).FirstOrDefaultAsync();
            Assert.NotNull(savedOperator);
            Assert.Equal(UserRole.GridOperator, savedOperator.Role);
            Assert.Equal("BADGE-999", savedOperator.BadgeId);
            Assert.Equal("HUB-WEST", savedOperator.AssignedHubId);
            Assert.Equal(AccountStatus.Active, savedOperator.AccountStatus);
            Assert.True(savedOperator.IsApproved);
        }

        // ---------------------------------------------------------------------
        // Integration Test 4: Update User Status (Deactivate / Activate)
        // ---------------------------------------------------------------------
        [Fact]
        public async Task IT04_UpdateUserStatus_TogglesAccountBetweenActiveAndDeactivated()
        {
            // Arrange
            var userController = new UserManagementController(_mongoDbService);
            string testNic = "INT_STATUS_" + DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            _createdTestNics.Add(testNic);

            var createDto = new CreateUserDto
            {
                NIC = testNic,
                FullName = "Status Toggle User",
                Email = $"toggle_{testNic}@microgrid.io",
                Password = "Password123!",
                Role = "grid_operator"
            };
            await userController.CreateUser(createDto);

            // Act - Deactivate status
            var statusDto = new UpdateUserStatusDto { Status = "deactivated" };
            var updateResult = await userController.UpdateUserStatus(testNic, statusDto);

            // Assert
            Assert.IsType<OkObjectResult>(updateResult);

            var updatedUser = await _mongoDbService.Users.Find(u => u.NIC == testNic).FirstOrDefaultAsync();
            Assert.NotNull(updatedUser);
            Assert.Equal(AccountStatus.Deactivated, updatedUser.AccountStatus);
        }

        // ---------------------------------------------------------------------
        // Integration Test 5: Prosumer Profile & Deactivation Request
        // ---------------------------------------------------------------------
        [Fact]
        public async Task IT05_ProsumerProfile_GetAndRequestDeactivation_UpdatesDatabaseRecord()
        {
            // Arrange
            var authController = new ProsumerAuthController(_mongoDbService, _configuration);
            var profileController = new ProsumerProfileController(_mongoDbService);
            string testNic = "INT_PROF_" + DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            _createdTestNics.Add(testNic);

            var registerDto = new RegisterDto
            {
                NIC = testNic,
                FullName = "Profile Test Prosumer",
                Email = $"prosumer_{testNic}@microgrid.io",
                Password = "Password123!",
                PhoneNumber = "0770001122",
                Address = "12 Main St"
            };
            await authController.Register(registerDto);

            // Act 1: Get profile
            var getResult = await profileController.GetProfile(testNic);
            Assert.IsType<OkObjectResult>(getResult);

            // Act 2: Submit deactivation request
            var deactDto = new DeactivationRequestDto
            {
                Reason = "Relocating abroad permanently"
            };
            var deactResult = await profileController.RequestDeactivation(testNic, deactDto);

            // Assert
            Assert.IsType<OkObjectResult>(deactResult);

            // Verify deactivation request updated in DB
            var dbUser = await _mongoDbService.Users.Find(u => u.NIC == testNic).FirstOrDefaultAsync();
            Assert.NotNull(dbUser);
            Assert.True(dbUser.IsDeactivationRequested);
            Assert.Equal("Relocating abroad permanently", dbUser.DeactivationReason);
        }
    }
}
