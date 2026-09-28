/*
 * Feature: Nearby station maps | Member 4
 * Purpose: Expose a read-only station search and nearby projection to both mobile roles.
 */
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Services.member4;
using Microsoft.AspNetCore.Authorization;

namespace SmartSolarMicrogrid.API.Controllers.member4;

[ApiController]
[Route("api/member4/maps/stations")]
[Authorize]
public sealed class StationMapController(StationMapService service) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> Get([FromQuery] StationMapQueryDto filters, CancellationToken cancellationToken)
    {
        // Require a complete finite origin and never accept a radius without an origin.
        if (filters.Latitude.HasValue != filters.Longitude.HasValue ||
            (filters.RadiusKm.HasValue && !filters.Latitude.HasValue) ||
            (filters.Latitude.HasValue && !double.IsFinite(filters.Latitude.Value)) ||
            (filters.Longitude.HasValue && !double.IsFinite(filters.Longitude.Value)) ||
            (filters.RadiusKm.HasValue && !double.IsFinite(filters.RadiusKm.Value)))
            return BadRequest(new { message = "Provide valid latitude and longitude together; a radius requires both." });
        return Ok(await service.GetStationsAsync(filters, cancellationToken));
    }
}
