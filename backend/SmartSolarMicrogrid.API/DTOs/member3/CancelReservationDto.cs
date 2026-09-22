/*
File Name   : CancelReservationDto.cs
Description : Data Transfer Object containing reason for cancelling an existing reservation

Creator     : Rathnayake R. M. S. D. (IT22140616)
*/

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