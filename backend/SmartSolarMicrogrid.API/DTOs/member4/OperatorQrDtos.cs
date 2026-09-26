/* Module: Grid Operator | Feature: QR verification and transfer completion
 * Member: Member 4
 * Purpose: Transport contracts; reservation entities remain shared. */
namespace SmartSolarMicrogrid.API.DTOs.member4;

public sealed record VerifyQrRequest(string? QrData);

public sealed record VerifiedReservation(
    string ReservationId, string ProsumerNic, string ProsumerName,
    string StationId, string StationName, string SlotId, int SlotNumber,
    string BookingDate, string StartTime, string EndTime, double CapacityKwh,
    string Status, DateTime VerifiedAt);

public sealed record VerifyQrResponse(string Result, string Code, string Message,
    VerifiedReservation? Reservation = null);

public sealed record CompleteTransferResponse(string Message, string ReservationId,
    string Status, string OperatorId, DateTime VerifiedAt, DateTime CompletedAt);

public sealed class QrVerificationException(int statusCode, string code, string message) : Exception(message)
{
    public int StatusCode { get; } = statusCode;
    public string Code { get; } = code;
}
