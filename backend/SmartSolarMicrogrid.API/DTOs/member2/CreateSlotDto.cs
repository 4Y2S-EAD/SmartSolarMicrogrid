using System;

namespace SmartSolarMicrogrid.API.DTOs.member2
{
    // we use this to add a new battery slot to a station
    public class CreateSlotDto
    {
        // number of the slot like 1, 2, 3
        public int SlotNumber { get; set; }
        
        // which date this slot is for
        public DateTime BookingDate { get; set; }
        
        // start time of slot. it must have a value
        public string StartTime { get; set; } = null!;
        
        // end time of slot
        public string EndTime { get; set; } = null!;
        
        // how much power in this slot
        public double CapacityKwh { get; set; }
        
        // by default a new slot is available for booking
        public string Status { get; set; } = "Available";
    }
}
