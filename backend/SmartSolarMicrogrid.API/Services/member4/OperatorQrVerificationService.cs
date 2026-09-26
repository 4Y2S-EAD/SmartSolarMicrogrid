/* Module: Grid Operator | Feature: QR verification and transfer completion
 * Member: Member 4
 * Purpose: Authoritative reservation/QR checks and atomic transitions; reservation dates are independent of slot defaults. */
using System.Globalization;
using System.Security.Claims;
using System.Text.Json;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services.member4;

public sealed class OperatorQrVerificationService(MongoDbService db)
{
    public async Task<VerifyQrResponse> VerifyAsync(string? qrData, ClaimsPrincipal principal, CancellationToken ct)
    {
        // Authenticate the operator and QR before returning any reservation or customer information.
        var operatorId = await RequireOperatorAsync(principal, ct);
        var data = ReservationQrCredential.Read(qrData, Environment.GetEnvironmentVariable("QR_JWT_SECRET"));
        var id = data.GetProperty("reservationId").GetString()!;
        var reservation = await GetReservationAsync(id, ct);
        var details = await ValidateAsync(reservation, data, qrData!, ct);
        var now = DateTime.UtcNow;
        var result = await db.EnergyReservations.UpdateOneAsync(SnapshotFilter(reservation),
            Builders<EnergyReservation>.Update.Set(r => r.VerifiedAt, now)
                .Set(r => r.OperatorId, operatorId).Set(r => r.UpdatedAt, now), cancellationToken: ct);
        if (result.MatchedCount != 1) throw Changed();
        return new("VALID", "QR_VERIFIED", "Reservation verified. Review the details before completing the transfer.",
            new(reservation.ReservationId, reservation.ProsumerNic, details.Prosumer.FullName,
                reservation.StationId, details.Station.StationName, reservation.SlotId, details.Slot.SlotNumber,
                reservation.BookingDate.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture),
                reservation.StartTime, reservation.EndTime, details.Slot.CapacityKwh,
                reservation.Status.ToString(), now));
    }

    public async Task<CompleteTransferResponse> CompleteAsync(string id, ClaimsPrincipal principal, CancellationToken ct)
    {
        // Recheck current state and the same operator's server verification, then transition Approved exactly once.
        var operatorId = await RequireOperatorAsync(principal, ct);
        var reservation = await GetReservationAsync(id, ct);
        RequireApproved(reservation);
        if (reservation.VerifiedAt == null || reservation.OperatorId != operatorId ||
            reservation.VerifiedAt != reservation.UpdatedAt || reservation.VerifiedAt < reservation.QrGeneratedAt)
            throw new QrVerificationException(409, "SCAN_REQUIRED", "Scan and verify the current QR code before completing this reservation.");
        var data = ReservationQrCredential.Read(reservation.QrToken, Environment.GetEnvironmentVariable("QR_JWT_SECRET"));
        await ValidateAsync(reservation, data, reservation.QrToken!, ct);
        var now = DateTime.UtcNow;
        var result = await db.EnergyReservations.UpdateOneAsync(SnapshotFilter(reservation) &
            Builders<EnergyReservation>.Filter.Eq(r => r.OperatorId, operatorId) &
            Builders<EnergyReservation>.Filter.Eq(r => r.VerifiedAt, reservation.VerifiedAt),
            Builders<EnergyReservation>.Update.Set(r => r.Status, ReservationStatus.Completed)
                .Set(r => r.CompletedAt, now).Set(r => r.UpdatedAt, now), cancellationToken: ct);
        if (result.ModifiedCount != 1) throw Changed();
        return new("Energy transfer finalized. Reservation completed.", id,
            ReservationStatus.Completed.ToString(), operatorId, reservation.VerifiedAt.Value, now);
    }

    private async Task<string> RequireOperatorAsync(ClaimsPrincipal principal, CancellationToken ct)
    {
        // Both existing login endpoints use the NIC as JWT subject; role and active account are checked in MongoDB too.
        var id = principal.FindFirstValue(ClaimTypes.NameIdentifier) ?? principal.FindFirstValue("sub");
        if (principal.Identity?.IsAuthenticated != true || string.IsNullOrWhiteSpace(id) ||
            !(principal.IsInRole("GridOperator") || principal.IsInRole("gridoperator")))
            throw new QrVerificationException(403, "OPERATOR_REQUIRED", "An authenticated Grid Operator is required.");
        var user = await db.Users.Find(u => u.NIC == id).FirstOrDefaultAsync(ct);
        if (user?.Role != UserRole.GridOperator || user.AccountStatus != AccountStatus.Active)
            throw new QrVerificationException(403, "OPERATOR_INACTIVE", "An active Grid Operator account is required.");
        return id;
    }

    private async Task<EnergyReservation> GetReservationAsync(string id, CancellationToken ct)
    {
        // Validate ObjectId before sending a typed MongoDB query.
        if (!ObjectId.TryParse(id, out _))
            throw new QrVerificationException(400, "INVALID_REFERENCE", "Invalid reservation reference.");
        return await db.EnergyReservations.Find(r => r.ReservationId == id).FirstOrDefaultAsync(ct)
            ?? throw new QrVerificationException(404, "NOT_FOUND", "Reservation not found.");
    }

    private async Task<(User Prosumer, SolarStationInfo Station, EnergyBookingSlots Slot)> ValidateAsync(
        EnergyReservation reservation, JsonElement data, string rawQr, CancellationToken ct)
    {
        // Read current related records on every verification and completion; QR fields are never the source of truth.
        RequireApproved(reservation);
        if (!ObjectId.TryParse(reservation.StationId, out _))
            throw new QrVerificationException(409, "INVALID_STATION", "The reservation station reference is invalid.");
        if (!ObjectId.TryParse(reservation.SlotId, out _))
            throw new QrVerificationException(409, "INVALID_SLOT", "The reservation slot reference is invalid.");
        var prosumer = await db.Users.Find(u => u.NIC == reservation.ProsumerNic).FirstOrDefaultAsync(ct);
        var station = await db.SolarStations.Find(s => s.StationId == reservation.StationId).FirstOrDefaultAsync(ct);
        var slot = await db.EnergyBookingSlots.Find(s => s.SlotId == reservation.SlotId).FirstOrDefaultAsync(ct);
        ValidateRecords(reservation, prosumer, station, slot, data, rawQr);
        return (prosumer!, station!, slot!);
    }

    public static void ValidateRecords(EnergyReservation r, User? prosumer, SolarStationInfo? station,
        EnergyBookingSlots? slot, JsonElement data, string rawQr)
    {
        // Validate related records and the reservation's own signed schedule, as created by Member 3.
        RequireApproved(r);
        if (prosumer?.Role != UserRole.Prosumer || prosumer.AccountStatus != AccountStatus.Active || prosumer.NIC != r.ProsumerNic)
            throw new QrVerificationException(409, "INVALID_PROSUMER", "The reservation requires an active Prosumer account.");
        if (station == null || station.StationId != r.StationId || !string.Equals(station.Status, "Active", StringComparison.OrdinalIgnoreCase))
            throw new QrVerificationException(409, "INVALID_STATION", "The reservation station is missing or inactive.");
        if (slot == null || slot.SlotId != r.SlotId || slot.StationId != r.StationId || slot.SlotNumber < 1 ||
            !double.IsFinite(slot.CapacityKwh) || slot.CapacityKwh <= 0 ||
            !(string.Equals(slot.Status, "Available", StringComparison.OrdinalIgnoreCase) || string.Equals(slot.Status, "Booked", StringComparison.OrdinalIgnoreCase)) ||
            (!string.IsNullOrEmpty(slot.ReservationId) && slot.ReservationId != r.ReservationId))
            throw new QrVerificationException(409, "INVALID_SLOT", "The reserved battery slot is missing, unavailable or allocated to another reservation.");
        // Slots are reused across bookings; Member 3 stores the chosen date/times on the reservation.
        // Compare QR schedule fields with that authoritative reservation below, not the slot's defaults.
        if (string.IsNullOrEmpty(r.QrToken) || rawQr != r.QrToken || r.QrGeneratedAt == null ||
            data.GetProperty("reservationId").GetString() != r.ReservationId ||
            data.GetProperty("prosumerNic").GetString() != r.ProsumerNic ||
            data.GetProperty("stationId").GetString() != r.StationId ||
            data.GetProperty("slotId").GetString() != r.SlotId || data.GetProperty("slotNumber").GetInt32() != slot.SlotNumber ||
            data.GetProperty("bookingDate").GetString() != r.BookingDate.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture) ||
            data.GetProperty("startTime").GetString() != r.StartTime || data.GetProperty("endTime").GetString() != r.EndTime)
            throw new QrVerificationException(409, "STALE_QR", "This QR no longer matches the reservation. Ask the Prosumer to generate a new QR.");
        // A QR generated while Pending remains usable after approval: approval is always read from MongoDB above.
    }

    private static void RequireApproved(EnergyReservation r)
    {
        // Reject all non-Approved states, including an already completed transfer.
        if (r.Status != ReservationStatus.Approved || r.CompletedAt != null)
            throw new QrVerificationException(409, "INVALID_STATUS", $"Reservation is {r.Status}. Only an Approved reservation can proceed.");
    }

    private static FilterDefinition<EnergyReservation> SnapshotFilter(EnergyReservation r)
    {
        // Compare the read snapshot so cancellation, QR regeneration or another operator's scan cannot win unnoticed.
        var f = Builders<EnergyReservation>.Filter;
        return f.Eq(x => x.ReservationId, r.ReservationId) & f.Eq(x => x.Status, ReservationStatus.Approved) &
            f.Eq(x => x.UpdatedAt, r.UpdatedAt) & f.Eq(x => x.QrToken, r.QrToken) & f.Eq(x => x.CompletedAt, null);
    }

    private static QrVerificationException Changed()
    {
        // Return a safe retry instruction for a concurrent reservation change.
        return new(409, "RESERVATION_CHANGED", "The reservation changed. Scan its current QR code again.");
    }
}
