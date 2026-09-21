using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.member3
{
  public class CancelReservationDto
  {
    [Required(ErrorMessage = "Cancellation reason is required.")]
    [MaxLength(200, ErrorMessage = "Cancellation reason cannot exceed 200 characters.")]
    public string? CancellationReason { get; set; }
  }
}