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
using Microsoft.AspNetCore.Authorization;
using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Threading.Tasks;

using System.Security.Cryptography;
using System.Security.Claims;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Configuration;

namespace SmartSolarMicrogrid.API.Controllers.member3
{
  [ApiController]
  [Route("api/reservations")]
  [Authorize]
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

      var startOfDayUtc = DateTime.SpecifyKind(dto.BookingDate.Date, DateTimeKind.Utc);
      var endOfDayUtc = startOfDayUtc.AddDays(1);

      string cleanStartTime = dto.StartTime.Trim();
      string altStartTime = cleanStartTime.StartsWith("0")
          ? cleanStartTime.Substring(1)
          : (cleanStartTime.Length == 7 ? "0" + cleanStartTime : cleanStartTime);

      // Check calendar conflict (same slot, date, and start time)
      var isSlotAlreadyReserved = await _mongoDbService.EnergyReservations
          .Find(r => r.SlotId == dto.SlotId
                  && r.BookingDate >= startOfDayUtc && r.BookingDate < endOfDayUtc
                  && (r.StartTime == cleanStartTime || r.StartTime == altStartTime)
                  && (r.Status == ReservationStatus.Pending || r.Status == ReservationStatus.Approved))
          .AnyAsync();

      if (isSlotAlreadyReserved)
      {
        return Conflict(new { Message = "This battery slot is already reserved for the selected date and time." });
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


    // Predefined 6 daily 2-hour slots (8:00 AM - 8:00 PM)
    private static readonly List<(string StartTime, string EndTime, string Label)> StandardDailySlots = new()
    {
      ("08:00 AM", "10:00 AM", "08:00 AM - 10:00 AM"),
      ("10:00 AM", "12:00 PM", "10:00 AM - 12:00 PM"),
      ("12:00 PM", "02:00 PM", "12:00 PM - 02:00 PM"),
      ("02:00 PM", "04:00 PM", "02:00 PM - 04:00 PM"),
      ("04:00 PM", "06:00 PM", "04:00 PM - 06:00 PM"),
      ("06:00 PM", "08:00 PM", "06:00 PM - 08:00 PM")
    };

    // Get available time slots on a date (GET: api/reservations/available-time-slots)
    [HttpGet("available-time-slots")]
    public async Task<IActionResult> GetAvailableTimeSlots(
        [FromQuery] string slotId,
        [FromQuery] DateTime date,
        [FromQuery] string? excludeReservationId = null)
    {
      if (string.IsNullOrWhiteSpace(slotId))
      {
        return BadRequest(new { Message = "Slot ID is required." });
      }

      DateTime now = DateTime.UtcNow;
      DateTime targetDateUtc = DateTime.SpecifyKind(date.Date, DateTimeKind.Utc);
      DateTime startOfDayUtc = targetDateUtc;
      DateTime endOfDayUtc = targetDateUtc.AddDays(1);

      // Both Pending and Approved status block the time slot
      var filterBuilder = Builders<EnergyReservation>.Filter;
      var filter = filterBuilder.Eq(r => r.SlotId, slotId)
                 & filterBuilder.Gte(r => r.BookingDate, startOfDayUtc)
                 & filterBuilder.Lt(r => r.BookingDate, endOfDayUtc)
                 & (filterBuilder.Eq(r => r.Status, ReservationStatus.Pending) | filterBuilder.Eq(r => r.Status, ReservationStatus.Approved));

      if (!string.IsNullOrWhiteSpace(excludeReservationId))
      {
        filter &= filterBuilder.Ne(r => r.ReservationId, excludeReservationId);
      }

      var existingReservations = await _mongoDbService.EnergyReservations.Find(filter).ToListAsync();

      var availableSlots = new List<AvailableTimeSlotDto>();

      foreach (var slot in StandardDailySlots)
      {
        // Check if already booked by another reservation (Pending or Approved)
        bool isBooked = existingReservations.Any(r =>
          string.Equals(r.StartTime.Trim(), slot.StartTime, StringComparison.OrdinalIgnoreCase) ||
          string.Equals(r.StartTime.Trim().TrimStart('0'), slot.StartTime.TrimStart('0'), StringComparison.OrdinalIgnoreCase)
        );

        if (isBooked)
        {
          continue;
        }

        // Check 12-hour minimum notice & past time
        DateTime slotScheduledDateTime = ParseSlotDateTime(targetDateUtc, slot.StartTime);
        if (slotScheduledDateTime < now || slotScheduledDateTime - now < TimeSpan.FromHours(12))
        {
          // Check if this slot belongs to the excluded reservation
          bool isCurrentHold = false;
          if (!string.IsNullOrWhiteSpace(excludeReservationId))
          {
            var excludedRes = await _mongoDbService.EnergyReservations.Find(r => r.ReservationId == excludeReservationId).FirstOrDefaultAsync();
            if (excludedRes != null && excludedRes.SlotId == slotId &&
                excludedRes.BookingDate >= startOfDayUtc && excludedRes.BookingDate < endOfDayUtc &&
                (string.Equals(excludedRes.StartTime.Trim(), slot.StartTime, StringComparison.OrdinalIgnoreCase) ||
                 string.Equals(excludedRes.StartTime.Trim().TrimStart('0'), slot.StartTime.TrimStart('0'), StringComparison.OrdinalIgnoreCase)))
            {
              isCurrentHold = true;
            }
          }

          if (!isCurrentHold)
          {
            continue;
          }
        }

        availableSlots.Add(new AvailableTimeSlotDto
        {
          Label = slot.Label,
          StartTime = slot.StartTime,
          EndTime = slot.EndTime
        });
      }

      return Ok(availableSlots);
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
        VerifiedAt = reservation.VerifiedAt,
        CompletedAt = reservation.CompletedAt,
        CancellationReason = reservation.CancellationReason,
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

      var updateStartOfDayUtc = DateTime.SpecifyKind(dto.BookingDate.Date, DateTimeKind.Utc);
      var updateEndOfDayUtc = updateStartOfDayUtc.AddDays(1);

      string cleanUpdateStartTime = dto.StartTime.Trim();
      string altUpdateStartTime = cleanUpdateStartTime.StartsWith("0")
          ? cleanUpdateStartTime.Substring(1)
          : (cleanUpdateStartTime.Length == 7 ? "0" + cleanUpdateStartTime : cleanUpdateStartTime);

      var hasConflict = await _mongoDbService.EnergyReservations
          .Find(r => r.ReservationId != id
                  && r.SlotId == dto.SlotId
                  && r.BookingDate >= updateStartOfDayUtc && r.BookingDate < updateEndOfDayUtc
                  && (r.StartTime == cleanUpdateStartTime || r.StartTime == altUpdateStartTime)
                  && (r.Status == ReservationStatus.Pending || r.Status == ReservationStatus.Approved))
          .AnyAsync();

      if (hasConflict)
      {
        return Conflict(new { Message = "The selected battery slot is already reserved by another booking for this date and time." });
      }

      // Update the reservation: reset to Pending with cleared QR and verification
      var updateReservationDef = Builders<EnergyReservation>.Update
          .Set(r => r.StationId, dto.StationId)
          .Set(r => r.SlotId, dto.SlotId)
          .Set(r => r.BookingDate, dto.BookingDate.Date)
          .Set(r => r.StartTime, dto.StartTime)
          .Set(r => r.EndTime, dto.EndTime)
          .Set(r => r.Status, ReservationStatus.Pending)
          .Set(r => r.QrToken, null)
          .Set(r => r.QrGeneratedAt, null)
          .Set(r => r.OperatorId, null)
          .Set(r => r.VerifiedAt, null)
          .Set(r => r.UpdatedAt, now);

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
        Status = ReservationStatus.Pending.ToString(),
        QrToken = null,
        OperatorId = null,
        VerifiedAt = null,
        CompletedAt = reservation.CompletedAt,
        CancellationReason = reservation.CancellationReason,
        CreatedAt = reservation.CreatedAt,
        UpdatedAt = now
      };

      return Ok(summary);
    }

