/* Module: Member 4 | Feature: Hub-scoped Grid Operator reservation dashboard
 * Purpose: All queries are restricted to the operator's assignedHubId (= StationId).
 *          Every count, filter and pagination decision is made here; no logic in the controller. */
using System.Globalization;
using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services.member4;

public sealed class HubOperatorReservationService(MongoDbService mongo)
{
    /// <summary>
    /// Returns a paginated, hub-scoped reservation page.
    /// <paramref name="assignedStationId"/> is read from the JWT; never trust a client-supplied value.
    /// </summary>
    public async Task<OperatorReservationPage> GetAsync(
        string view,
        string assignedStationId,
        OperatorReservationQuery query,
        CancellationToken ct)
    {
        var f = Builders<EnergyReservation>.Filter;

        // Always restrict to the operator's assigned station.
        var filter = f.Eq(x => x.StationId, assignedStationId);

        // Tab-level status restrictions applied entirely in MongoDB.
        if (view == "pending")  filter &= f.Eq(x => x.Status, ReservationStatus.Pending);
        if (view == "history")  filter &= f.In(x => x.Status, [ReservationStatus.Completed, ReservationStatus.Cancelled]);
        if (view == "approved") filter &= f.Eq(x => x.Status, ReservationStatus.Approved);
        if (view == "completed") filter &= f.Eq(x => x.Status, ReservationStatus.Completed);

        // Optional search/filter criteria — only for the "search" view.
        if (!string.IsNullOrWhiteSpace(query.ReservationId))
            filter &= f.Eq(x => x.ReservationId, query.ReservationId.Trim().ToLowerInvariant());
        if (!string.IsNullOrWhiteSpace(query.ProsumerNic))
            filter &= f.Regex(x => x.ProsumerNic, Literal(query.ProsumerNic));
        if (!string.IsNullOrWhiteSpace(query.Status))
            filter &= f.Eq(x => x.Status, Enum.Parse<ReservationStatus>(query.Status.Trim(), ignoreCase: true));
        if (!string.IsNullOrWhiteSpace(query.BookingDate))
        {
            var day = DateTime.SpecifyKind(
                DateTime.ParseExact(query.BookingDate, "yyyy-MM-dd", CultureInfo.InvariantCulture),
                DateTimeKind.Utc);
            filter &= f.Gte(x => x.BookingDate, day) & f.Lt(x => x.BookingDate, day.AddDays(1));
        }

        // Station text/ID search within the operator's assigned station only.
        // (A secondary station match is still allowed for search convenience, but the assignedStationId
        //  pre-filter already limits the data to a single station.)
        if (!string.IsNullOrWhiteSpace(query.Station))
        {
            var stF = Builders<SolarStationInfo>.Filter.Regex(x => x.StationName, Literal(query.Station));
            if (ObjectId.TryParse(query.Station.Trim(), out var sid))
                stF |= Builders<SolarStationInfo>.Filter.Eq(x => x.StationId, sid.ToString());
            var ids = await mongo.SolarStations.Find(stF).Project(x => x.StationId).ToListAsync(ct);
            // Only keep results that still belong to the operator's assigned station.
            ids = ids.Where(id => id == assignedStationId).ToList();
            filter &= f.In(x => x.StationId, ids);
        }

        var total = await mongo.EnergyReservations.CountDocumentsAsync(filter, cancellationToken: ct);
        var rows  = await mongo.EnergyReservations
            .Find(filter)
            .SortByDescending(x => x.BookingDate)
            .ThenByDescending(x => x.ReservationId)
            .Skip((query.Page - 1) * query.PageSize)
            .Limit(query.PageSize)
            .ToListAsync(ct);

        // Join station/slot names for the rows returned on this page only.
        var stations = await mongo.SolarStations
            .Find(Builders<SolarStationInfo>.Filter.In(x => x.StationId, rows.Select(x => x.StationId).Distinct()))
            .Project(x => new { x.StationId, x.StationName })
            .ToListAsync(ct);
        var slots = await mongo.EnergyBookingSlots
            .Find(Builders<EnergyBookingSlots>.Filter.In(x => x.SlotId, rows.Select(x => x.SlotId).Distinct()))
            .Project(x => new { x.SlotId, x.SlotNumber })
            .ToListAsync(ct);

        var names   = stations.ToDictionary(x => x.StationId, x => x.StationName);
        var numbers = slots.ToDictionary(x => x.SlotId, x => x.SlotNumber);

        var items = rows.Select(x => new OperatorReservationItem(
            x.ReservationId, x.ProsumerNic,
            x.StationId, names.GetValueOrDefault(x.StationId),
            x.SlotId, numbers.TryGetValue(x.SlotId, out var n) ? n : null,
            x.BookingDate, x.StartTime, x.EndTime, x.Status.ToString(),
            x.CreatedAt, x.UpdatedAt, x.CompletedAt, x.CancellationReason,
            x.Status == ReservationStatus.Pending
        )).ToList();

        // Hub-scoped counts: aggregate only within the operator's station.
        var hubFilter = f.Eq(x => x.StationId, assignedStationId);
        var counts = await mongo.EnergyReservations
            .Aggregate()
            .Match(hubFilter)
            .Group(x => x.Status, g => new { Status = g.Key, Count = g.Count() })
            .ToListAsync(ct);
        long Count(ReservationStatus status) => counts.FirstOrDefault(x => x.Status == status)?.Count ?? 0;
        var pending  = Count(ReservationStatus.Pending);
        var approved = Count(ReservationStatus.Approved);

        return new OperatorReservationPage(
            items, query.Page, query.PageSize, total,
            (long)Math.Ceiling((double)total / query.PageSize),
            new(pending + approved, pending, approved, Count(ReservationStatus.Completed)),
            Enum.GetNames<ReservationStatus>());
    }

    private static BsonRegularExpression Literal(string input)
        => new(Regex.Escape(input.Trim()), "i");
}
