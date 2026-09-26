/*
 * Feature: Nearby station driving routes | Member 4
 * Purpose: Offline .NET contract checks; fake HTTP responses exist only in this test executable.
 */
using System.Net;
using System.Text.Json;
using Microsoft.Extensions.Configuration;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Services.member4;

// Verify the Google request contract and failure handling without spending quota or modifying MongoDB.
var station = new StationMapItem("test-id", "Test station", new(40.7, -120.95), 1, 1, 1, "Active", null, 2);
var origin = new MapLocation(38.5, -120.2);
var settings = new ConfigurationBuilder().AddInMemoryCollection(
    new Dictionary<string, string?> { ["GoogleRoutes:ApiKey"] = "test-only-key" }).Build();
var handler = new StubHandler(HttpStatusCode.OK,
    """{"routes":[{"distanceMeters":18200,"duration":"1680.5s","polyline":{"encodedPolyline":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"}}]}""");
var result = await new StationRouteService(new HttpClient(handler), settings).ComputeAsync(origin, station, default);
Check(result.DistanceMeters == 18200 && result.DurationSeconds == 1680.5, "Provider metrics preserved");
Check(result.Origin == origin && result.Destination == station.Location, "Real request coordinates preserved");
Check(handler.Url == "https://routes.googleapis.com/directions/v2:computeRoutes", "Fixed Google endpoint");
using (var body = JsonDocument.Parse(handler.Body!))
{
    Check(body.RootElement.GetProperty("travelMode").GetString() == "DRIVE", "Driving mode");
    Check(body.RootElement.GetProperty("computeAlternativeRoutes").GetBoolean(), "Alternative road routes requested");
    Check(body.RootElement.GetProperty("requestedReferenceRoutes")[0].GetString() == "SHORTER_DISTANCE",
        "Distance-optimized Google route requested");
    Check(body.RootElement.GetProperty("origin").GetProperty("location").GetProperty("latLng")
        .GetProperty("latitude").GetDouble() == origin.Latitude, "Origin serialized");
    Check(body.RootElement.GetProperty("destination").GetProperty("location").GetProperty("latLng")
        .GetProperty("longitude").GetDouble() == station.Location!.Longitude, "Destination serialized");
}
Check(handler.Mask == "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.routeLabels,routes.routeToken",
    "Shorter-distance required field mask");
var alternatives = new StubHandler(HttpStatusCode.OK,
    """{"routes":[{"distanceMeters":2700,"duration":"540s","polyline":{"encodedPolyline":"default"}},{"distanceMeters":1500,"duration":"660s","polyline":{"encodedPolyline":"shorter"}},{"distanceMeters":2100,"duration":"480s","polyline":{"encodedPolyline":"fastest"}}]}""");
var shortest = await new StationRouteService(new HttpClient(alternatives), settings).ComputeAsync(origin, station, default);
Check(shortest.DistanceMeters == 1500, "Shortest distance chosen even when not first or fastest");
Check(shortest.DurationSeconds == 660 && shortest.EncodedPolyline == "shorter",
    "Selected route geometry and ETA stay together");
await ExpectFailure(new StubHandler(HttpStatusCode.OK, """{"routes":[]}"""), settings, "NO_DRIVING_ROUTE");
await ExpectFailure(new StubHandler(HttpStatusCode.Forbidden, "private provider failure"), settings, "ROUTING_CONFIGURATION");
await ExpectFailure(new StubHandler(HttpStatusCode.TooManyRequests, ""), settings, "ROUTING_BUSY");
await ExpectFailure(new StubHandler(HttpStatusCode.OK, """{"routes":[{"distanceMeters":-1}]}"""), settings, "ROUTING_INVALID_RESPONSE");
await ExpectFailure(new StubHandler(HttpStatusCode.OK, "invalid json"), settings, "ROUTING_INVALID_RESPONSE");
await ExpectFailure(new StubHandler(HttpStatusCode.OK, "{}"), new ConfigurationBuilder().Build(), "ROUTING_NOT_CONFIGURED");
using (var cancelled = new CancellationTokenSource())
{
    cancelled.Cancel();
    try
    {
        await new StationRouteService(new HttpClient(handler), settings).ComputeAsync(origin, station, cancelled.Token);
        throw new Exception("Cancellation not propagated");
    }
    catch (OperationCanceledException) { Console.WriteLine("PASS: Cancellation propagated"); }
}
Console.WriteLine("All route contract checks passed.");

void Check(bool condition, string name)
{
    // Fail the executable immediately when a contract regresses.
    if (!condition) throw new Exception(name);
    Console.WriteLine($"PASS: {name}");
}

async Task ExpectFailure(StubHandler stub, IConfiguration config, string code)
{
    // Reject fabricated routes on empty, malformed, unauthorized or unavailable provider responses.
    try
    {
        await new StationRouteService(new HttpClient(stub), config).ComputeAsync(origin, station, default);
        throw new Exception($"Expected {code}");
    }
    catch (StationRouteException error)
    {
        Check(error.Code == code, code);
        Check(!error.Message.Contains("private provider"), "Provider errors sanitized");
    }
}

sealed class StubHandler(HttpStatusCode status, string body) : HttpMessageHandler
{
    public string? Url { get; private set; }
    public string? Body { get; private set; }
    public string? Mask { get; private set; }

    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        // Capture outgoing data and return a controlled provider fixture only inside the test executable.
        cancellationToken.ThrowIfCancellationRequested();
        Url = request.RequestUri!.ToString();
        Body = await request.Content!.ReadAsStringAsync(cancellationToken);
        Mask = request.Headers.GetValues("X-Goog-FieldMask").Single();
        return new HttpResponseMessage(status) { Content = new StringContent(body) };
    }
}
