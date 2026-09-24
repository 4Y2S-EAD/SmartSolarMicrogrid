/* Member 4 | Map verification | Database-free regression checks for map query business rules. */
using MongoDB.Bson;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Services.member4;

static void Check(bool condition, string description)
{
    // Fail the executable immediately if a query invariant is broken.
    if (!condition) throw new InvalidOperationException(description);
    Console.WriteLine($"PASS: {description}");
}

static BsonDocument Station(string id, string name, BsonValue location, string status = "Active")
{
    // Keep malformed-coordinate fixtures outside the actual MongoDB collection.
    return new BsonDocument {
        { "_id", id }, { "stationName", name }, { "location", location }, { "status", status },
        { "capacityKwh", 100 }, { "batterySlotCount", 4 }, { "availableSlotCount", 2 }
    };
}

var origin = Station("origin", "Origin Station", new BsonDocument { { "latitude", 0d }, { "longitude", 0d } });
var east = Station("east", "East Station", new BsonDocument { { "latitude", 0d }, { "longitude", 1d } }, "Inactive");
var missing = Station("missing", "Missing coordinate", new BsonDocument { { "latitude", 0d } });
var corrupt = Station("corrupt", "Corrupt coordinate", new BsonDocument { { "latitude", "invalid" }, { "longitude", 10d } });
var invalid = Station("invalid", "Outside range", new BsonDocument { { "latitude", 91d }, { "longitude", 0d } });
var nan = Station("nan", "Not finite", new BsonDocument { { "latitude", double.NaN }, { "longitude", 0d } });
var documents = new[] { east, missing, origin, corrupt, invalid, nan };
var all = StationMapService.Project(documents, new StationMapQueryDto());
Check(all.Stations.Count == 6 && all.UnplottableCount == 4, "Invalid locations remain explicit in all-stations results");
Check(all.Stations.Single(x => x.StationId == "origin").Location == new MapLocation(0, 0), "Zero coordinates are valid");
Check(all.Stations.Single(x => x.StationId == "missing").Location is null, "Absent longitude never defaults to zero");
Check(all.Stations.Single(x => x.StationId == "east").Status == "Inactive", "Inactive status is preserved");
Check(all.Stations.All(x => x.DistanceKm is null), "No distance is fabricated without an origin");
var distance = StationMapService.DistanceKm(0, 0, 0, 1);
Check(Math.Abs(distance - 111.195) < 0.01, "One longitude degree at equator is approximately 111.195 km");
Check(StationMapService.DistanceKm(0, 179.9, 0, -179.9) < 23, "Distance crosses the antimeridian correctly");
var nearby = StationMapService.Project(documents, new StationMapQueryDto { Latitude = 0, Longitude = 0, RadiusKm = 120 });
Check(nearby.Stations.Select(x => x.StationId).SequenceEqual(new[] { "origin", "east" }), "Nearby results exclude unplottable records and sort by distance");
var boundary = StationMapService.Project(documents, new StationMapQueryDto { Latitude = 0, Longitude = 0, RadiusKm = distance });
Check(boundary.Stations.Count == 2, "Radius boundary is inclusive");
var narrow = StationMapService.Project(documents, new StationMapQueryDto { Latitude = 0, Longitude = 0, RadiusKm = distance - 0.001 });
Check(narrow.Stations.Count == 1, "Station just beyond radius is excluded");
var search = StationMapService.Project(documents, new StationMapQueryDto { Query = "  eAsT  " });
Check(search.Stations.Count == 1 && search.Stations[0].StationId == "east", "Search is trimmed and case-insensitive");
Check(StationMapService.Project(documents, new StationMapQueryDto { Query = ".*" }).Stations.Count == 0, "Search treats regex metacharacters as literal text");
Console.WriteLine("All Member 4 map checks passed.");
