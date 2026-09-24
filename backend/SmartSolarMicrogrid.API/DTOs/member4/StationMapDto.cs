/*
 * Feature: Nearby station maps | Member 4
 * Purpose: Read-only map projections and validated search/location input.
 */
using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.member4;

public sealed class StationMapQueryDto
{
    [Range(-90d, 90d)] public double? Latitude { get; set; }
    [Range(-180d, 180d)] public double? Longitude { get; set; }
    [Range(0.1d, 500d)] public double? RadiusKm { get; set; }
    [StringLength(100)] public string? Query { get; set; }
}

public sealed record MapLocation(double Latitude, double Longitude);

public sealed record StationMapItem(
    string StationId, string StationName, MapLocation? Location,
    double? CapacityKwh, int? BatterySlotCount, int? AvailableSlotCount,
    string Status, string? Schedule, double? DistanceKm);

public sealed record StationMapResponse(
    IReadOnlyList<StationMapItem> Stations, int UnplottableCount);
