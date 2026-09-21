using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member2;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers.member2
{
    // tell .net this is an api controller
    [ApiController]
    // route is how we access this api. ex: http://localhost:5000/api/stations
    [Route("api/stations")]
    public class StationsController : ControllerBase
    {
        // we need mongodb service to talk to database
        private readonly MongoDbService _mongoDbService;

        // constructor to get mongodb service
        public StationsController(MongoDbService mongoDbService)
        {
            _mongoDbService = mongoDbService;
        }

        // 1. CREATE A NEW MICROGRID NODE
        [HttpPost]
        public async Task<IActionResult> CreateStation([FromBody] CreateStationDto dto)
        {
            // make a new solar station info object from dto data
            var station = new SolarStationInfo
            {
                StationName = dto.StationName,
                // set map location
                Location = new GeoLocation { Latitude = dto.Latitude, Longitude = dto.Longitude },
                CapacityKwh = dto.CapacityKwh,
                BatterySlotCount = dto.BatterySlotCount,
                AvailableSlotCount = dto.BatterySlotCount, // when creating, all slots are available
                Status = dto.Status,
                Schedule = dto.Schedule,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            // save to database table
            await _mongoDbService.SolarStations.InsertOneAsync(station);

            // return 200 OK message
            return Ok(new { Message = "Microgrid node created successfully", StationId = station.StationId });
        }

        // 2. GET ALL MICROGRID NODES
        [HttpGet]
        public async Task<IActionResult> GetAllStations()
        {
            // get all stations from database without any filter
            var stations = await _mongoDbService.SolarStations.Find(_ => true).ToListAsync();
            
            // return the list of stations
            return Ok(stations);
        }

                // 3. GET SINGLE STATION BY ID
        [HttpGet("{id}")]
        public async Task<IActionResult> GetStationById(string id)
        {
            // find station where station id matches
            var station = await _mongoDbService.SolarStations.Find(s => s.StationId == id).FirstOrDefaultAsync();
            
            if (station == null)
            {
                return NotFound(new { Message = "Station not found" });
            }
            
            return Ok(station);
        }

        // 4. UPDATE A STATION
        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateStation(string id, [FromBody] UpdateStationDto dto)
        {
            // first check if station exists
            var station = await _mongoDbService.SolarStations.Find(s => s.StationId == id).FirstOrDefaultAsync();
            
            if (station == null)
            {
                return NotFound(new { Message = "Station not found" });
            }

            // update only if they send a new value (that is why we use != null)
            if (dto.StationName != null) station.StationName = dto.StationName;
            
            if (dto.Latitude != null && dto.Longitude != null)
            {
                station.Location = new GeoLocation { Latitude = dto.Latitude.Value, Longitude = dto.Longitude.Value };
            }
            
            if (dto.CapacityKwh != null) station.CapacityKwh = dto.CapacityKwh.Value;
            if (dto.BatterySlotCount != null) station.BatterySlotCount = dto.BatterySlotCount.Value;
            if (dto.Status != null) station.Status = dto.Status;
            if (dto.Schedule != null) station.Schedule = dto.Schedule;
            
            station.UpdatedAt = DateTime.UtcNow;

            // save updated station to database
            await _mongoDbService.SolarStations.ReplaceOneAsync(s => s.StationId == id, station);
            
            return Ok(new { Message = "Station updated successfully" });
        }

        // 5. DEACTIVATE STATION (IMPORTANT RULE HERE)
        [HttpDelete("{id}")]
        public async Task<IActionResult> DeactivateStation(string id)
        {
            var station = await _mongoDbService.SolarStations.Find(s => s.StationId == id).FirstOrDefaultAsync();
            
            if (station == null)
            {
                return NotFound(new { Message = "Station not found" });
            }

            // BUSINESS RULE: cannot deactivate if active reservations exist
            // check in EnergyReservations collection
            var activeReservations = await _mongoDbService.EnergyReservations
                .Find(r => r.StationId == id && r.BookingStatus == "Active")
                .ToListAsync();

            if (activeReservations.Count > 0)
            {
                // block deactivation
                return BadRequest(new { Message = "Cannot deactivate station. There are active energy reservations." });
            }

            // if no active reservations, we can deactivate
            station.Status = "Inactive";
            station.UpdatedAt = DateTime.UtcNow;

            await _mongoDbService.SolarStations.ReplaceOneAsync(s => s.StationId == id, station);

            return Ok(new { Message = "Station deactivated successfully" });
        }

    }
}
