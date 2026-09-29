// ============================================================================
// File: RegisterDto.cs
// Description: Data transfer object representing register.
// Author: Member 1
// ============================================================================
using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.member1
{
    public class RegisterDto
    {
        [Required]
        public string NIC { get; set; } = null!;

        [Required]
        public string FullName { get; set; } = null!;

        [Required]
        [EmailAddress]
        public string Email { get; set; } = null!;

        [Required]
        [MinLength(6)]
        public string Password { get; set; } = null!;

        public string? PhoneNumber { get; set; }
        public string? Address { get; set; }
    }
}
