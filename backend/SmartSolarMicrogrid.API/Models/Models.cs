using System;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.API.Models
{
    // 1. USER COLLECTION MODEL
    public enum UserRole
    {
        Backoffice,
        GridOperator,
        Prosumer
    }

    public enum AccountStatus
    {
        Pending,
        Active,
        Deactivated
    }

    public class User
    {
        [BsonId] // Primary Key explicitly maps to Prosumer / User NIC
        [BsonRepresentation(BsonType.String)]
        public string NIC { get; set; } = null!;

        [BsonElement("fullName")]
        public string FullName { get; set; } = null!;

        [BsonElement("email")]
        public string Email { get; set; } = null!;

        [BsonElement("phoneNumber")]
        public string? PhoneNumber { get; set; }

        [BsonElement("passwordHash")]
        public string PasswordHash { get; set; } = null!;

        [BsonElement("role")]
        [BsonRepresentation(BsonType.String)]
        public UserRole Role { get; set; }

        [BsonElement("status")]
        [BsonRepresentation(BsonType.String)]
        public AccountStatus Status { get; set; } = AccountStatus.Pending;

        [BsonElement("deactivationReason")]
        public string? DeactivationReason { get; set; }

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }

    // 2. SOLAR STATION INFO (MICROGRID HUB) MODEL
    public class GeoLocation
    {
        [BsonElement("type")]
        public string Type { get; set; } = "Point";

        // GeoJSON format: [Longitude, Latitude] for Google Maps API query support
        [BsonElement("coordinates")]
        public double[] Coordinates { get; set; } = new double[2];
    }

    public class SolarStationInfo
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string Id { get; set; } = null!;

        [BsonElement("stationName")]
        public string StationName { get; set; } = null!;

        [BsonElement("locationName")]
        public string LocationName { get; set; } = null!;

        [BsonElement("location")]
        public GeoLocation Location { get; set; } = null!;

        [BsonElement("capacityKwH")]
        public double CapacityKwH { get; set; }

        [BsonElement("totalBatterySlots")]
        public int TotalBatterySlots { get; set; }

        [BsonElement("availableBatterySlots")]
        public int AvailableBatterySlots { get; set; }

        [BsonElement("isActive")]
        public bool IsActive { get; set; } = true;

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }

    // 3. ENERGY BOOKING SLOTS MODEL
    public class EnergyBookingSlots
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string Id { get; set; } = null!;

        [BsonElement("stationId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string StationId { get; set; } = null!;

        [BsonElement("slotDate")]
        public DateTime SlotDate { get; set; }

        [BsonElement("startTime")]
        public string StartTime { get; set; } = null!; // e.g., "09:00 AM"

        [BsonElement("endTime")]
        public string EndTime { get; set; } = null!; // e.g., "10:00 AM"

        [BsonElement("maxEnergyKwh")]
        public double MaxEnergyKwh { get; set; }

        [BsonElement("isAvailable")]
        public bool IsAvailable { get; set; } = true;
    }

    // 4. ENERGY RESERVATION MODEL
    public enum ReservationStatus
    {
        Pending,
        Approved,
        Completed,
        Cancelled
    }

    public class EnergyReservation
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string Id { get; set; } = null!;

        [BsonElement("prosumerNIC")]
        public string ProsumerNIC { get; set; } = null!; // Reference to User.NIC

        [BsonElement("stationId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string StationId { get; set; } = null!; // Reference to SolarStationInfo.Id

        [BsonElement("slotId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string SlotId { get; set; } = null!; // Reference to EnergyBookingSlots.Id

        [BsonElement("reservationDate")]
        public DateTime ReservationDate { get; set; }

        [BsonElement("energyAmountKwh")]
        public double EnergyAmountKwh { get; set; }

        [BsonElement("qrCodeToken")]
        public string? QrCodeToken { get; set; } // Encrypted transaction string for Android scanner

        [BsonElement("status")]
        [BsonRepresentation(BsonType.String)]
        public ReservationStatus Status { get; set; } = ReservationStatus.Pending;

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("verifiedByOperatorNIC")]
        public string? VerifiedByOperatorNIC { get; set; }

        [BsonElement("completedAt")]
        public DateTime? CompletedAt { get; set; }
    }
}
