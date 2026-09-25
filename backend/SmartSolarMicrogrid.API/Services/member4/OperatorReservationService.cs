/* Module: Member 4 | Feature: Operator reservation dashboard
 * Purpose: Query existing collections, join page data and calculate authoritative dashboard counts. */
using System.Globalization;
using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services.member4;

public sealed class OperatorReservationService(MongoDbService mongo)
{
    public async Task<OperatorReservationPage> GetAsync(string view, OperatorReservationQuery query, CancellationToken ct)
    {
        // Apply all search/status rules in MongoDB and retrieve only the requested page.
        var f = Builders<EnergyReservation>.Filter;
        var filter = f.Empty;
        if (view == "pending") filter &= f.Eq(x => x.Status, ReservationStatus.Pending);
        if (view == "history") filter &= f.In(x => x.Status, [ReservationStatus.Completed, ReservationStatus.Cancelled]);
        if (!string.IsNullOrWhiteSpace(query.ReservationId)) filter &= f.Eq(x => x.ReservationId, query.ReservationId.Trim().ToLowerInvariant());
        if (!string.IsNullOrWhiteSpace(query.ProsumerNic)) filter &= f.Regex(x => x.ProsumerNic, Literal(query.ProsumerNic));
        if (!string.IsNullOrWhiteSpace(query.Status)) filter &= f.Eq(x => x.Status, Enum.Parse<ReservationStatus>(query.Status.Trim(), true));
        if (!string.IsNullOrWhiteSpace(query.BookingDate))
        {
            var day = DateTime.SpecifyKind(DateTime.ParseExact(query.BookingDate, "yyyy-MM-dd", CultureInfo.InvariantCulture), DateTimeKind.Utc);
            filter &= f.Gte(x => x.BookingDate, day) & f.Lt(x => x.BookingDate, day.AddDays(1));
        }
        if (!string.IsNullOrWhiteSpace(query.Station))
        {
            var stationFilter = Builders<SolarStationInfo>.Filter.Regex(x => x.StationName, Literal(query.Station));
            if (ObjectId.TryParse(query.Station.Trim(), out var stationId))
                stationFilter |= Builders<SolarStationInfo>.Filter.Eq(x => x.StationId, stationId.ToString());
            var ids = await mongo.SolarStations.Find(stationFilter).Project(x => x.StationId).ToListAsync(ct);
            // Preserve ID searches even if a station reference has since been removed.
            if (ObjectId.TryParse(query.Station.Trim(), out stationId)) ids.Add(stationId.ToString());
            filter &= f.In(x => x.StationId, ids);
        }
        var total = await mongo.EnergyReservations.CountDocumentsAsync(filter, cancellationToken: ct);
        var rows = await mongo.EnergyReservations.Find(filter).SortByDescending(x => x.BookingDate)
            .ThenByDescending(x => x.ReservationId).Skip((query.Page - 1) * query.PageSize).Limit(query.PageSize).ToListAsync(ct);
        var stations = await mongo.SolarStations.Find(Builders<SolarStationInfo>.Filter.In(x => x.StationId, rows.Select(x => x.StationId)))
            .Project(x => new { x.StationId, x.StationName }).ToListAsync(ct);
        var slots = await mongo.EnergyBookingSlots.Find(Builders<EnergyBookingSlots>.Filter.In(x => x.SlotId, rows.Select(x => x.SlotId)))
            .Project(x => new { x.SlotId, x.SlotNumber }).ToListAsync(ct);
        var names = stations.ToDictionary(x => x.StationId, x => x.StationName);
        var numbers = slots.ToDictionary(x => x.SlotId, x => x.SlotNumber);
        var items = rows.Select(x => new OperatorReservationItem(x.ReservationId, x.ProsumerNic,
            x.StationId, names.GetValueOrDefault(x.StationId), x.SlotId,
            numbers.TryGetValue(x.SlotId, out var number) ? number : null, x.BookingDate, x.StartTime, x.EndTime,
            x.Status.ToString(), x.CreatedAt, x.UpdatedAt, x.CompletedAt, x.CancellationReason,
            x.Status == ReservationStatus.Pending)).ToList();
        // Global counts deliberately do not change with the current page, tab or search filters.
        var counts = await mongo.EnergyReservations.Aggregate().Group(x => x.Status, g => new { Status = g.Key, Count = g.Count() }).ToListAsync(ct);
        long Count(ReservationStatus status) => counts.FirstOrDefault(x => x.Status == status)?.Count ?? 0;
        var pending = Count(ReservationStatus.Pending);
        var approved = Count(ReservationStatus.Approved);
        return new OperatorReservationPage(items, query.Page, query.PageSize, total,
            (long)Math.Ceiling((double)total / query.PageSize),
            new(pending + approved, pending, approved, Count(ReservationStatus.Completed)), Enum.GetNames<ReservationStatus>());
    }

    private static BsonRegularExpression Literal(string input)
    {
        // Treat user text literally so regex syntax cannot alter the query's meaning.
        return new BsonRegularExpression(Regex.Escape(input.Trim()), "i");
    }
}
