/*
File Name   : ReservationsController.cs
Description : API Controller managing prosumer reservation, rule validation and queries

Creator     : Rathnayake R. M. S. D. (IT22140616)
*/

using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.member3;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Threading.Tasks;

using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Configuration;

namespace SmartSolarMicrogrid.API.Controllers.member3
{
  [ApiController]
  [Route("api/reservations")]
  public class ReservationsController : ControllerBase
  {
    private readonly MongoDbService _mongoDbService;
    private readonly IConfiguration _configuration;

    public ReservationsController(MongoDbService mongoDbService, IConfiguration configuration)
    {
      _mongoDbService = mongoDbService;
      _configuration = configuration;
    }


    //  Create Reservation (POST: api/reservations)
    [HttpPost]
    public async Task<IActionResult> CreateReservation([FromBody] CreateReservationDto dto)
    {
      DateTime now = DateTime.UtcNow;
      DateTime scheduledDateTime = ParseSlotDateTime(dto.BookingDate, dto.StartTime);

      //  Prosumer 
      //  Verify prosumer account exists and Active      
      var prosumer = await _mongoDbService.Users.Find(u => u.NIC == dto.ProsumerNic).FirstOrDefaultAsync();
      if (prosumer == null)
      {
        return NotFound(new { Message = "Prosumer account not found." });
      }

      if (prosumer.AccountStatus != AccountStatus.Active)
      {
        return BadRequest(new { Message = "Prosumer account is not active." });
      }

      //  Station and Slot
      //  Verify is station exists
      var station = await _mongoDbService.SolarStations.Find(s => s.StationId == dto.StationId).FirstOrDefaultAsync();
      if (station == null)
      {
        return NotFound(new { Message = "Solar station not found." });
      }

      //  Verify is slot exists and matches to selected Station
      var slot = await _mongoDbService.EnergyBookingSlots.Find(s => s.SlotId == dto.SlotId).FirstOrDefaultAsync();
      if (slot == null || slot.StationId != dto.StationId)
      {
        return NotFound(new { Message = "The specified battery slot was not found on this station." });
      }

      //  Prevent double booking
      if (!string.Equals(slot.Status, "Available", StringComparison.OrdinalIgnoreCase))
      {
        return Conflict(new { Message = "The requested battery slot is already reserved or unavailable." });
      }


      //  Reserve date and time
      //  Cannot scheduled in past
      if (scheduledDateTime < now)
      {
        return BadRequest(new { Message = "Schedule can only be made for future dates." });
      }

      //  Can only scheduled within 7 days
      if (scheduledDateTime > now.AddDays(7))
      {
        return BadRequest(new { Message = "Reservations can only be scheduled within 7 days from today." });
      }


      // Create reservation
      var reservation = new EnergyReservation
      {
        ProsumerNic = dto.ProsumerNic,
        StationId = dto.StationId,
        SlotId = dto.SlotId,
        BookingDate = dto.BookingDate.Date,
        StartTime = dto.StartTime,
        EndTime = dto.EndTime,
        Status = ReservationStatus.Pending,
        CreatedAt = now,
        UpdatedAt = now
      };

      await _mongoDbService.EnergyReservations.InsertOneAsync(reservation);

      // Reserve the slot
      var updateSlotDef = Builders<EnergyBookingSlots>.Update
          .Set(s => s.Status, "Booked")
          .Set(s => s.ReservationId, reservation.ReservationId)
          .Set(s => s.UpdatedAt, DateTime.UtcNow);

      await _mongoDbService.EnergyBookingSlots.UpdateOneAsync(s => s.SlotId == dto.SlotId, updateSlotDef);

      var summary = new ReservationSummaryDto
      {
        ReservationId = reservation.ReservationId,
        ProsumerNic = reservation.ProsumerNic,
        StationId = station.StationId,
        StationName = station.StationName,
        SlotId = slot.SlotId,
        SlotNumber = slot.SlotNumber,
        BookingDate = reservation.BookingDate,
        StartTime = reservation.StartTime,
        EndTime = reservation.EndTime,
        Status = reservation.Status.ToString(),
        QrToken = null,
        CreatedAt = reservation.CreatedAt,
        UpdatedAt = reservation.UpdatedAt
      };

      // Return the created reservation
      return CreatedAtAction(nameof(GetReservationById), new { id = reservation.ReservationId }, summary);
    }


