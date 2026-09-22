/*
File Name   : CreateReservationDto.cs
Description : Data Transfer Object for creating an energy slot reservation

Creator     : Rathnayake R. M. S. D. (IT22140616)
*/


using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.member3
{
  public class CreateReservationDto
  {
    [Required(ErrorMessage = "Prosumer NIC is required.")]
    [RegularExpression(@"^[0-9]{9}[V]$|^[0-9]{12}$", ErrorMessage = "NIC must be either 9 digits followed by V or 12 digits.")]
    public string ProsumerNic { get; set; } = null!;

    [Required(ErrorMessage = "Station id is required.")]
    public string StationId { get; set; } = null!;

    [Required(ErrorMessage = "Slot id is required.")]
    public string SlotId { get; set; } = null!;

    [Required(ErrorMessage = "Booking date is required.")]
    public DateTime BookingDate { get; set; }

    [Required(ErrorMessage = "Start time is required.")]
    [RegularExpression(@"^(0?[1-9]|1[0-2]):[0-5][0-9]\s?(AM|PM)$", ErrorMessage = "Start time must be in 12-hour format")]
    public string StartTime { get; set; } = null!;

    [Required(ErrorMessage = "End time is required.")]
    [RegularExpression(@"^(0?[1-9]|1[0-2]):[0-5][0-9]\s?(AM|PM)$", ErrorMessage = "End time must be in 12-hour format")]
    public string EndTime { get; set; } = null!;
  }
}


/*
ReservationId

ProsumerNic
StationId
SlotId
BookingDate [DateTime]
StartTime
EndTime
Status - Pending (auto)

QrToken
QrGeneratedAt [DateTime]
OperatorId
VerifiedAt  [DateTime]
CompletedAt [DateTime]
CancellationReason

CreatedAt [DateTime]
UpdatedAt [DateTime]
*/