using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs
{
    public class ProsumerStatusDto
    {
        [Required]
        public string Status { get; set; } = null!;
    }
}