    // Read one reservation by ID (GET: api/reservations/{id})
    [HttpGet("{id}")]
    public async Task<IActionResult> GetReservationById(string id)
    {
      // Fetch the reservation
      var reservation = await _mongoDbService.EnergyReservations.Find(r => r.ReservationId == id).FirstOrDefaultAsync();

      // Check if reservation exists
      if (reservation == null)
      {
        return NotFound(new { Message = "Reservation not found." });
      }

      // Fetch the station and slot details
      var station = await _mongoDbService.SolarStations.Find(s => s.StationId == reservation.StationId).FirstOrDefaultAsync();
      var slot = await _mongoDbService.EnergyBookingSlots.Find(s => s.SlotId == reservation.SlotId).FirstOrDefaultAsync();

      // Return reservation details
      var summary = new ReservationSummaryDto
      {
        ReservationId = reservation.ReservationId,
        ProsumerNic = reservation.ProsumerNic,
        StationId = reservation.StationId,
        StationName = station?.StationName ?? "Unknown Station",
        SlotId = reservation.SlotId,
        SlotNumber = slot?.SlotNumber ?? 0,
        BookingDate = reservation.BookingDate,
        StartTime = reservation.StartTime,
        EndTime = reservation.EndTime,
        Status = reservation.Status.ToString(),
        QrToken = reservation.QrToken,
        CreatedAt = reservation.CreatedAt,
        UpdatedAt = reservation.UpdatedAt
      };

      return Ok(summary);
    }


    // Update reservation (PUT: api/reservations/{id})
    [HttpPut("{id}")]
    public async Task<IActionResult> UpdateReservation(string id, [FromBody] UpdateReservationDto dto)
    {
      // get existing reservation
      var reservation = await _mongoDbService.EnergyReservations.Find(r => r.ReservationId == id).FirstOrDefaultAsync();

      // Check reservation exists
      if (reservation == null)
      {
        return NotFound(new { Message = "Reservation not found." });
      }

      // Cannot modify completed or cancelled reservations
      if (reservation.Status == ReservationStatus.Completed || reservation.Status == ReservationStatus.Cancelled)
      {
        return BadRequest(new { Message = $"Cannot modify a reservation that is already {reservation.Status}." });
      }

      DateTime now = DateTime.UtcNow;
      DateTime currentScheduledTime = ParseSlotDateTime(reservation.BookingDate, reservation.StartTime);
      DateTime newScheduledTime = ParseSlotDateTime(dto.BookingDate, dto.StartTime);

      // Cannot update reservation less than 12 hours remain
      if (currentScheduledTime - now < TimeSpan.FromHours(12))
      {
        return BadRequest(new { Message = "Reservation can only modified least 12 hours remain." });
      }

      // Cannot reschedule to a past date
      if (newScheduledTime < now)
      {
        return BadRequest(new { Message = "Reservation reschedule can only be made for future dates." });
      }

      // Cannot reschedule beyond 7 days
      if (newScheduledTime > now.AddDays(7))
      {
        return BadRequest(new { Message = "Reservation reschedule must within the 7 day period." });
      }
      
      // Verify the new station exists
      var newStation = await _mongoDbService.SolarStations.Find(s => s.StationId == dto.StationId).FirstOrDefaultAsync();
      if (newStation == null)
      {
        return NotFound(new { Message = "The selected station does not exist." });
      }

      // Verify the new slot exists on the new Station
      var newSlot = await _mongoDbService.EnergyBookingSlots.Find(s => s.SlotId == dto.SlotId).FirstOrDefaultAsync();
      if (newSlot == null || newSlot.StationId != dto.StationId)
      {
        return NotFound(new { Message = "Selected slot was not found on the specified station." });
      }

      // Is target slot available and free the previous slot
      if (reservation.SlotId != dto.SlotId)
      {
        if (!string.Equals(newSlot.Status, "Available", StringComparison.OrdinalIgnoreCase))
        {
          return Conflict(new { Message = "The target slot is already reserved or unavailable." });
        }

        // Release previous slot
        var releaseOldSlotDef = Builders<EnergyBookingSlots>.Update
            .Set(s => s.Status, "Available")
            .Set(s => s.ReservationId, null)
            .Set(s => s.UpdatedAt, DateTime.UtcNow);
        await _mongoDbService.EnergyBookingSlots.UpdateOneAsync(s => s.SlotId == reservation.SlotId, releaseOldSlotDef);

        // Reserve new slot
        var reserveNewSlotDef = Builders<EnergyBookingSlots>.Update
            .Set(s => s.Status, "Booked")
            .Set(s => s.ReservationId, reservation.ReservationId)
            .Set(s => s.UpdatedAt, DateTime.UtcNow);
        await _mongoDbService.EnergyBookingSlots.UpdateOneAsync(s => s.SlotId == dto.SlotId, reserveNewSlotDef);
      }

      // Update the reservation
      var updateReservationDef = Builders<EnergyReservation>.Update
          .Set(r => r.StationId, dto.StationId)
          .Set(r => r.SlotId, dto.SlotId)
          .Set(r => r.BookingDate, dto.BookingDate.Date)
          .Set(r => r.StartTime, dto.StartTime)
          .Set(r => r.EndTime, dto.EndTime)
          .Set(r => r.UpdatedAt, DateTime.UtcNow);

      await _mongoDbService.EnergyReservations.UpdateOneAsync(r => r.ReservationId == id, updateReservationDef);

      var summary = new ReservationSummaryDto
      {
        ReservationId = reservation.ReservationId,
        ProsumerNic = reservation.ProsumerNic,
        StationId = newStation.StationId,
        StationName = newStation.StationName,
        SlotId = newSlot.SlotId,
        SlotNumber = newSlot.SlotNumber,
        BookingDate = dto.BookingDate.Date,
        StartTime = dto.StartTime,
        EndTime = dto.EndTime,
        Status = reservation.Status.ToString(),
        QrToken = reservation.QrToken,
        CreatedAt = reservation.CreatedAt,
        UpdatedAt = DateTime.UtcNow
      };

      return Ok(summary);
    }

