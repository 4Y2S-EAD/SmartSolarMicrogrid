using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member1;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace SmartSolarMicrogrid.API.Controllers.member1
{
    [ApiController]
    [Route("api/member1/auth")]
    public class ProsumerAuthController : ControllerBase
    {
        private readonly MongoDbService _mongoDbService;
        private readonly IConfiguration _configuration;

        public ProsumerAuthController(MongoDbService mongoDbService, IConfiguration configuration)
        {
            _mongoDbService = mongoDbService;
            _configuration = configuration;
        }

        [HttpPost("register")]
        public async Task<IActionResult> Register([FromBody] RegisterDto dto)
        {
            var existingUser = await _mongoDbService.Users.Find(u => u.NIC == dto.NIC).FirstOrDefaultAsync();
            if (existingUser != null)
            {
                return BadRequest(new { Message = "User with this NIC already exists." });
            }

            var user = new User
            {
                NIC = dto.NIC,
                FullName = dto.FullName,
                Email = dto.Email,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password),
                PhoneNumber = dto.PhoneNumber,
                Address = dto.Address,
                Role = UserRole.Prosumer,
                AccountStatus = AccountStatus.Pending,
                IsApproved = false,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            await _mongoDbService.Users.InsertOneAsync(user);

            return Ok(new { Message = "Registration submitted successfully. Pending activation." });
        }

        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] LoginDto dto)
        {
            var user = await _mongoDbService.Users.Find(u => u.NIC == dto.NIC).FirstOrDefaultAsync();

            if (user == null || !BCrypt.Net.BCrypt.Verify(dto.Password, user.PasswordHash))
            {
                return Unauthorized(new { Message = "Invalid credentials." });
            }

            if (user.AccountStatus == AccountStatus.Deactivated)
            {
                return Unauthorized(new { Message = "Account is deactivated." });
            }

            var token = GenerateJwtToken(user);

            return Ok(new
            {
                Token = token,
                User = new
                {
                    user.NIC,
                    user.FullName,
                    user.Email,
                    user.Role,
                    user.AccountStatus
                }
            });
        }

        private string GenerateJwtToken(User user)
        {
            var jwtSettings = _configuration.GetSection("JwtSettings");
            var secretKey = jwtSettings["Secret"];
            if (string.IsNullOrEmpty(secretKey))
            {
                throw new InvalidOperationException("JWT Secret is not configured.");
            }

            var claims = new[]
            {
                new Claim(JwtRegisteredClaimNames.Sub, user.NIC),
                new Claim(JwtRegisteredClaimNames.Email, user.Email),
                new Claim(ClaimTypes.Role, user.Role.ToString()),
                new Claim("AccountStatus", user.AccountStatus.ToString())
            };

            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secretKey));
            var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            var token = new JwtSecurityToken(
                issuer: jwtSettings["Issuer"],
                audience: jwtSettings["Audience"],
                claims: claims,
                expires: DateTime.UtcNow.AddHours(2),
                signingCredentials: creds
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
}
