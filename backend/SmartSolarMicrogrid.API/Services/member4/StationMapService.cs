/*
 * Feature: Nearby station maps | Member 4
 * Purpose: Read Member 2's collection; validate map coordinates and perform search/distance queries.
 */
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member4;

namespace SmartSolarMicrogrid.API.Services.member4;

public sealed class StationMapService(MongoDbService mongo)
{
    public async Task<StationMapResponse> GetStationsAsync(StationMapQueryDto query, CancellationToken cancellationToken)
    {
        // Read BSON so absent coordinates cannot silently become zero through non-nullable doubles.
        var collection = mongo.SolarStations.Database.GetCollection<BsonDocument>(
            mongo.SolarStations.CollectionNamespace.CollectionName);
        var documents = await collection.Find(FilterDefinition<BsonDocument>.Empty).ToListAsync(cancellationToken);
        return Project(documents, query);
    }

    public static StationMapResponse Project(IEnumerable<BsonDocument> documents, StationMapQueryDto query)
    {
        // Apply map-only business rules without changing the stored station or Member 2's APIs.
        var items = new List<StationMapItem>();
        var unplottableCount = 0;
        var search = query.Query?.Trim() ?? "";
        foreach (var document in documents)
        {
            var name = ReadText(document, "stationName") ?? "Unnamed station";
            if (!name.Contains(search, StringComparison.OrdinalIgnoreCase)) continue;
            var locationDocument = document.GetValue("location", BsonNull.Value) as BsonDocument;
            var latitude = ReadNumber(locationDocument, "latitude");
            var longitude = ReadNumber(locationDocument, "longitude");
            MapLocation? location = latitude is >= -90 and <= 90 && longitude is >= -180 and <= 180
                ? new MapLocation(latitude.Value, longitude.Value) : null;
            if (location is null) unplottableCount++;
            double? distance = location is not null && query.Latitude.HasValue && query.Longitude.HasValue
                ? DistanceKm(query.Latitude.Value, query.Longitude.Value, location.Latitude, location.Longitude) : null;
            if (query.RadiusKm.HasValue && (!distance.HasValue || distance.Value > query.RadiusKm.Value)) continue;
            items.Add(new StationMapItem(
                document.GetValue("_id", BsonNull.Value).ToString() ?? "",
                name, location, ReadNumber(document, "capacityKwh"),
                ReadCount(document, "batterySlotCount"), ReadCount(document, "availableSlotCount"),
                ReadText(document, "status") ?? "Unknown", ReadText(document, "schedule"), distance));
        }
        var ordered = query.Latitude.HasValue
            ? items.OrderBy(item => item.DistanceKm ?? double.MaxValue).ThenBy(item => item.StationName)
            : items.OrderBy(item => item.StationName);
        return new StationMapResponse(ordered.ToList(), unplottableCount);
    }

    public static double DistanceKm(double latitude, double longitude, double stationLatitude, double stationLongitude)
    {
        // Haversine returns straight-line surface distance, not road distance.
        const double radians = Math.PI / 180;
        var latDelta = (stationLatitude - latitude) * radians;
        var lngDelta = (stationLongitude - longitude) * radians;
        var a = Math.Pow(Math.Sin(latDelta / 2), 2)
            + Math.Cos(latitude * radians) * Math.Cos(stationLatitude * radians) * Math.Pow(Math.Sin(lngDelta / 2), 2);
        return 6371.0088 * 2 * Math.Asin(Math.Sqrt(Math.Clamp(a, 0, 1)));
    }

    private static double? ReadNumber(BsonDocument? document, string field)
    {
        // Reject absent, non-numeric and non-finite values without inventing station data.
        if (document is null || !document.TryGetValue(field, out var value) || !value.IsNumeric) return null;
        var number = value.ToDouble();
        return double.IsFinite(number) ? number : null;
    }

    private static int? ReadCount(BsonDocument document, string field)
    {
        // Preserve unavailable counts instead of coercing corrupt values to a real slot count.
        var number = ReadNumber(document, field);
        return number is >= 0 and <= int.MaxValue && number == Math.Truncate(number.Value) ? (int)number.Value : null;
    }

    private static string? ReadText(BsonDocument document, string field)
    {
        // Read optional text without exposing BSON conversion failures to the client.
        return document.TryGetValue(field, out var value) && value.IsString ? value.AsString : null;
    }
}