    // Cancel Reservation (DELETE: api/reservations/{id})
    [HttpDelete("{id}")]
    public async Task<IActionResult> CancelReservation(string id, [FromBody] CancelReservationDto dto)
    {
      // Fetch the reservation
      var reservation = await _mongoDbService.EnergyReservations.Find(r => r.ReservationId == id).FirstOrDefaultAsync();

      // Check if reservation exists
      if (reservation == null)
      {
        return NotFound(new { Message = "Reservation not found." });
      }

      // Check if reservation is already cancelled or completed
      if (reservation.Status == ReservationStatus.Cancelled)
      {
        return BadRequest(new { Message = "This reservation is already cancelled." });
      }

      if (reservation.Status == ReservationStatus.Completed)
      {
        return BadRequest(new { Message = "Cannot cancel a completed reservation." });
      }

      DateTime now = DateTime.UtcNow;
      DateTime currentScheduledTime = ParseSlotDateTime(reservation.BookingDate, reservation.StartTime);

      // Check if least 12 hours remain
      if (currentScheduledTime - now < TimeSpan.FromHours(12))
      {
        return BadRequest(new { Message = "Reservations can be cancelled with 12 hours." });
      }

      // Update the reservation status
      var updateReservationDef = Builders<EnergyReservation>.Update
          .Set(r => r.Status, ReservationStatus.Cancelled)
          .Set(r => r.CancellationReason, dto.CancellationReason)
          .Set(r => r.UpdatedAt, DateTime.UtcNow);

      await _mongoDbService.EnergyReservations.UpdateOneAsync(r => r.ReservationId == id, updateReservationDef);

      // Release the slot
      var releaseSlotDef = Builders<EnergyBookingSlots>.Update
          .Set(s => s.Status, "Available")
          .Set(s => s.ReservationId, null)
          .Set(s => s.UpdatedAt, DateTime.UtcNow);

      await _mongoDbService.EnergyBookingSlots.UpdateOneAsync(s => s.SlotId == reservation.SlotId, releaseSlotDef);

      return Ok(new
      {
        Message = "Reservation cancelled successfully.",
        ReservationId = id,
        Status = ReservationStatus.Cancelled.ToString()
      });
    }


