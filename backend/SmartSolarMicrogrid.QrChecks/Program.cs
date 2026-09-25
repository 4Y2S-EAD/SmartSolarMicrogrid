/* Module: Grid Operator | Feature: QR verification checks | Member: Member 4
 * Purpose: Real HTTP/service/MongoDB checks in a uniquely named, disposable test database. */
using System.IdentityModel.Tokens.Jwt;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.IdentityModel.Tokens;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Controllers.member4;
using SmartSolarMicrogrid.API.DTOs.member4;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;
using SmartSolarMicrogrid.API.Services.member4;

var secret = Convert.ToBase64String(RandomNumberGenerator.GetBytes(48));
var data = new {
    reservationId = "66f200000000000000000001", prosumerNic = "qr-check-prosumer",
    stationId = "66f200000000000000000002", stationName = "QR check station",
    slotId = "66f200000000000000000003", slotNumber = 1,
    bookingDate = "2026-09-25", startTime = "09:00 AM", endTime = "10:00 AM",
    status = "Approved", generatedAt = DateTime.UtcNow.ToString("o")
};
string Sign(object value) {
    // Reproduce the existing generator's serialization and HMAC format, using only a random test secret.
    var json = JsonSerializer.Serialize(value);
    return JsonSerializer.Serialize(new { data = value, signature = Convert.ToBase64String(
        HMACSHA256.HashData(Encoding.UTF8.GetBytes(secret), Encoding.UTF8.GetBytes(json))) });
}
var qr = Sign(data);
var count = 0;
void Check(bool pass, string name) {
    // Fail immediately and print only test descriptions, never credentials or QR contents.
    if (!pass) throw new Exception("FAILED: " + name);
    count++; Console.WriteLine("PASS " + name);
}
Check(ReservationQrCredential.Read(qr, secret).GetProperty("reservationId").GetString() == data.reservationId, "existing signed QR format");
foreach (var invalid in new string?[] { null, "", "not-json", "{}", new string('x', 8193),
    qr.Replace("09:00 AM", "11:00 AM"), qr.Replace("\"signature\":", "\"signature\":\"x\",\"signature\":"),
    qr.Replace(data.reservationId, "not-an-object-id") }) {
    try { ReservationQrCredential.Read(invalid, secret); throw new Exception("Accepted invalid QR"); }
    catch (QrVerificationException ex) { Check(ex.StatusCode == 400, "reject malformed/tampered QR"); }
}
var deviceMode = args.Contains("--device");
var artifactDirectory = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "../../../../../android/app/build/qr-device"));
if (!args.Contains("--integration") && !deviceMode) { Console.WriteLine($"{count} checks passed. Use --integration for isolated MongoDB/HTTP checks."); return; }

