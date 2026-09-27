using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using System;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using System.Threading.Tasks;

namespace SmartSolarMicrogrid.API.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class AuthController : ControllerBase
    {
        private readonly MongoDbService _mongoDbService;
        private readonly IConfiguration _configuration;

        public AuthController(MongoDbService mongoDbService, IConfiguration configuration)
        {
            _mongoDbService = mongoDbService;
            _configuration = configuration;
        }

        public class RegisterRequest
        {
            public string NIC { get; set; } = null!;
            public string FullName { get; set; } = null!;
            public string Email { get; set; } = null!;
            public string Password { get; set; } = null!;
            public UserRole Role { get; set; }
        }

        [HttpPost("register")]
        public async Task<IActionResult> Register([FromBody] RegisterRequest request)
        {
            var existingUser = await _mongoDbService.Users.Find(u => u.NIC == request.NIC || u.Email == request.Email).FirstOrDefaultAsync();
            if (existingUser != null)
            {
                return BadRequest(new { message = "User with this NIC or Email already exists." });
            }

            // In a real application, use a proper hashing library like BCrypt.Net-Next.
            // Using a simple placeholder hash logic here, but ASP.NET hashing via Identity can also be used if configured.
            var passwordHash = BCrypt.Net.BCrypt.HashPassword(request.Password);

            var newUser = new User
            {
                NIC = request.NIC,
                FullName = request.FullName,
                Email = request.Email,
                PasswordHash = passwordHash,
                Role = request.Role,
                AccountStatus = AccountStatus.Active // Defaulting to active for ease of testing
            };

            await _mongoDbService.Users.InsertOneAsync(newUser);

            return Ok(new { message = "Registration successful" });
        }

        public class LoginRequest
        {
            public string Email { get; set; } = null!;
            public string Password { get; set; } = null!;
        }

        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] LoginRequest request)
        {
            var user = await _mongoDbService.Users.Find(u => u.Email == request.Email).FirstOrDefaultAsync();
            
            if (user == null || !BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
            {
                return Unauthorized(new { message = "Invalid email or password." });
            }

            if (user.AccountStatus != AccountStatus.Active)
            {
                return Unauthorized(new { message = "Account is not active." });
            }

            var token = GenerateJwtToken(user);

            return Ok(new
            {
                token,
                user = new
                {
                    id = user.NIC, // Frontend uses 'id' in its mock
                    nic = user.NIC,
                    email = user.Email,
                    fullName = user.FullName,
                    role = user.Role.ToString().ToLower()
                }
            });
        }

        private string GenerateJwtToken(User user)
        {
            var jwtSettings = _configuration.GetSection("JwtSettings");
            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSettings["Secret"] ?? string.Empty));
            var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            var claimsList = new List<Claim>
            {
                new Claim(JwtRegisteredClaimNames.Sub, user.NIC),
                new Claim(JwtRegisteredClaimNames.Email, user.Email),
                new Claim("role", user.Role.ToString().ToLower()),
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
            };
            // Include assignedHubId so hub-scoped controllers can filter without an extra DB query.
            if (!string.IsNullOrWhiteSpace(user.AssignedHubId))
                claimsList.Add(new Claim("assignedHubId", user.AssignedHubId));
            var claims = claimsList;

            var token = new JwtSecurityToken(
                issuer: jwtSettings["Issuer"],
                audience: jwtSettings["Audience"],
                claims: claims,
                expires: DateTime.UtcNow.AddMinutes(double.Parse(jwtSettings["ExpiryMinutes"] ?? "60")),
                signingCredentials: creds
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
}
