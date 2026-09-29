// ============================================================================
// File: EnergyReservation.cs
// Description: Data model representing energy reservation.
// Author: Member 1
// ============================================================================
using System;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.API.Models
{
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
