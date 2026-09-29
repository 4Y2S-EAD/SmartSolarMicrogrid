// ============================================================================
// File: User.cs
// Description: Data model representing user.
// Author: Member 1
// ============================================================================
using System;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.API.Models
{
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

        [BsonElement("isDeactivationRequested")]
        public bool IsDeactivationRequested { get; set; } = false;

        [BsonElement("deactivationReason")]
        public string? DeactivationReason { get; set; }

        [BsonElement("badgeId")]
        public string? BadgeId { get; set; }

        [BsonElement("assignedHubId")]
        public string? AssignedHubId { get; set; }
    }
}
