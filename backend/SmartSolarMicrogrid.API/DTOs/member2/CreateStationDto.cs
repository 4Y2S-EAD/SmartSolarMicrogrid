namespace SmartSolarMicrogrid.API.DTOs.member2
{
    // this class is for get data when we create a new solar station
    public class CreateStationDto
    {
        // name of the station, it cant be null
        public string StationName { get; set; } = null!;
        
        // for map gps location
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        
        // total capacity of the station
        public double CapacityKwh { get; set; }
        
        // how many battery slots this station have
        public int BatterySlotCount { get; set; }
        
        // status is active when created by default
        public string Status { get; set; } = "Active";
        
        // schedule is optional so put ? 
        public string? Schedule { get; set; }
    }
}
