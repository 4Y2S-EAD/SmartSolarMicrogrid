/*
File Name   : AvailableTimeSlotDto.cs
Description : Data Transfer Object representing an available 2 hour battery time slot
Creator     : Rathnayake R. M. S. D. (IT22140616)
*/

namespace SmartSolarMicrogrid.API.DTOs.member3
{
  public class AvailableTimeSlotDto
  {
    public string Label { get; set; } = null!;
    public string StartTime { get; set; } = null!;
    public string EndTime { get; set; } = null!;
  }
}
