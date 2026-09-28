using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member1;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using Microsoft.AspNetCore.Authorization;

namespace SmartSolarMicrogrid.API.Controllers.member1
{
    [ApiController]
    [Route("api/member1/profile")]
    [Authorize]
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
                user.IsDeactivationRequested,
                user.DeactivationReason,
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

        [HttpPost("{nic}/deactivation-request")]
        public async Task<IActionResult> RequestDeactivation(string nic, [FromBody] DeactivationRequestDto dto)
        {
            var user = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();

            if (user == null)
            {
                return NotFound(new { Message = "User not found." });
            }

            var updateDefinition = Builders<User>.Update
                .Set(u => u.IsDeactivationRequested, true)
                .Set(u => u.DeactivationReason, dto.Reason)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            await _mongoDbService.Users.UpdateOneAsync(u => u.NIC == nic, updateDefinition);

            return Ok(new { Message = "Deactivation request submitted for review." });
        }
    }
}
