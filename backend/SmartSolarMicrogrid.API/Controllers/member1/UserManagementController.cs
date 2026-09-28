using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member1;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using Microsoft.AspNetCore.Authorization;

namespace SmartSolarMicrogrid.API.Controllers.member1
{
    [ApiController]
    [Route("api/member1/users")]
    [Authorize]
    public class UserManagementController : ControllerBase
    {
        private readonly MongoDbService _mongoDbService;

        public UserManagementController(MongoDbService mongoDbService)
        {
            _mongoDbService = mongoDbService;
        }

        [HttpGet]
        public async Task<IActionResult> GetUsers()
        {
            var users = await _mongoDbService.Users.Find(u => u.Role == UserRole.Backoffice || u.Role == UserRole.GridOperator).ToListAsync();

            var mappedProfiles = users.Select(u => new {
                id = u.NIC, 
                nic = u.NIC,
                email = u.Email,
                full_name = u.FullName,
                role = u.Role == UserRole.GridOperator ? "grid_operator" : "backoffice",
                status = u.AccountStatus.ToString().ToLower(),
                badge_id = u.BadgeId,
                assigned_hub_id = u.AssignedHubId,
                created_at = u.CreatedAt,
                updated_at = u.UpdatedAt
            });

            return Ok(mappedProfiles);
        }

        [HttpPost]
        public async Task<IActionResult> CreateUser([FromBody] CreateUserDto dto)
        {
            var existingUser = await _mongoDbService.Users.Find(u => u.NIC == dto.NIC || u.Email == dto.Email).FirstOrDefaultAsync();
            if (existingUser != null)
            {
                return BadRequest(new { Message = "User with this NIC or Email already exists." });
            }

            var userRole = dto.Role.ToLower() == "grid_operator" ? UserRole.GridOperator : UserRole.Backoffice;

            var newUser = new User
            {
                NIC = dto.NIC,
                FullName = dto.FullName,
                Email = dto.Email,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password),
                Role = userRole,
                AccountStatus = AccountStatus.Active,
                IsApproved = true,
                BadgeId = userRole == UserRole.GridOperator ? dto.BadgeId : null,
                AssignedHubId = userRole == UserRole.GridOperator ? dto.AssignedHubId : null
            };

            await _mongoDbService.Users.InsertOneAsync(newUser);

            return Ok(new { Message = "User created successfully." });
        }

        [HttpPut("{nic}")]
        public async Task<IActionResult> UpdateUser(string nic, [FromBody] UpdateUserDto dto)
        {
            var user = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();
            if (user == null)
            {
                return NotFound(new { Message = "User not found." });
            }

            var updateDefinition = Builders<User>.Update
                .Set(u => u.FullName, dto.FullName)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            if (user.Role == UserRole.GridOperator)
            {
                updateDefinition = updateDefinition
                    .Set(u => u.BadgeId, dto.BadgeId)
                    .Set(u => u.AssignedHubId, dto.AssignedHubId);
            }

            await _mongoDbService.Users.UpdateOneAsync(u => u.NIC == nic, updateDefinition);

            return Ok(new { Message = "User updated successfully." });
        }

        [HttpPut("{nic}/status")]
        public async Task<IActionResult> UpdateUserStatus(string nic, [FromBody] UpdateUserStatusDto dto)
        {
            var user = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();
            if (user == null)
            {
                return NotFound(new { Message = "User not found." });
            }

            var newStatus = dto.Status.ToLower() == "active" ? AccountStatus.Active : AccountStatus.Deactivated;

            var updateDefinition = Builders<User>.Update
                .Set(u => u.AccountStatus, newStatus)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            await _mongoDbService.Users.UpdateOneAsync(u => u.NIC == nic, updateDefinition);

            return Ok(new { Message = $"User status updated to {newStatus}." });
        }

        [HttpDelete("{nic}")]
        public async Task<IActionResult> DeleteUser(string nic)
        {
            var user = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();
            if (user == null)
            {
                return NotFound(new { Message = "User not found." });
            }

            await _mongoDbService.Users.DeleteOneAsync(u => u.NIC == nic);

            return Ok(new { Message = "User deleted successfully." });
        }
    }
}
