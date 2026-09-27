/*
 * Feature: Nearby station driving routes | Member 4
 * Purpose: Authenticated routing to an existing station, separate from the unchanged station API.
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Services.member4;

namespace SmartSolarMicrogrid.API.Controllers.member4;

[ApiController]
[Authorize]
[Route("api/member4/maps/route")]
public sealed class StationRoutesController(StationMapService stations, StationRouteService routes) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> Post([FromBody] StationRouteRequest request, CancellationToken cancellationToken)
    {
        // Resolve destination from authoritative station data, never from client-supplied destination coordinates.
        if (request.Latitude is not double latitude || request.Longitude is not double longitude ||
            !double.IsFinite(latitude) || !double.IsFinite(longitude))
            return BadRequest(new { code = "INVALID_ORIGIN", message = "Provide a valid current location." });
        var projection = await stations.GetStationsAsync(new StationMapQueryDto(), cancellationToken);
        var station = projection.Stations.FirstOrDefault(item => item.StationId == request.StationId);
        if (station is null)
            return NotFound(new { code = "STATION_NOT_FOUND", message = "The selected station is no longer available." });
        if (station.Location is null)
            return BadRequest(new { code = "STATION_LOCATION_MISSING", message = "The station has no valid map location." });
        try
        {
            return Ok(await routes.ComputeAsync(new(latitude, longitude), station, cancellationToken));
        }
        catch (StationRouteException error)
        {
            // Return actionable, sanitized failures while leaving the existing map/list usable.
            return StatusCode(error.Status, new { code = error.Code, message = error.Message });
        }
    }
}