// Load the project's MongoDB connection without printing it, then isolate every write in a new database.
var apiDirectory = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "../../../../SmartSolarMicrogrid.API"));
var configuration = new ConfigurationBuilder().SetBasePath(apiDirectory).AddJsonFile("appsettings.json").Build();
DotNetEnv.Env.Load(Path.Combine(apiDirectory, ".env"));
var connection = Environment.GetEnvironmentVariable("MONGODB_CONNECTION_STRING") ?? configuration["MongoDbSettings:ConnectionString"]!;
var databaseName = "m4qr_" + Guid.NewGuid().ToString("N");
var scratch = Path.Combine(Path.GetTempPath(), databaseName);
Directory.CreateDirectory(scratch);
var oldDirectory = Environment.CurrentDirectory;
Environment.CurrentDirectory = scratch;
Environment.SetEnvironmentVariable("MONGODB_CONNECTION_STRING", connection);
Environment.SetEnvironmentVariable("MONGODB_DATABASE_NAME", databaseName);
Environment.SetEnvironmentVariable("QR_JWT_SECRET", secret);
var db = new MongoDbService(configuration);
var builder = WebApplication.CreateBuilder();
builder.Logging.ClearProviders();
builder.WebHost.UseSetting("urls", "http://127.0.0.1:5236");
builder.Services.AddSingleton(db);
builder.Services.AddScoped<OperatorQrVerificationService>();
builder.Services.AddScoped<OperatorReservationService>();
builder.Services.AddScoped<StationMapService>();
builder.Services.AddControllers().AddApplicationPart(typeof(OperatorQrController).Assembly);
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer(options => {
    options.TokenValidationParameters = new() { ValidateIssuer = false, ValidateAudience = false,
        ValidateIssuerSigningKey = true, IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secret)),
        ValidateLifetime = true, ClockSkew = TimeSpan.Zero };
});
builder.Services.AddAuthorization();
await using var app = builder.Build();
app.UseAuthentication(); app.UseAuthorization(); app.MapControllers();
using var http = new HttpClient { BaseAddress = new Uri("http://127.0.0.1:5236/api/") };
string Token(string role = "GridOperator", string nic = "qr-check-operator") {
    // Mint short-lived test JWTs accepted only by this temporary HTTP host.
    return new JwtSecurityTokenHandler().WriteToken(new JwtSecurityToken(claims: [new("sub", nic), new(ClaimTypes.Role, role)],
        expires: DateTime.UtcNow.AddMinutes(35), signingCredentials: new(new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secret)), SecurityAlgorithms.HmacSha256)));
}
void Authenticate(string? token) { http.DefaultRequestHeaders.Authorization = token == null ? null : new AuthenticationHeaderValue("Bearer", token); }
var now = DateTime.UtcNow;
var reservation = new EnergyReservation { ReservationId = data.reservationId, ProsumerNic = data.prosumerNic,
    StationId = data.stationId, SlotId = data.slotId, BookingDate = DateTime.UtcNow.Date.AddDays(2),
    StartTime = data.startTime, EndTime = data.endTime, Status = ReservationStatus.Approved, CreatedAt = now, UpdatedAt = now };
var station = new SolarStationInfo { StationId = data.stationId, StationName = data.stationName, Status = "Active", CapacityKwh = 100, BatterySlotCount = 1, AvailableSlotCount = 1 };
var slot = new EnergyBookingSlots { SlotId = data.slotId, StationId = data.stationId, SlotNumber = 1,
    BookingDate = reservation.BookingDate, StartTime = data.startTime, EndTime = data.endTime, CapacityKwh = 20, Status = "Available" };
