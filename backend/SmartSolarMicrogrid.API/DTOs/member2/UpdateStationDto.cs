namespace SmartSolarMicrogrid.API.DTOs.member2
{
    // this is used to update station details later
    public class UpdateStationDto
    {
        // all variables have ? mark because they are optional
        // we only send what we want to update
        
        // station name can be changed
        public string? StationName { get; set; }
        
        // map location update
        public double? Latitude { get; set; }
        public double? Longitude { get; set; }
        
        // capacity update if they upgrade station
        public double? CapacityKwh { get; set; }
        
        // slot count can change
        public int? BatterySlotCount { get; set; }
        
        // active or inactive status
        public string? Status { get; set; }
        
        // update the schedule times
        public string? Schedule { get; set; }
    }
}
