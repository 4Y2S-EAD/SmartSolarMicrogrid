// ============================================================================
// File: UpdateUserDto.cs
// Description: Data transfer object representing update user.
// Author: Member 1
// ============================================================================
using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SmartSolarMicrogrid.API.DTOs.member1
{
    public class UpdateUserDto
    {
        [Required]
        [JsonPropertyName("full_name")]
        public string FullName { get; set; } = null!;

        [JsonPropertyName("badge_id")]
        public string? BadgeId { get; set; }

        [JsonPropertyName("assigned_hub_id")]
        public string? AssignedHubId { get; set; }
    }
}
