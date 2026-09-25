/* Module: Grid Operator | Feature: QR verification and transfer completion
 * Member: Member 4
 * Purpose: The single new operator QR endpoint; completion uses the existing reservation route. */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Services.member4;

namespace SmartSolarMicrogrid.API.Controllers.member4;

[ApiController]
[Route("api/operator")]
[Authorize(Roles = "GridOperator,gridoperator")]
public sealed class OperatorQrController(OperatorQrVerificationService service) : ControllerBase
{
    [HttpPost("verify-qr")]
    [RequestSizeLimit(16384)]
    public async Task<IActionResult> Verify([FromBody] VerifyQrRequest request, CancellationToken ct)
    {
        // Return controlled rejection reasons without QR contents, secrets or database exceptions.
        try { return Ok(await service.VerifyAsync(request.QrData, User, ct)); }
        catch (QrVerificationException ex)
        { return StatusCode(ex.StatusCode, new VerifyQrResponse("REJECTED", ex.Code, ex.Message)); }
        catch (Exception ex) when (ex is MongoException or TimeoutException)
        { return StatusCode(503, new VerifyQrResponse("REJECTED", "SERVICE_UNAVAILABLE", "Verification is unavailable. Please try again.")); }
    }
}