    // Get all Pending reservations for user GET: api/reservations/user/{nic}/pending?page=1&pageSize=10
    [HttpGet("user/{nic}/pending")]
    public async Task<IActionResult> GetPendingReservations(string nic, [FromQuery] int page = 1, [FromQuery] int pageSize = 10)
    {
      return await GetPaginatedReservationsByStatus(nic, ReservationStatus.Pending, page, pageSize);
    }


    // GET: api/reservations/user/{nic}/approved?page=1&pageSize=10
    [HttpGet("user/{nic}/approved")]
    public async Task<IActionResult> GetApprovedReservations(string nic, [FromQuery] int page = 1, [FromQuery] int pageSize = 10)
    {
      return await GetPaginatedReservationsByStatus(nic, ReservationStatus.Approved, page, pageSize);
    }


    // GET: api/reservations/user/{nic}/completed?page=1&pageSize=10
    [HttpGet("user/{nic}/completed")]
    public async Task<IActionResult> GetCompletedReservations(string nic, [FromQuery] int page = 1, [FromQuery] int pageSize = 10)
    {
      return await GetPaginatedReservationsByStatus(nic, ReservationStatus.Completed, page, pageSize);
    }


    // GET: api/reservations/user/{nic}/cancelled?page=1&pageSize=10
    [HttpGet("user/{nic}/cancelled")]
    public async Task<IActionResult> GetCancelledReservations(string nic, [FromQuery] int page = 1, [FromQuery] int pageSize = 10)
    {
      return await GetPaginatedReservationsByStatus(nic, ReservationStatus.Cancelled, page, pageSize);
    }


    // paginate and join station, slot details
    private async Task<IActionResult> GetPaginatedReservationsByStatus(string nic, ReservationStatus status, int page, int pageSize)
    {
      if (page < 1) page = 1;
      if (pageSize < 1) pageSize = 10;

      var filterBuilder = Builders<EnergyReservation>.Filter;
      var filter = filterBuilder.Eq(r => r.ProsumerNic, nic) & filterBuilder.Eq(r => r.Status, status);

      long totalCount = await _mongoDbService.EnergyReservations.CountDocumentsAsync(filter);

      var reservations = await _mongoDbService.EnergyReservations
          .Find(filter)
          .SortByDescending(r => r.BookingDate)
          .Skip((page - 1) * pageSize)
          .Limit(pageSize)
          .ToListAsync();

      // Fetch referenced station and slot details
      var stationIds = reservations.Select(r => r.StationId).Distinct().ToList();
      var slotIds = reservations.Select(r => r.SlotId).Distinct().ToList();

      var stations = await _mongoDbService.SolarStations
          .Find(s => stationIds.Contains(s.StationId))
          .ToListAsync();

      var slots = await _mongoDbService.EnergyBookingSlots
          .Find(s => slotIds.Contains(s.SlotId))
          .ToListAsync();

      var stationDict = stations.ToDictionary(s => s.StationId, s => s.StationName);
      var slotDict = slots.ToDictionary(s => s.SlotId, s => s.SlotNumber);

      var items = reservations.Select(r => new ReservationSummaryDto
      {
        ReservationId = r.ReservationId,
        ProsumerNic = r.ProsumerNic,
        StationId = r.StationId,
        StationName = stationDict.ContainsKey(r.StationId) ? stationDict[r.StationId] : "Unknown",
        SlotId = r.SlotId,
        SlotNumber = slotDict.ContainsKey(r.SlotId) ? slotDict[r.SlotId] : 0,
        BookingDate = r.BookingDate,
        StartTime = r.StartTime,
        EndTime = r.EndTime,
        Status = r.Status.ToString(),
        QrToken = r.QrToken,
        CreatedAt = r.CreatedAt,
        UpdatedAt = r.UpdatedAt
      }).ToList();

      return Ok(new
      {
        CurrentPage = page,
        PageSize = pageSize,
        TotalRecords = totalCount,
        TotalPages = (int)Math.Ceiling((double)totalCount / pageSize),
        Items = items
      });
    }

