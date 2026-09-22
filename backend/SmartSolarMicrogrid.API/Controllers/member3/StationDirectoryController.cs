using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using System.Linq;
using System.Threading.Tasks;

namespace SmartSolarMicrogrid.API.Controllers.member3
{
  [ApiController]
  [Route("api/member3/stations")]
  public class StationDirectoryController : ControllerBase
  {
    private readonly MongoDbService _mongoDbService;

    public StationDirectoryController(MongoDbService mongoDbService)
    {
      _mongoDbService = mongoDbService;
    }


    // Fetch all active solar stations (GET: api/stations)
    [HttpGet]
    public async Task<IActionResult> GetAllActiveStations()
    {
      var stations = await _mongoDbService.SolarStations
          .Find(s => s.Status.ToLower() == "active")
          .ToListAsync();

      var response = stations.Select(s => new
      {
        stationId = s.StationId,
        stationName = s.StationName,
        latitude = s.Location.Latitude,
        longitude = s.Location.Longitude,
        capacityKwh = s.CapacityKwh,
        batterySlotCount = s.BatterySlotCount,
        availableSlotCount = s.AvailableSlotCount,
        status = s.Status,
        schedule = s.Schedule
      });

      return Ok(response);
    }

    // Fetch available booking slots for a selected station (GET: api/stations/{stationId}/slots)
    [HttpGet("{stationId}/slots")]
    public async Task<IActionResult> GetAvailableSlotsByStation(string stationId)
    {
      var slots = await _mongoDbService.EnergyBookingSlots
          .Find(s => s.StationId == stationId && s.Status.ToLower() == "available")
          .SortBy(s => s.BookingDate)
          .ThenBy(s => s.StartTime)
          .ToListAsync();

      var response = slots.Select(s => new
      {
        slotId = s.SlotId,
        stationId = s.StationId,
        slotNumber = s.SlotNumber,
        bookingDate = s.BookingDate,
        startTime = s.StartTime,
        endTime = s.EndTime,
        capacityKwh = s.CapacityKwh,
        status = s.Status
      });

      return Ok(response);
    }
  }
}