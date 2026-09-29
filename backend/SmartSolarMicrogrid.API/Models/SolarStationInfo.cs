// ============================================================================
// File: SolarStationInfo.cs
// Description: Data model representing solar station info.
// Author: Member 1
// ============================================================================
using System;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.API.Models
{
    public class GeoLocation
    {
        [BsonElement("latitude")]
        public double Latitude { get; set; }

        [BsonElement("longitude")]
        public double Longitude { get; set; }
    }

    public class SolarStationInfo
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string StationId { get; set; } = null!;

        [BsonElement("stationName")]
        public string StationName { get; set; } = null!;

        [BsonElement("location")]
        public GeoLocation Location { get; set; } = null!;

        [BsonElement("capacityKwh")]
        public double CapacityKwh { get; set; }

        [BsonElement("batterySlotCount")]
        public int BatterySlotCount { get; set; }

        [BsonElement("availableSlotCount")]
        public int AvailableSlotCount { get; set; }

        [BsonElement("status")]
        public string Status { get; set; } = null!;

        [BsonElement("schedule")]
        public string? Schedule { get; set; }

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }
}
