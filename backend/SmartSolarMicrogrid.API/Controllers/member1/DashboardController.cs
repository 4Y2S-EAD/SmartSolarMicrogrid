using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using Microsoft.AspNetCore.Authorization;

namespace SmartSolarMicrogrid.API.Controllers.member1
{
    [ApiController]
    [Route("api/member1/dashboard")]
    [Authorize]
    public class DashboardController : ControllerBase
    {
        private readonly MongoDbService _mongoDbService;

        public DashboardController(MongoDbService mongoDbService)
        {
            _mongoDbService = mongoDbService;
        }

        [HttpGet("recent-reservations")]
        public async Task<IActionResult> GetRecentReservations()
        {
            var recentReservations = await _mongoDbService.EnergyReservations
                .Find(_ => true)
                .SortByDescending(r => r.CreatedAt)
                .Limit(5)
                .ToListAsync();

            var prosumerNics = recentReservations.Select(r => r.ProsumerNic).Distinct().ToList();
            var stationIds = recentReservations.Select(r => r.StationId).Distinct().ToList();
            var slotIds = recentReservations.Select(r => r.SlotId).Distinct().ToList();

            var prosumers = await _mongoDbService.Users
                .Find(u => prosumerNics.Contains(u.NIC))
                .ToListAsync();

            var stations = await _mongoDbService.SolarStations
                .Find(s => stationIds.Contains(s.StationId))
                .ToListAsync();
            
            var slots = await _mongoDbService.EnergyBookingSlots
                .Find(s => slotIds.Contains(s.SlotId))
                .ToListAsync();

            var result = recentReservations.Select(r =>
            {
                var prosumer = prosumers.FirstOrDefault(u => u.NIC == r.ProsumerNic);
                var station = stations.FirstOrDefault(s => s.StationId == r.StationId);
                var slot = slots.FirstOrDefault(s => s.SlotId == r.SlotId);

                return new
                {
                    id = r.ReservationId,
                    status = r.Status.ToString().ToLower(),
                    created_at = r.CreatedAt,
                    prosumer = new { full_name = prosumer?.FullName ?? "Unknown" },
                    hub = new { stationName = station?.StationName ?? "Unknown hub" },
                    energy_kwh = slot != null ? slot.CapacityKwh : 0
                };
            });

            return Ok(result);
        }
    }
}
