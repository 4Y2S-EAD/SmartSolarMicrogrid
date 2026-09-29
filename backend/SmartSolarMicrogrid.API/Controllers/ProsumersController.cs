// ============================================================================
// File: ProsumersController.cs
// Description: Handles API requests and operations for prosumers.
// Author: Member 1
// ============================================================================
using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using Microsoft.AspNetCore.Authorization;

namespace SmartSolarMicrogrid.API.Controllers
{
    [ApiController]
    [Route("api/prosumers")]
    [Authorize]
    public class ProsumersController : ControllerBase
    {
        private readonly MongoDbService _mongoDbService;

        // Initializes a new instance of the ProsumersController class.
        public ProsumersController(MongoDbService mongoDbService)
        {
            _mongoDbService = mongoDbService;
        }

        // Gets all prosumers.
        [HttpGet]
        public async Task<IActionResult> GetAllProsumers()
        {
            var prosumers = await _mongoDbService.Users
                .Find(u => u.Role == UserRole.Prosumer)
                .ToListAsync();

            var response = prosumers.Select(u => new
            {
                id = u.NIC, // map id to NIC for frontend
                nic = u.NIC,
                full_name = u.FullName,
                email = u.Email,
                phone = u.PhoneNumber,
                address = u.Address,
                status = u.AccountStatus.ToString().ToLower(),
                deactivation_requested = u.IsDeactivationRequested,
                deactivation_reason = u.DeactivationReason,
                created_at = u.CreatedAt,
                updated_at = u.UpdatedAt
            });

            return Ok(response);
        }

        // Updates prosumer status.
        [HttpPut("{nic}/status")]
        public async Task<IActionResult> UpdateProsumerStatus(string nic, [FromBody] ProsumerStatusDto dto)
        {
            var user = await _mongoDbService.Users.Find(u => u.NIC == nic).FirstOrDefaultAsync();

            if (user == null)
            {
                return NotFound(new { Message = "Prosumer not found." });
            }

            AccountStatus newStatus;
            if (dto.Status.ToLower() == "active")
            {
                newStatus = AccountStatus.Active;
            }
            else if (dto.Status.ToLower() == "deactivated")
            {
                newStatus = AccountStatus.Deactivated;
            }
            else if (dto.Status.ToLower() == "pending")
            {
                newStatus = AccountStatus.Pending;
            }
            else
            {
                return BadRequest(new { Message = "Invalid status." });
            }

            var updateDefinition = Builders<User>.Update
                .Set(u => u.AccountStatus, newStatus)
                .Set(u => u.IsApproved, newStatus == AccountStatus.Active)
                .Set(u => u.IsDeactivationRequested, false)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            await _mongoDbService.Users.UpdateOneAsync(u => u.NIC == nic, updateDefinition);

            return Ok(new { Message = "Prosumer status updated successfully." });
        }
    }
}
