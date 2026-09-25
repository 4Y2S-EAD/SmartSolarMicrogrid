/*
File Name   : ReservationSummaryDto.cs
Description : Data Transfer Object for returning reservation summary with station and slot details

Creator     : Rathnayake R. M. S. D. (IT22140616)
*/

using System;

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
    public string? OperatorId { get; set; }
    public DateTime? VerifiedAt { get; set; }
    public DateTime? CompletedAt { get; set; }
    public string? CancellationReason { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Station & Slot metadata
    public double? StationCapacityKwh { get; set; }
    public int? BatterySlotCount { get; set; }
    public int? AvailableSlotCount { get; set; }
    public double? Latitude { get; set; }
    public double? Longitude { get; set; }
    public double? SlotCapacityKwh { get; set; }
  }
}