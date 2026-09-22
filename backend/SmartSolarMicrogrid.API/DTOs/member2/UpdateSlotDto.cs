using System;

namespace SmartSolarMicrogrid.API.DTOs.member2
{
    // this class is for updating slot details 
    public class UpdateSlotDto
    {
        // ? means optional. maybe we just want to update status only
        
        // slot number can change 
        public int? SlotNumber { get; set; }
        
        // date can change
        public DateTime? BookingDate { get; set; }
        
        // time update
        public string? StartTime { get; set; }
        public string? EndTime { get; set; }
        
        // capacity update
        public double? CapacityKwh { get; set; }
        
        // update status like Available to Booked or Unavailable
        public string? Status { get; set; }
    }
}
