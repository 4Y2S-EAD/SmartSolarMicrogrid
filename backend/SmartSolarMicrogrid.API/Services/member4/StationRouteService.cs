/*
 * Feature: Nearby station driving routes | Member 4
 * Purpose: Request Google driving routes and display the shortest returned road distance.
 */
using System.Globalization;
using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using SmartSolarMicrogrid.API.DTOs.member4;

namespace SmartSolarMicrogrid.API.Services.member4;

public sealed class StationRouteException(string code, string message, int status) : Exception(message)
{
    public string Code { get; } = code;
    public int Status { get; } = status;
}

public sealed class StationRouteService(HttpClient http, IConfiguration configuration)
{
    public async Task<StationRouteResponse> ComputeAsync(
        MapLocation origin, StationMapItem station, CancellationToken cancellationToken)
    {
        // Keep the server credential out of Android and send only real origin/station coordinates.
        var key = configuration["GoogleRoutes:ApiKey"];
        if (string.IsNullOrWhiteSpace(key))
            throw new StationRouteException("ROUTING_NOT_CONFIGURED", "Driving directions are not configured yet.", 503);
        using var request = new HttpRequestMessage(HttpMethod.Post,
            "https://routes.googleapis.com/directions/v2:computeRoutes");
        request.Headers.Add("X-Goog-Api-Key", key);
        request.Headers.Add("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.routeLabels,routes.routeToken");
        request.Content = JsonContent.Create(new
        {
            origin = new { location = new { latLng = origin } },
            destination = new { location = new { latLng = station.Location } },
            travelMode = "DRIVE",
            routingPreference = "TRAFFIC_AWARE",
            requestedReferenceRoutes = new[] { "SHORTER_DISTANCE" },
            computeAlternativeRoutes = true,
            polylineQuality = "HIGH_QUALITY",
            polylineEncoding = "ENCODED_POLYLINE",
            units = "METRIC"
        });
        try
        {
            using var response = await http.SendAsync(request, cancellationToken);
            if (response.StatusCode is HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden)
                throw new StationRouteException("ROUTING_CONFIGURATION", "Driving directions are temporarily unavailable.", 503);
            if (response.StatusCode == HttpStatusCode.TooManyRequests)
                throw new StationRouteException("ROUTING_BUSY", "Driving directions are busy. Please try again later.", 503);
            if (!response.IsSuccessStatusCode)
                throw new StationRouteException("ROUTING_UNAVAILABLE", "Could not calculate driving directions. Please try again.", 502);
            using var json = await JsonDocument.ParseAsync(await response.Content.ReadAsStreamAsync(cancellationToken),
                cancellationToken: cancellationToken);
            if (!json.RootElement.TryGetProperty("routes", out var routes) || routes.GetArrayLength() == 0)
                throw new StationRouteException("NO_DRIVING_ROUTE", "No driving route was found for this station.", 404);
            // Compare actual road distances, including Google's distance-optimized reference route.
            // Keep geometry and ETA from that same route; a shorter drive can still take longer.
            var route = routes.EnumerateArray()
                .OrderBy(candidate => candidate.GetProperty("distanceMeters").GetInt32())
                .First();
            var distance = route.GetProperty("distanceMeters").GetInt32();
            var duration = route.GetProperty("duration").GetString() ?? "";
            var polyline = route.GetProperty("polyline").GetProperty("encodedPolyline").GetString();
            if (!duration.EndsWith('s') ||
                !double.TryParse(duration[..^1], NumberStyles.Float, CultureInfo.InvariantCulture, out var seconds) ||
                !double.IsFinite(seconds) || seconds < 0 || distance < 0 || string.IsNullOrWhiteSpace(polyline))
                throw new JsonException("Invalid route result.");
            return new(station.StationId, station.StationName, origin, station.Location!, distance, seconds, polyline);
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            // Provider timeout is distinct from a caller cancelling an obsolete request.
            throw new StationRouteException("ROUTING_TIMEOUT", "Driving directions timed out. Please try again.", 504);
        }
        catch (HttpRequestException)
        {
            // Do not return raw provider errors, headers or credentials to the mobile client.
            throw new StationRouteException("ROUTING_UNAVAILABLE", "Could not reach driving directions. Please try again.", 502);
        }
        catch (Exception exception) when (exception is JsonException or KeyNotFoundException or InvalidOperationException or FormatException)
        {
            // A malformed provider result must never become a fabricated straight-line route.
            throw new StationRouteException("ROUTING_INVALID_RESPONSE", "Could not read driving directions. Please try again.", 502);
        }
    }
}
