/* Module: Member 4 | Feature: Grid Operator reservations
 * Purpose: Four authenticated read endpoints; existing reservation writes remain unchanged. */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Services.member4;

namespace SmartSolarMicrogrid.API.Controllers.member4;

[ApiController]
[Route("api/operator/reservations")]
[Authorize(Roles = "GridOperator,gridoperator,Backoffice,backoffice")]
public sealed class OperatorReservationsController(OperatorReservationService service, ILogger<OperatorReservationsController> logger) : ControllerBase
{
    [HttpGet]
    public Task<IActionResult> All([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
    {
        // Return all reservations with global metrics and pagination.
        return Read("all", filters, ct);
    }
    [HttpGet("pending")]
    public Task<IActionResult> Pending([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
    {
        // Restrict results to the existing Pending status in the backend.
        return Read("pending", filters, ct);
    }
    [HttpGet("history")]
    public Task<IActionResult> History([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
    {
        // Return Completed and Cancelled reservations without introducing new statuses.
        return Read("history", filters, ct);
    }
    [HttpGet("search")]
    public Task<IActionResult> Search([FromQuery] OperatorReservationQuery filters, CancellationToken ct)
    {
        // Combine the optional validated criteria in the central query service.
        return Read("search", filters, ct);
    }
    private async Task<IActionResult> Read(string view, OperatorReservationQuery filters, CancellationToken ct)
    {
        // Return a safe retryable error without exposing connection strings or database internals.
        try { return Ok(await service.GetAsync(view, filters, ct)); }
        catch (Exception ex) when (ex is MongoException or TimeoutException)
        {
            logger.LogWarning("Operator reservation query failed ({FailureType}).", ex.GetType().Name);
            return Problem(statusCode: 503, title: "Reservations are temporarily unavailable. Please retry.");
        }
    }
}
