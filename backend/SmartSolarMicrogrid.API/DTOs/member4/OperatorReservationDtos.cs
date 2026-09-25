/* Module: Member 4 | Feature: Operator reservations
 * Purpose: Validated query and read-only dashboard projections of shared reservation data. */
using System.ComponentModel.DataAnnotations;
using System.Globalization;
using MongoDB.Bson;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.DTOs.member4;

public sealed class OperatorReservationQuery : IValidatableObject
{
    [Range(1, 100000)] public int Page { get; set; } = 1;
    [Range(1, 100)] public int PageSize { get; set; } = 20;
    [StringLength(24)] public string? ReservationId { get; set; }
    [StringLength(32)] public string? ProsumerNic { get; set; }
    [StringLength(100)] public string? Station { get; set; }
    [StringLength(10)] public string? BookingDate { get; set; }
    [StringLength(20)] public string? Status { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext context)
    {
        // Reject invalid input before constructing BSON filters; numeric enum aliases are not statuses.
        if (!string.IsNullOrWhiteSpace(ReservationId) && !ObjectId.TryParse(ReservationId.Trim(), out _))
            yield return new ValidationResult("Reservation ID must contain 24 hexadecimal characters.", [nameof(ReservationId)]);
        if (!string.IsNullOrWhiteSpace(BookingDate) &&
            (!DateTime.TryParseExact(BookingDate, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var day)
             || day.Year == 9999))
            yield return new ValidationResult("Booking date must be a valid yyyy-MM-dd date before year 9999.", [nameof(BookingDate)]);
        if (!string.IsNullOrWhiteSpace(Status) && !Enum.GetNames<ReservationStatus>().Any(x => x.Equals(Status.Trim(), StringComparison.OrdinalIgnoreCase)))
            yield return new ValidationResult("Status must be Pending, Approved, Completed or Cancelled.", [nameof(Status)]);
    }
}

public sealed record OperatorReservationItem(string ReservationId, string ProsumerNic,
    string StationId, string? StationName, string SlotId, int? SlotNumber,
    DateTime BookingDate, string StartTime, string EndTime, string Status,
    DateTime CreatedAt, DateTime UpdatedAt, DateTime? CompletedAt, string? CancellationReason, bool CanApprove);
public sealed record OperatorReservationSummary(long ActiveCount, long PendingCount, long ApprovedCount, long CompletedCount);
public sealed record OperatorReservationPage(IReadOnlyList<OperatorReservationItem> Items,
    int CurrentPage, int PageSize, long TotalRecords, long TotalPages,
    OperatorReservationSummary Summary, string[] StatusOptions);
