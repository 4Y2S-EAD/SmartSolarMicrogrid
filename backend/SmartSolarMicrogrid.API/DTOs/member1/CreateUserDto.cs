// ============================================================================
// File: CreateUserDto.cs
// Description: Data transfer object representing create user.
// Author: Member 1
// ============================================================================
using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SmartSolarMicrogrid.API.DTOs.member1
{
    public class CreateUserDto
    {
        [Required]
        [JsonPropertyName("nic")]
        public string NIC { get; set; } = null!;

        [Required]
        [JsonPropertyName("full_name")]
        public string FullName { get; set; } = null!;

        [Required]
        [EmailAddress]
        [JsonPropertyName("email")]
        public string Email { get; set; } = null!;

        [Required]
        [JsonPropertyName("password")]
        public string Password { get; set; } = null!;

        [Required]
        [JsonPropertyName("role")]
        public string Role { get; set; } = null!; // "backoffice" or "grid_operator"

        [JsonPropertyName("badge_id")]
        public string? BadgeId { get; set; }

        [JsonPropertyName("assigned_hub_id")]
        public string? AssignedHubId { get; set; }
    }
}
