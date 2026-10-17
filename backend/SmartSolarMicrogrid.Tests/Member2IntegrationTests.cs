using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using SmartSolarMicrogrid.API.Controllers.member2;
using SmartSolarMicrogrid.API.DTOs.member2;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using Xunit;

namespace SmartSolarMicrogrid.Tests
{
    public class Member2IntegrationTests
    {
        private readonly StationsController _controller;

        public Member2IntegrationTests()
        {
            _controller = new StationsController(TestSharedContext.MongoDbService);
        }

        // INTEGRATION TEST 01: GET All Stations returns 200 OK with List
        [Fact]
        public async Task IT01_GetAllStations_ReturnsOkResultWithStationsList()
        {
            // Act
            var actionResult = await _controller.GetAllStations();

            // Assert
            var okResult = Assert.IsType<OkObjectResult>(actionResult);
            var stations = Assert.IsAssignableFrom<List<SolarStationInfo>>(okResult.Value);
            Assert.NotNull(stations);
        }

        // INTEGRATION TEST 02: POST Create Station & GET By ID
        [Fact]
        public async Task IT02_CreateStation_And_GetStationById_PersistsAndRetrievesData()
        {
            // Arrange
            var uniqueName = $"Integration Hub {Guid.NewGuid().ToString()[..8]}";
            var createDto = new CreateStationDto
            {
                StationName = uniqueName,
                Latitude = 6.9271,
                Longitude = 79.8612,
                CapacityKwh = 120.0,
                BatterySlotCount = 4,
                Status = "Active",
                Schedule = "08:00 AM - 06:00 PM"
            };

            // Act 1: Create Station
            var createResult = await _controller.CreateStation(createDto);
            var okCreate = Assert.IsType<OkObjectResult>(createResult);
            
            // Extract generated StationId dynamically via reflection
            var responseObj = okCreate.Value;
            Assert.NotNull(responseObj);
            var stationIdProp = responseObj.GetType().GetProperty("StationId");
            Assert.NotNull(stationIdProp);
            var stationId = stationIdProp.GetValue(responseObj)?.ToString();
            Assert.False(string.IsNullOrWhiteSpace(stationId));

            // Act 2: Fetch created station by ID
            var getResult = await _controller.GetStationById(stationId!);

            // Assert
            var okGet = Assert.IsType<OkObjectResult>(getResult);
            var retrievedStation = Assert.IsType<SolarStationInfo>(okGet.Value);
            Assert.Equal(stationId, retrievedStation.StationId);
            Assert.Equal(uniqueName, retrievedStation.StationName);
            Assert.Equal(120.0, retrievedStation.CapacityKwh);
            Assert.Equal("Active", retrievedStation.Status);

            // Clean up: Deactivate test station
            await _controller.DeactivateStation(stationId!);
        }

        // INTEGRATION TEST 03: GET Station by Non-Existent ID returns 404
        [Fact]
        public async Task IT03_GetStationById_NonExistentId_ReturnsNotFoundResult()
        {
            // Arrange: Valid 24-character hex ID that does not exist in DB
            const string nonExistentId = "000000000000000000000000";

            // Act
            var actionResult = await _controller.GetStationById(nonExistentId);

            // Assert
            var notFoundResult = Assert.IsType<NotFoundObjectResult>(actionResult);
            Assert.NotNull(notFoundResult.Value);
        }

        // INTEGRATION TEST 04: PUT Update Station updates persisted data
        [Fact]
        public async Task IT04_UpdateStation_UpdatesExistingStationDetails()
        {
            // Arrange: Create a temporary station to update
            var createDto = new CreateStationDto
            {
                StationName = $"Before Update {Guid.NewGuid().ToString()[..8]}",
                Latitude = 6.9000,
                Longitude = 79.9000,
                CapacityKwh = 80.0,
                BatterySlotCount = 2,
                Status = "Active"
            };

            var createResult = await _controller.CreateStation(createDto);
            var okCreate = Assert.IsType<OkObjectResult>(createResult);
            var stationId = okCreate.Value?.GetType().GetProperty("StationId")?.GetValue(okCreate.Value)?.ToString()!;

            // Act: Update station
            var updatedName = $"After Update {Guid.NewGuid().ToString()[..8]}";
            var updateDto = new UpdateStationDto
            {
                StationName = updatedName,
                CapacityKwh = 110.0
            };

            var updateResult = await _controller.UpdateStation(stationId, updateDto);
            Assert.IsType<OkObjectResult>(updateResult);

            // Verify the updated values in DB
            var verifyResult = await _controller.GetStationById(stationId);
            var okVerify = Assert.IsType<OkObjectResult>(verifyResult);
            var updatedStation = Assert.IsType<SolarStationInfo>(okVerify.Value);
            Assert.Equal(updatedName, updatedStation.StationName);
            Assert.Equal(110.0, updatedStation.CapacityKwh);

            // Clean up
            await _controller.DeactivateStation(stationId);
        }

        // INTEGRATION TEST 05: Complete Slot & Station Lifecycle
        [Fact]
        public async Task IT05_SlotLifecycle_Create_Get_Update_Delete_And_DeactivateStation()
        {
            // 1. Create a parent station
            var createDto = new CreateStationDto
            {
                StationName = $"Lifecycle Station {Guid.NewGuid().ToString()[..8]}",
                Latitude = 6.9100,
                Longitude = 79.9200,
                CapacityKwh = 100.0,
                BatterySlotCount = 4,
                Status = "Active"
            };

            var stationResult = await _controller.CreateStation(createDto);
            var okStation = Assert.IsType<OkObjectResult>(stationResult);
            var stationId = okStation.Value?.GetType().GetProperty("StationId")?.GetValue(okStation.Value)?.ToString()!;

            // 2. Create Battery Slot for this station
            var slotDto = new CreateSlotDto
            {
                SlotNumber = 1,
                BookingDate = DateTime.UtcNow.AddDays(7),
                StartTime = "09:00 AM",
                EndTime = "11:00 AM",
                CapacityKwh = 25.0,
                Status = "Available"
            };

            var slotResult = await _controller.CreateSlot(stationId, slotDto);
            var okSlot = Assert.IsType<OkObjectResult>(slotResult);
            var slotId = okSlot.Value?.GetType().GetProperty("SlotId")?.GetValue(okSlot.Value)?.ToString()!;
            Assert.False(string.IsNullOrWhiteSpace(slotId));

            // 3. Get All Slots for the station and verify presence
            var slotsResult = await _controller.GetSlotsForStation(stationId);
            var okSlots = Assert.IsType<OkObjectResult>(slotsResult);
            var slotsList = Assert.IsAssignableFrom<List<EnergyBookingSlots>>(okSlots.Value);
            Assert.Contains(slotsList, s => s.SlotId == slotId);

            // 4. Update the slot details
            var updateSlotDto = new UpdateSlotDto
            {
                CapacityKwh = 30.0,
                Status = "Available"
            };
            var updateResult = await _controller.UpdateSlot(slotId, updateSlotDto);
            Assert.IsType<OkObjectResult>(updateResult);

            // 5. Delete the slot
            var deleteSlotResult = await _controller.DeleteSlot(slotId);
            Assert.IsType<OkObjectResult>(deleteSlotResult);

            // 6. Deactivate the station (business rule succeeds since zero active reservations exist)
            var deactivateResult = await _controller.DeactivateStation(stationId);
            Assert.IsType<OkObjectResult>(deactivateResult);
        }
    }
}