    //  Approve Reservation (PUT: api/reservations/{id}/approve)
    [HttpPut("{id}/approve")]
    public async Task<IActionResult> ApproveReservation(string id, [FromQuery] string? operatorId = null)
    {
      // Fetch the reservation
      var reservation = await _mongoDbService.EnergyReservations.Find(r => r.ReservationId == id).FirstOrDefaultAsync();
     
      // Check if reservation exists
      if (reservation == null)
      {
        return NotFound(new { Message = "Reservation not found." });
      }

      // Check if reservation is in pending status
      if (reservation.Status != ReservationStatus.Pending)
      {
        return BadRequest(new { Message = $"Only pending reservations can be approved. Current status: {reservation.Status}" });
      }

      string? resolvedOperatorId = !string.IsNullOrWhiteSpace(operatorId)
          ? operatorId.Trim()
          : (User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub"));

      DateTime now = DateTime.UtcNow;

      // Generate the signed QR token on approval
      var (qrTokenString, qrGeneratedAt) = await CreateQrTokenAsync(reservation);

      var updateDef = Builders<EnergyReservation>.Update
          .Set(r => r.Status, ReservationStatus.Approved)
          .Set(r => r.QrToken, qrTokenString)
          .Set(r => r.QrGeneratedAt, qrGeneratedAt)
          .Set(r => r.OperatorId, resolvedOperatorId)
          .Set(r => r.UpdatedAt, now);

      await _mongoDbService.EnergyReservations.UpdateOneAsync(r => r.ReservationId == id, updateDef);

      return Ok(new
      {
        Message = "Reservation approved successfully.",
        ReservationId = id,
        Status = ReservationStatus.Approved.ToString(),
        OperatorId = resolvedOperatorId,
        QrToken = qrTokenString,
        QrGeneratedAt = qrGeneratedAt
      });
    }

    // Member 4 integration: retain the existing completion route and delegate authoritative checks.
    [HttpPut("{id}/complete")]
    [Microsoft.AspNetCore.Authorization.Authorize(Roles = "GridOperator,gridoperator")]
    public async Task<IActionResult> CompleteReservation(string id,
      [FromServices] SmartSolarMicrogrid.API.Services.member4.OperatorQrVerificationService qrService,
      System.Threading.CancellationToken ct)
    {
      // Operator identity comes exclusively from JWT; query-string operator IDs are never trusted.
      try { return Ok(await qrService.CompleteAsync(id, User, ct)); }
      catch (SmartSolarMicrogrid.API.DTOs.member4.QrVerificationException ex)
      { return StatusCode(ex.StatusCode, new { Message = ex.Message, Code = ex.Code }); }
      catch (Exception ex) when (ex is MongoException or TimeoutException)
      { return StatusCode(503, new { Message = "Completion could not be confirmed. Refresh reservations before trying again." }); }
    }

    // Cancel Reservation (PUT: api/reservations/{id}/cancel)
    [HttpPut("{id}/cancel")]
    public async Task<IActionResult> CancelReservation(string id, [FromBody] CancelReservationDto dto)
    {
      var reservation = await _mongoDbService.EnergyReservations.Find(r => r.ReservationId == id).FirstOrDefaultAsync();
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

      return Ok(new
      {
        Message = "Reservation cancelled successfully.",
        ReservationId = id,
        Status = ReservationStatus.Cancelled.ToString()
      });
    }

    // Delete Reservation permanently (DELETE: api/reservations/{id})
    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteReservation(string id)
    {
      var reservation = await _mongoDbService.EnergyReservations.Find(r => r.ReservationId == id).FirstOrDefaultAsync();
      if (reservation == null)
      {
        return NotFound(new { Message = "Reservation not found." });
      }

      await _mongoDbService.EnergyReservations.DeleteOneAsync(r => r.ReservationId == id);

      return Ok(new
      {
        Message = "Reservation permanently deleted from the database.",
        ReservationId = id
      });
    }

    // Get All Reservations for a User (Paginated) (GET: api/reservations/user/{nic})
    [HttpGet("user/{nic}")]
    public async Task<IActionResult> GetAllReservationsForUser(string nic, [FromQuery] int page = 1, [FromQuery] int pageSize = 10)
    {
      if (page < 1) page = 1;
      if (pageSize < 1) pageSize = 10;

      var filter = Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerNic, nic);
      return await FetchAndFormatPaginatedReservations(filter, page, pageSize);
    }

