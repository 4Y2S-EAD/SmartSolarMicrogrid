// ============================================================================
// File: ProsumerStatusDto.cs
// Description: Data transfer object representing prosumer status.
// Author: Member 1
// ============================================================================
using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs
{
    public class ProsumerStatusDto
    {
        [Required]
        public string Status { get; set; } = null!;
    }
}
