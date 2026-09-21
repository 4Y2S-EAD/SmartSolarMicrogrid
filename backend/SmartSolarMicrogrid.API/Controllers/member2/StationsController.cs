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


        //battery slot management part 

                // 6. CREATE A NEW BATTERY SLOT FOR A STATION
        [HttpPost("{id}/slots")]
        public async Task<IActionResult> CreateSlot(string id, [FromBody] CreateSlotDto dto)
        {
            // check if the station exists first
            var station = await _mongoDbService.SolarStations.Find(s => s.StationId == id).FirstOrDefaultAsync();
            if (station == null)
            {
                return NotFound(new { Message = "Station not found" });
            }

            var slot = new EnergyBookingSlots
            {
                StationId = id,
                SlotNumber = dto.SlotNumber,
                BookingDate = dto.BookingDate,
                StartTime = dto.StartTime,
                EndTime = dto.EndTime,
                CapacityKwh = dto.CapacityKwh,
                Status = dto.Status, // default is Available
                UpdatedAt = DateTime.UtcNow
            };

            await _mongoDbService.EnergyBookingSlots.InsertOneAsync(slot);

            return Ok(new { Message = "Battery slot created successfully", SlotId = slot.SlotId });
        }


        // 7. GET ALL SLOTS FOR A SPECIFIC STATION
        [HttpGet("{id}/slots")]
        public async Task<IActionResult> GetSlotsForStation(string id)
        {
            // find all slots where StationId matches the given id
            var slots = await _mongoDbService.EnergyBookingSlots.Find(s => s.StationId == id).ToListAsync();
            
            return Ok(slots);
        }

        // 8. UPDATE SLOT AVAILABILITY OR DETAILS
        [HttpPut("slots/{slotId}")]
        public async Task<IActionResult> UpdateSlot(string slotId, [FromBody] UpdateSlotDto dto)
        {
            var slot = await _mongoDbService.EnergyBookingSlots.Find(s => s.SlotId == slotId).FirstOrDefaultAsync();
            
            if (slot == null)
            {
                return NotFound(new { Message = "Slot not found" });
            }

            // update fields if they are sent in request
            if (dto.SlotNumber != null) slot.SlotNumber = dto.SlotNumber.Value;
            if (dto.BookingDate != null) slot.BookingDate = dto.BookingDate.Value;
            if (dto.StartTime != null) slot.StartTime = dto.StartTime;
            if (dto.EndTime != null) slot.EndTime = dto.EndTime;
            if (dto.CapacityKwh != null) slot.CapacityKwh = dto.CapacityKwh.Value;
            if (dto.Status != null) slot.Status = dto.Status; // eg: changing "Available" to "Unavailable"

            slot.UpdatedAt = DateTime.UtcNow;

            await _mongoDbService.EnergyBookingSlots.ReplaceOneAsync(s => s.SlotId == slotId, slot);

            return Ok(new { Message = "Slot updated successfully" });
        }

        // 9. DELETE A BATTERY SLOT
        [HttpDelete("slots/{slotId}")]
        public async Task<IActionResult> DeleteSlot(string slotId)
        {
            var slot = await _mongoDbService.EnergyBookingSlots.Find(s => s.SlotId == slotId).FirstOrDefaultAsync();
            
            if (slot == null)
            {
                return NotFound(new { Message = "Slot not found" });
            }

            // BUSINESS RULE: Cannot delete a slot if it is already booked
            if (slot.Status == "Booked")
            {
                return BadRequest(new { Message = "Cannot delete this slot because it is already booked by a prosumer." });
            }

            // if it is not booked, we can delete it
            await _mongoDbService.EnergyBookingSlots.DeleteOneAsync(s => s.SlotId == slotId);

            return Ok(new { Message = "Battery slot deleted successfully" });
        }


    }
}