    // Get User Dashboard Counts (GET: api/reservations/user/{nic}/dashboard)
    [HttpGet("user/{nic}/dashboard")]
    public async Task<IActionResult> GetUserReservationDashboard(string nic)
    {
      var filterBuilder = Builders<EnergyReservation>.Filter;

      long total = await _mongoDbService.EnergyReservations.CountDocumentsAsync(filterBuilder.Eq(r => r.ProsumerNic, nic));
      long pending = await _mongoDbService.EnergyReservations.CountDocumentsAsync(
          filterBuilder.Eq(r => r.ProsumerNic, nic) & filterBuilder.Eq(r => r.Status, ReservationStatus.Pending));
      long approved = await _mongoDbService.EnergyReservations.CountDocumentsAsync(
          filterBuilder.Eq(r => r.ProsumerNic, nic) & filterBuilder.Eq(r => r.Status, ReservationStatus.Approved));
      long completed = await _mongoDbService.EnergyReservations.CountDocumentsAsync(
          filterBuilder.Eq(r => r.ProsumerNic, nic) & filterBuilder.Eq(r => r.Status, ReservationStatus.Completed));
      long cancelled = await _mongoDbService.EnergyReservations.CountDocumentsAsync(
          filterBuilder.Eq(r => r.ProsumerNic, nic) & filterBuilder.Eq(r => r.Status, ReservationStatus.Cancelled));

      return Ok(new
      {
        ProsumerNic = nic,
        TotalReservations = total,
        PendingCount = pending,
        ApprovedCount = approved,
        CompletedCount = completed,
        CancelledCount = cancelled
      });
    }