async Task<string> Generate() {
    // Exercise the real existing Member 3 QR generator, including persistence.
    var response = await http.PostAsync($"reservations/{data.reservationId}/generate-qr", null);
    response.EnsureSuccessStatusCode();
    return (await response.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("qrToken").GetString()!;
}
async Task<HttpResponseMessage> Verify(string? value) => await http.PostAsJsonAsync("operator/verify-qr", new { qrData = value });
async Task ExpectReject(string value, string code) {
    // Check controlled server rejection and confirm it never completes the reservation.
    using var response = await Verify(value);
    var body = await response.Content.ReadFromJsonAsync<JsonElement>();
    Check(!response.IsSuccessStatusCode && body.GetProperty("result").GetString() == "REJECTED" && body.GetProperty("code").GetString() == code, code);
}
try {
    await db.Users.InsertManyAsync([
        new User { NIC = "qr-check-operator", FullName = "Test operator", Email = "operator@example.invalid", PasswordHash = "unused", Role = UserRole.GridOperator, AccountStatus = AccountStatus.Active },
        new User { NIC = data.prosumerNic, FullName = "Test prosumer", Email = "prosumer@example.invalid", PasswordHash = "unused", Role = UserRole.Prosumer, AccountStatus = AccountStatus.Active }
    ]);
    await db.SolarStations.InsertOneAsync(station); await db.EnergyBookingSlots.InsertOneAsync(slot);
    await db.EnergyReservations.InsertOneAsync(reservation);
    await app.StartAsync();
    qr = await Generate();
    if (deviceMode) {
        // Support physical camera acceptance using a real generated QR and real API, never inject a decoded value into the scanner.
        Directory.CreateDirectory(artifactDirectory);
        var fixturePath = Path.Combine(artifactDirectory, "qr-device.json");
        var stopPath = Path.Combine(artifactDirectory, "stop");
        if (File.Exists(stopPath)) File.Delete(stopPath);
        await File.WriteAllTextAsync(fixturePath, JsonSerializer.Serialize(new { token = Token(), reservationId = data.reservationId, qrData = qr }));
        Console.WriteLine("DEVICE FIXTURE READY on loopback port 5236. Test credentials are in ignored android/app/build/qr-device/qr-device.json.");
        var deadline = DateTime.UtcNow.AddMinutes(30);
        while (!File.Exists(stopPath) && DateTime.UtcNow < deadline) await Task.Delay(1000);
        File.Delete(fixturePath);
        return;
    }
    Check((await Verify(qr)).StatusCode == HttpStatusCode.Unauthorized, "verify requires authentication");
    Check((await http.PutAsync($"reservations/{data.reservationId}/complete", null)).StatusCode == HttpStatusCode.Unauthorized, "completion requires authentication");
    Authenticate(Token("Prosumer", data.prosumerNic));
    Check((await Verify(qr)).StatusCode == HttpStatusCode.Forbidden, "non-operator forbidden");
    Authenticate(Token(nic: "missing-operator")); await ExpectReject(qr, "OPERATOR_INACTIVE");
    Authenticate(Token());
    Check((await http.PutAsync($"reservations/{data.reservationId}/complete", null)).StatusCode == HttpStatusCode.Conflict, "cannot complete without server verification");
    await ExpectReject("", "INVALID_QR"); await ExpectReject("not-a-QR", "INVALID_QR");
    await ExpectReject(qr.Replace("09:00 AM", "11:00 AM"), "INVALID_QR");
    var missingData = JsonSerializer.Deserialize<Dictionary<string, object>>(JsonSerializer.Serialize(data))!;
    missingData["reservationId"] = "66f200000000000000000099";
    await ExpectReject(Sign(missingData), "NOT_FOUND");
    foreach (var status in new[] { ReservationStatus.Pending, ReservationStatus.Cancelled, ReservationStatus.Completed }) {
        await db.EnergyReservations.UpdateOneAsync(r => r.ReservationId == data.reservationId, Builders<EnergyReservation>.Update.Set(r => r.Status, status));
        await ExpectReject(qr, "INVALID_STATUS");
    }
    await db.EnergyReservations.UpdateOneAsync(r => r.ReservationId == data.reservationId, Builders<EnergyReservation>.Update.Set(r => r.Status, ReservationStatus.Approved));
    await db.Users.UpdateOneAsync(u => u.NIC == data.prosumerNic, Builders<User>.Update.Set(u => u.AccountStatus, AccountStatus.Deactivated));
    await ExpectReject(qr, "INVALID_PROSUMER");
    await db.Users.UpdateOneAsync(u => u.NIC == data.prosumerNic, Builders<User>.Update.Set(u => u.AccountStatus, AccountStatus.Active));
    await db.SolarStations.DeleteOneAsync(s => s.StationId == data.stationId); await ExpectReject(qr, "INVALID_STATION"); await db.SolarStations.InsertOneAsync(station);
    await db.EnergyBookingSlots.UpdateOneAsync(s => s.SlotId == data.slotId, Builders<EnergyBookingSlots>.Update.Set(s => s.StationId, "66f200000000000000000099"));
    await ExpectReject(qr, "INVALID_SLOT"); await db.EnergyBookingSlots.ReplaceOneAsync(s => s.SlotId == data.slotId, slot);
    await db.EnergyBookingSlots.UpdateOneAsync(s => s.SlotId == data.slotId, Builders<EnergyBookingSlots>.Update.Set(s => s.StartTime, "08:00 AM"));
    await ExpectReject(qr, "INVALID_SCHEDULE"); await db.EnergyBookingSlots.ReplaceOneAsync(s => s.SlotId == data.slotId, slot);
    var oldQr = qr; qr = await Generate(); await ExpectReject(oldQr, "STALE_QR");
    using (var valid = await Verify(qr)) {
        var body = await valid.Content.ReadFromJsonAsync<JsonElement>();
        Check(valid.IsSuccessStatusCode && body.GetProperty("result").GetString() == "VALID" && body.GetProperty("reservation").GetProperty("prosumerName").GetString() == "Test prosumer", "valid QR returns authoritative details");
    }
    Check((await db.EnergyReservations.Find(r => r.ReservationId == data.reservationId).FirstAsync()).Status == ReservationStatus.Approved, "verification does not complete");
    qr = await Generate();
    Check((await http.PutAsync($"reservations/{data.reservationId}/complete", null)).StatusCode == HttpStatusCode.Conflict, "QR regeneration invalidates prior verification");
    Check((await Verify(qr)).IsSuccessStatusCode, "fresh scan verifies");
    await db.Users.UpdateOneAsync(u => u.NIC == data.prosumerNic, Builders<User>.Update.Set(u => u.AccountStatus, AccountStatus.Deactivated));
    Check((await http.PutAsync($"reservations/{data.reservationId}/complete", null)).StatusCode == HttpStatusCode.Conflict, "completion revalidates prosumer");
    await db.Users.UpdateOneAsync(u => u.NIC == data.prosumerNic, Builders<User>.Update.Set(u => u.AccountStatus, AccountStatus.Active));
    var completions = await Task.WhenAll(Enumerable.Range(0, 6).Select(_ => http.PutAsync($"reservations/{data.reservationId}/complete?operatorId=spoofed", null)));
    Check(completions.Count(r => r.IsSuccessStatusCode) == 1 && completions.All(r => r.IsSuccessStatusCode || r.StatusCode == HttpStatusCode.Conflict), "concurrent completion succeeds exactly once");
    var completed = await db.EnergyReservations.Find(r => r.ReservationId == data.reservationId).FirstAsync();
    Check(completed.Status == ReservationStatus.Completed && completed.OperatorId == "qr-check-operator" && completed.VerifiedAt != null && completed.CompletedAt != null, "completion records JWT operator and timestamps");
    await ExpectReject(qr, "INVALID_STATUS");
    var operatorPage = await http.GetFromJsonAsync<JsonElement>("operator/reservations?status=Completed");
    Check(operatorPage.ToString().Contains(data.reservationId), "operator mobile/web read includes completed reservation");
    var prosumerPage = await http.GetFromJsonAsync<JsonElement>($"reservations/user/{data.prosumerNic}/completed");
    Check(prosumerPage.ToString().Contains(data.reservationId), "existing prosumer completed read reflects transfer");
    Console.WriteLine($"{count} checks passed; production collections were not modified.");
    if (args.Contains("--web")) {
        // Keep the completed fixture available for the existing web UI's real GET requests.
        Directory.CreateDirectory(artifactDirectory);
        var fixturePath = Path.Combine(artifactDirectory, "qr-device.json");
        var stopPath = Path.Combine(artifactDirectory, "stop");
        if (File.Exists(stopPath)) File.Delete(stopPath);
        await File.WriteAllTextAsync(fixturePath, JsonSerializer.Serialize(new { token = Token(), reservationId = data.reservationId }));
        Console.WriteLine("WEB COMPLETION FIXTURE READY on loopback port 5236.");
        var deadline = DateTime.UtcNow.AddMinutes(30);
        while (!File.Exists(stopPath) && DateTime.UtcNow < deadline) await Task.Delay(1000);
        File.Delete(fixturePath);
    }
}
finally {
    // Delete only this process's randomly named test database, never the configured application database.
    await app.StopAsync();
    if (databaseName.StartsWith("m4qr_") && Guid.TryParseExact(databaseName[5..], "N", out _))
        await new MongoClient(connection).DropDatabaseAsync(databaseName);
    Environment.CurrentDirectory = oldDirectory;
    Directory.Delete(scratch, false);
}
