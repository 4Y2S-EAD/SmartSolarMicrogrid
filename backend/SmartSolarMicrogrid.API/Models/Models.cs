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

        [BsonElement("passwordHash")]
        public string PasswordHash { get; set; } = null!;

        [BsonElement("phoneNumber")]
        public string? PhoneNumber { get; set; }

        [BsonElement("address")]
        public string? Address { get; set; }

        [BsonElement("role")]
        [BsonRepresentation(BsonType.String)]
        public UserRole Role { get; set; }

        [BsonElement("accountStatus")]
        [BsonRepresentation(BsonType.String)]
        public AccountStatus AccountStatus { get; set; } = AccountStatus.Pending;

        [BsonElement("isApproved")]
        public bool IsApproved { get; set; }

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("deactivatedAt")]
        public DateTime? DeactivatedAt { get; set; }
    }

    // 2. SOLAR STATION INFO (MICROGRID HUB) MODEL
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

    // 3. ENERGY BOOKING SLOTS MODEL
    public class EnergyBookingSlots
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string SlotId { get; set; } = null!;

        [BsonElement("stationId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string StationId { get; set; } = null!;

        [BsonElement("slotNumber")]
        public int SlotNumber { get; set; }

        [BsonElement("bookingDate")]
        public DateTime BookingDate { get; set; }

        [BsonElement("startTime")]
        public string StartTime { get; set; } = null!; // e.g., "09:00 AM"

        [BsonElement("endTime")]
        public string EndTime { get; set; } = null!; // e.g., "10:00 AM"

        [BsonElement("capacityKwh")]
        public double CapacityKwh { get; set; }

        [BsonElement("status")]
        public string Status { get; set; } = null!;

        [BsonElement("reservationId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? ReservationId { get; set; }

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
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
        public string ReservationId { get; set; } = null!;

        [BsonElement("prosumerNic")]
        public string ProsumerNic { get; set; } = null!; // Reference to User.NIC

        [BsonElement("stationId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string StationId { get; set; } = null!; // Reference to SolarStationInfo.StationId

        [BsonElement("slotId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string SlotId { get; set; } = null!; // Reference to EnergyBookingSlots.SlotId

        [BsonElement("bookingDate")]
        public DateTime BookingDate { get; set; }

        [BsonElement("startTime")]
        public string StartTime { get; set; } = null!;

        [BsonElement("endTime")]
        public string EndTime { get; set; } = null!;

        [BsonElement("status")]
        [BsonRepresentation(BsonType.String)]
        public ReservationStatus Status { get; set; } = ReservationStatus.Pending;

        [BsonElement("qrToken")]
        public string? QrToken { get; set; } // Encrypted transaction string for Android scanner

        [BsonElement("qrGeneratedAt")]
        public DateTime? QrGeneratedAt { get; set; }

        [BsonElement("operatorId")]
        public string? OperatorId { get; set; }

        [BsonElement("verifiedAt")]
        public DateTime? VerifiedAt { get; set; }

        [BsonElement("completedAt")]
        public DateTime? CompletedAt { get; set; }

        [BsonElement("cancellationReason")]
        public string? CancellationReason { get; set; }

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }
}

