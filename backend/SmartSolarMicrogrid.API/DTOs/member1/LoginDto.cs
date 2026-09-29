// ============================================================================
// File: LoginDto.cs
// Description: Data transfer object representing login.
// Author: Member 1
// ============================================================================
using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.member1
{
    public class LoginDto
    {
        [Required]
        public string NIC { get; set; } = null!;

        [Required]
        public string Password { get; set; } = null!;
    }
}
