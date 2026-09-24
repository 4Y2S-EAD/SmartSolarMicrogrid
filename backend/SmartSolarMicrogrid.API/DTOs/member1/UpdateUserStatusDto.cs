using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SmartSolarMicrogrid.API.DTOs.member1
{
    public class UpdateUserStatusDto
    {
        [Required]
        [JsonPropertyName("status")]
        public string Status { get; set; } = null!; // "active" or "deactivated"
    }
}