    // Get Station Dashboard Counts for Grid Operator (GET: api/reservations/station/{stationId}/dashboard)
    [HttpGet("station/{stationId}/dashboard")]
    public async Task<IActionResult> GetStationReservationDashboard(string stationId)
    {
      var station = await _mongoDbService.SolarStations.Find(s => s.StationId == stationId).FirstOrDefaultAsync();
      if (station == null)
      {
        return NotFound(new { Message = "Solar station not found." });
      }

      var filterBuilder = Builders<EnergyReservation>.Filter;

      long total = await _mongoDbService.EnergyReservations.CountDocumentsAsync(filterBuilder.Eq(r => r.StationId, stationId));
      long pending = await _mongoDbService.EnergyReservations.CountDocumentsAsync(
          filterBuilder.Eq(r => r.StationId, stationId) & filterBuilder.Eq(r => r.Status, ReservationStatus.Pending));
      long approved = await _mongoDbService.EnergyReservations.CountDocumentsAsync(
          filterBuilder.Eq(r => r.StationId, stationId) & filterBuilder.Eq(r => r.Status, ReservationStatus.Approved));
      long completed = await _mongoDbService.EnergyReservations.CountDocumentsAsync(
          filterBuilder.Eq(r => r.StationId, stationId) & filterBuilder.Eq(r => r.Status, ReservationStatus.Completed));
      long cancelled = await _mongoDbService.EnergyReservations.CountDocumentsAsync(
          filterBuilder.Eq(r => r.StationId, stationId) & filterBuilder.Eq(r => r.Status, ReservationStatus.Cancelled));

      return Ok(new
      {
        StationId = station.StationId,
        StationName = station.StationName,
        TotalReservations = total,
        PendingCount = pending,
        ApprovedCount = approved,
        CompletedCount = completed,
        CancelledCount = cancelled
      });
    }

    // Status-specific queries
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

      return await FetchAndFormatPaginatedReservations(filter, page, pageSize);
    }

    // Helper: Execute search & join metadata
    private async Task<IActionResult> FetchAndFormatPaginatedReservations(FilterDefinition<EnergyReservation> filter, int page, int pageSize)
    {
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

      // Fetch referenced station and slot details
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
        VerifiedAt = r.VerifiedAt,
        CompletedAt = r.CompletedAt,
        CancellationReason = r.CancellationReason,
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


    // Generate QR token payload and HMAC signature for a reservation
    private async Task<(string QrToken, DateTime GeneratedAt)> CreateQrTokenAsync(EnergyReservation reservation)
    {
      var station = await _mongoDbService.SolarStations.Find(s => s.StationId == reservation.StationId).FirstOrDefaultAsync();
      var slot = await _mongoDbService.EnergyBookingSlots.Find(s => s.SlotId == reservation.SlotId).FirstOrDefaultAsync();

      DateTime generatedAt = DateTime.UtcNow;

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
        status = ReservationStatus.Approved.ToString(),
        generatedAt = generatedAt.ToString("o")
      };

      DotNetEnv.Env.Load();

      var secretKey = Environment.GetEnvironmentVariable("QR_JWT_SECRET")
                   ?? _configuration["JwtSettings:Secret"]
                   ?? "SuperSecretKeyThatIsAtLeast32BytesLongForJWTAuthentication1234!!";

      string serializedPayload = JsonSerializer.Serialize(qrPayloadObject);
      string signature = ComputeHmacSha256(serializedPayload, secretKey);

      var fullQrData = new
      {
        data = qrPayloadObject,
        signature = signature
      };

      string qrTokenString = JsonSerializer.Serialize(fullQrData);
      return (qrTokenString, generatedAt);
    }

    // Generate QR code for a reservation (POST: api/reservations/{id}/generate-qr)
    [HttpPost("{id}/generate-qr")]
    public async Task<IActionResult> GenerateReservationQr(string id)
    {
      var reservation = await _mongoDbService.EnergyReservations.Find(r => r.ReservationId == id).FirstOrDefaultAsync();
      if (reservation == null)
      {
        return NotFound(new { Message = "Reservation not found." });
      }

      // Check if reservation is in pending status
      if (reservation.Status == ReservationStatus.Cancelled || reservation.Status == ReservationStatus.Completed)
      {
        return BadRequest(new { Message = "Cannot generate QR code for a cancelled or completed reservation." });
      }

      var (qrTokenString, generatedAt) = await CreateQrTokenAsync(reservation);

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