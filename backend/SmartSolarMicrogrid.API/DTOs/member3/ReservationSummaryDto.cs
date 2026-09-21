using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.member3
{
  public class ReservationSummaryDto
  {
    public string ReservationId { get; set; } = null!;
    public string ProsumerNic { get; set; } = null!;
    public string StationId { get; set; } = null!;
    public string StationName { get; set; } = null!;
    public string SlotId { get; set; } = null!;
    public int SlotNumber { get; set; }
    public DateTime BookingDate { get; set; }
    public string StartTime { get; set; } = null!;
    public string EndTime { get; set; } = null!;
    public string Status { get; set; } = null!;
    public string? QrToken { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
  }
}