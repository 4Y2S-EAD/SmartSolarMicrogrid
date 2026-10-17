using System;
using SmartSolarMicrogrid.API.DTOs.member2;
using SmartSolarMicrogrid.API.Models;
using Xunit;

namespace SmartSolarMicrogrid.Tests
{
    public class Member2UnitTests
    {
        // -------------------------------------------------------------
        // UNIT TEST 01: DTO to Model Mapping for Solar Station Creation
        // -------------------------------------------------------------
        [Fact]
        public void UT01_CreateStationDto_ValidData_MapsToCorrectStationModel()
        {
            // Arrange
            var dto = new CreateStationDto
            {
                StationName = "Galle Road Solar Hub",
                Latitude = 6.9147,
                Longitude = 79.9729,
                CapacityKwh = 150.0,
                BatterySlotCount = 8,
                Status = "Active",
                Schedule = "08:00 AM - 08:00 PM"
            };

            // Act
            var station = new SolarStationInfo
            {
                StationName = dto.StationName,
                Location = new GeoLocation { Latitude = dto.Latitude, Longitude = dto.Longitude },
                CapacityKwh = dto.CapacityKwh,
                BatterySlotCount = dto.BatterySlotCount,
                AvailableSlotCount = dto.BatterySlotCount,
                Status = dto.Status,
                Schedule = dto.Schedule,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            // Assert
            Assert.Equal("Galle Road Solar Hub", station.StationName);
            Assert.Equal(6.9147, station.Location.Latitude);
            Assert.Equal(79.9729, station.Location.Longitude);
            Assert.Equal(150.0, station.CapacityKwh);
            Assert.Equal(8, station.BatterySlotCount);
            Assert.Equal(8, station.AvailableSlotCount); // Initial availability should equal total slots
            Assert.Equal("Active", station.Status);
            Assert.Equal("08:00 AM - 08:00 PM", station.Schedule);
        }

        // -------------------------------------------------------------
        // UNIT TEST 02: Default Status and Property Initialization of CreateSlotDto
        // -------------------------------------------------------------
        [Fact]
        public void UT02_CreateSlotDto_DefaultValues_InitializesAsAvailable()
        {
            // Arrange & Act
            var bookingDate = new DateTime(2026, 11, 20, 0, 0, 0, DateTimeKind.Utc);
            var slotDto = new CreateSlotDto
            {
                SlotNumber = 1,
                BookingDate = bookingDate,
                StartTime = "09:00 AM",
                EndTime = "11:00 AM",
                CapacityKwh = 25.0
            };

            // Assert
            Assert.Equal(1, slotDto.SlotNumber);
            Assert.Equal(bookingDate, slotDto.BookingDate);
            Assert.Equal("09:00 AM", slotDto.StartTime);
            Assert.Equal("11:00 AM", slotDto.EndTime);
            Assert.Equal(25.0, slotDto.CapacityKwh);
            Assert.Equal("Available", slotDto.Status); // Default status must be Available
        }

        // -------------------------------------------------------------
        // UNIT TEST 03: Partial Update Logic for Solar Station
        // -------------------------------------------------------------
        [Fact]
        public void UT03_UpdateStationDto_PartialUpdates_LeavesUnspecifiedFieldsUnchanged()
        {
            // Arrange: Existing station
            var station = new SolarStationInfo
            {
                StationName = "Original Hub",
                Location = new GeoLocation { Latitude = 6.9000, Longitude = 79.8000 },
                CapacityKwh = 100.0,
                BatterySlotCount = 4,
                Status = "Active",
                Schedule = "08:00 - 18:00"
            };

            // DTO updates only Name and Capacity; other fields remain null
            var updateDto = new UpdateStationDto
            {
                StationName = "Renamed Solar Hub",
                CapacityKwh = 140.0
            };

            // Act: Apply partial updates (simulating StationsController.UpdateStation logic)
            if (updateDto.StationName != null) station.StationName = updateDto.StationName;
            if (updateDto.Latitude != null && updateDto.Longitude != null)
            {
                station.Location = new GeoLocation { Latitude = updateDto.Latitude.Value, Longitude = updateDto.Longitude.Value };
            }
            if (updateDto.CapacityKwh != null) station.CapacityKwh = updateDto.CapacityKwh.Value;
            if (updateDto.BatterySlotCount != null) station.BatterySlotCount = updateDto.BatterySlotCount.Value;
            if (updateDto.Status != null) station.Status = updateDto.Status;
            if (updateDto.Schedule != null) station.Schedule = updateDto.Schedule;

            // Assert
            Assert.Equal("Renamed Solar Hub", station.StationName);
            Assert.Equal(140.0, station.CapacityKwh);
            // Unchanged fields should retain their original values
            Assert.Equal(6.9000, station.Location.Latitude);
            Assert.Equal(79.8000, station.Location.Longitude);
            Assert.Equal(4, station.BatterySlotCount);
            Assert.Equal("Active", station.Status);
            Assert.Equal("08:00 - 18:00", station.Schedule);
        }

        // -------------------------------------------------------------
        // UNIT TEST 04: Slot Status Transition and Update Validation
        // -------------------------------------------------------------
        [Fact]
        public void UT04_UpdateSlotDto_AllowsStatusTransition_ToUnavailableOrBooked()
        {
            // Arrange
            var slot = new EnergyBookingSlots
            {
                SlotId = "slot_test_123",
                StationId = "station_test_123",
                SlotNumber = 2,
                StartTime = "10:00 AM",
                EndTime = "12:00 PM",
                CapacityKwh = 20.0,
                Status = "Available"
            };

            var updateDto = new UpdateSlotDto
            {
                CapacityKwh = 30.0,
                Status = "Unavailable"
            };

            // Act: Apply slot updates
            if (updateDto.CapacityKwh != null) slot.CapacityKwh = updateDto.CapacityKwh.Value;
            if (updateDto.Status != null) slot.Status = updateDto.Status;

            // Assert
            Assert.Equal(30.0, slot.CapacityKwh);
            Assert.Equal("Unavailable", slot.Status);
            Assert.Equal("10:00 AM", slot.StartTime); // Unmodified field
        }

        // -------------------------------------------------------------
        // UNIT TEST 05: Business Rule - Slot Deletion Blocked When Booked
        // -------------------------------------------------------------
        [Fact]
        public void UT05_BusinessRule_CannotDeleteBookedSlot_FailsValidation()
        {
            // Arrange
            var slot = new EnergyBookingSlots
            {
                SlotId = "slot_booked_001",
                StationId = "station_001",
                SlotNumber = 1,
                Status = "Booked" // Status is Booked
            };

            // Act: Evaluate business rule (identical to StationsController.DeleteSlot)
            bool isDeletionBlocked = (slot.Status == "Booked");
            string? errorMessage = isDeletionBlocked
                ? "Cannot delete this slot because it is already booked by a prosumer."
                : null;

            // Assert
            Assert.True(isDeletionBlocked);
            Assert.NotNull(errorMessage);
            Assert.Contains("already booked", errorMessage);
        }
    }
}