    // Combine date and time strings
    private DateTime ParseSlotDateTime(DateTime bookingDate, string timeString)
    {
      string[] formats = { "hh:mm tt", "h:mm tt", "HH:mm" };
      TimeSpan timeOfDay = TimeSpan.Zero;

      if (DateTime.TryParseExact(timeString.Trim(), formats, CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime parsedTime))
      {
        timeOfDay = parsedTime.TimeOfDay;
      }

      return new DateTime(bookingDate.Year, bookingDate.Month, bookingDate.Day, 0, 0, 0, DateTimeKind.Utc).Add(timeOfDay);
    }


    // Generate QR code for a reservation (POST: api/reservations/{id}/generate-qr)
    [HttpPost("{id}/generate-qr")]
    public async Task<IActionResult> GenerateReservationQr(string id)
    {
      // Fetch the reservation
      var reservation = await _mongoDbService.EnergyReservations
          .Find(r => r.ReservationId == id)
          .FirstOrDefaultAsync();

      // Check if reservation exists
      if (reservation == null)
      {
        return NotFound(new { Message = "Reservation not found." });
      }

      // Check if reservation is in pending status
      if (reservation.Status == ReservationStatus.Cancelled || reservation.Status == ReservationStatus.Completed)
      {
        return BadRequest(new { Message = "Cannot generate QR code for a cancelled or completed reservation." });
      }

      // Fetch the station and slot details
      var station = await _mongoDbService.SolarStations
          .Find(s => s.StationId == reservation.StationId)
          .FirstOrDefaultAsync();

      var slot = await _mongoDbService.EnergyBookingSlots
          .Find(s => s.SlotId == reservation.SlotId)
          .FirstOrDefaultAsync();

      DateTime generatedAt = DateTime.UtcNow;

      // Generate a payload for the QR code
      var qrPayloadObject = new
      {
        reservationId = reservation.ReservationId,
        prosumerNic = reservation.ProsumerNic,
        stationId = reservation.StationId,
        stationName = station?.StationName ?? "Solar Microgrid Hub",
        slotId = reservation.SlotId,
        slotNumber = slot?.SlotNumber ?? 0,
        bookingDate = reservation.BookingDate.ToString("yyyy-MM-dd"),
        startTime = reservation.StartTime,
        endTime = reservation.EndTime,
        status = reservation.Status.ToString(),
        generatedAt = generatedAt.ToString("o")
      };

      DotNetEnv.Env.Load();

      var secretKey = Environment.GetEnvironmentVariable("QR_JWT_SECRET");

      if (string.IsNullOrEmpty(secretKey))
      {
          throw new InvalidOperationException("QR JWT Secret is not configured in .env.");
      }

      string serializedPayload = JsonSerializer.Serialize(qrPayloadObject);
      string signature = ComputeHmacSha256(serializedPayload, secretKey);

      var fullQrData = new
      {
        data = qrPayloadObject,
        signature = signature
      };

      string qrTokenString = JsonSerializer.Serialize(fullQrData);

      var updateDef = Builders<EnergyReservation>.Update
          .Set(r => r.QrToken, qrTokenString)
          .Set(r => r.QrGeneratedAt, generatedAt)
          .Set(r => r.UpdatedAt, generatedAt);

      await _mongoDbService.EnergyReservations.UpdateOneAsync(r => r.ReservationId == id, updateDef);

      return Ok(new
      {
        Message = "QR code generated successfully.",
        ReservationId = id,
        QrToken = qrTokenString,
        GeneratedAt = generatedAt
      });
    }

    private string ComputeHmacSha256(string rawData, string key)
    {
      using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(key));
      byte[] hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(rawData));
      return Convert.ToBase64String(hash);
    }
  }
}