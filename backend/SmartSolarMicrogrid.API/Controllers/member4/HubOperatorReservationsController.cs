/* Module: Member 4 | Feature: Hub-scoped Grid Operator reservations
 * Purpose: Grid Operators see ONLY reservations belonging to their assignedHubId (= StationId).
 *          The station restriction is read from the JWT; if absent (old token), a DB fallback is used. */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using SmartSolarMicrogrid.API.Services.member4;
using System.Security.Claims;

namespace SmartSolarMicrogrid.API.Controllers.member4;

[ApiController]
[Route("api/hub/reservations")]
[Authorize(Roles = "GridOperator,gridoperator")]
public sealed class HubOperatorReservationsController(
    HubOperatorReservationService service,
    MongoDbService mongo,
    ILogger<HubOperatorReservationsController> logger) : ControllerBase
{
    [HttpGet]
    public Task<IActionResult> All([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
        => Read("all", filters, ct);

    [HttpGet("pending")]
    public Task<IActionResult> Pending([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
        => Read("pending", filters, ct);

    [HttpGet("approved")]
    public Task<IActionResult> Approved([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
        => Read("approved", filters, ct);

    [HttpGet("completed")]
    public Task<IActionResult> Completed([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
        => Read("completed", filters, ct);

    [HttpGet("history")]
    public Task<IActionResult> History([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
        => Read("history", filters, ct);

    [HttpGet("search")]
    public Task<IActionResult> Search([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
        => Read("search", filters, ct);

    private async Task<IActionResult> Read(string view, OperatorReservationQuery filters, CancellationToken ct)
    {
        // Primary path: assignedHubId is embedded in the JWT at login.
        var assignedHubId = User.FindFirstValue("assignedHubId");

        // Fallback: token was issued before the claim was added — look up the user record.
        if (string.IsNullOrWhiteSpace(assignedHubId))
        {
            var nic = User.FindFirstValue(ClaimTypes.NameIdentifier)
                       ?? User.FindFirstValue("sub");
            if (!string.IsNullOrWhiteSpace(nic))
            {
                var user = await mongo.Users
                    .Find(u => u.NIC == nic)
                    .Project(u => new { u.AssignedHubId })
                    .FirstOrDefaultAsync(ct);
                assignedHubId = user?.AssignedHubId;
            }
        }

        if (string.IsNullOrWhiteSpace(assignedHubId))
            return BadRequest(new { Message = "Your account does not have an assigned hub. Contact a Back Office administrator." });

        try
        {
            var result = await service.GetAsync(view, assignedHubId, filters, ct);
            return Ok(result);
        }
        catch (Exception ex) when (ex is MongoException or TimeoutException)
        {
            logger.LogWarning("Hub operator reservation query failed ({FailureType}).", ex.GetType().Name);
            return Problem(statusCode: 503, title: "Reservations are temporarily unavailable. Please retry.");
        }
    }
}

