using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member1;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers.member1
{
    [ApiController]
    [Route("api/member1/profile")]
    public class ProsumerProfileController : ControllerBase
    {
        private readonly MongoDbService _mongoDbService;

        public ProsumerProfileController(MongoDbService mongoDbService)
        {
            _mongoDbService = mongoDbService;
        }

        [HttpGet("{nic}")]
        public async Task<IActionResult> GetProfile(string nic)
        {
            var user = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();

            if (user == null)
            {
                return NotFound(new { Message = "User not found." });
            }

            // Return user details except password hash
            return Ok(new
            {
                user.NIC,
                user.FullName,
                user.Email,
                user.PhoneNumber,
                user.Address,
                user.Role,
                user.AccountStatus,
                user.IsApproved,
                user.CreatedAt,
                user.UpdatedAt,
                user.DeactivatedAt
            });
        }

        [HttpPut("{nic}")]
        public async Task<IActionResult> UpdateProfile(string nic, [FromBody] UpdateProfileDto dto)
        {
            var user = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();

            if (user == null)
            {
                return NotFound(new { Message = "User not found." });
            }

            var updateDefinition = Builders<User>.Update
                .Set(u => u.FullName, dto.FullName)
                .Set(u => u.Email, dto.Email)
                .Set(u => u.PhoneNumber, dto.PhoneNumber)
                .Set(u => u.Address, dto.Address)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            await _mongoDbService.Users.UpdateOneAsync(u => u.NIC == nic, updateDefinition);

            return Ok(new { Message = "Profile updated successfully." });
        }

        [HttpPost("{nic}/deactivate")]
        public async Task<IActionResult> RequestDeactivation(string nic)
        {
            var user = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();

            if (user == null)
            {
                return NotFound(new { Message = "User not found." });
            }

            // Assuming a request deactivation marks the account as pending deactivation or deactivates it directly.
            // Based on standard flows, we mark it Deactivated or create a deactivation request.
            // Let's set it to deactivated here.
            var updateDefinition = Builders<User>.Update
                .Set(u => u.AccountStatus, AccountStatus.Deactivated)
                .Set(u => u.DeactivatedAt, DateTime.UtcNow)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            await _mongoDbService.Users.UpdateOneAsync(u => u.NIC == nic, updateDefinition);

            return Ok(new { Message = "Account deactivated." });
        }
    }
}
