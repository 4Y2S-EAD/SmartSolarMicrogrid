using System;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.API.Models
{
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
}
