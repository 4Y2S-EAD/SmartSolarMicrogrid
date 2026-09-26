/*
 * Feature: Nearby station driving routes | Member 4
 * Purpose: Carry device origin and Google road-route results without changing station records.
 */
using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.member4;

public sealed class StationRouteRequest
{
    [Required, StringLength(100)] public string StationId { get; set; } = "";
    [Required, Range(-90d, 90d)] public double? Latitude { get; set; }
    [Required, Range(-180d, 180d)] public double? Longitude { get; set; }
}

public sealed record StationRouteResponse(
    string StationId, string StationName, MapLocation Origin, MapLocation Destination,
    int DistanceMeters, double DurationSeconds, string EncodedPolyline);
