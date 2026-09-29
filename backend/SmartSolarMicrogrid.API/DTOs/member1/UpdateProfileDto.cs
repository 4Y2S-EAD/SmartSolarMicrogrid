// ============================================================================
// File: UpdateProfileDto.cs
// Description: Data transfer object representing update profile.
// Author: Member 1
// ============================================================================
using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.member1
{
    public class UpdateProfileDto
    {
        [Required]
        public string FullName { get; set; } = null!;

        [Required]
        [EmailAddress]
        public string Email { get; set; } = null!;

        public string? PhoneNumber { get; set; }
        public string? Address { get; set; }
    }
}
