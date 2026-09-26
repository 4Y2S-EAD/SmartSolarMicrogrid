/* Module: Grid Operator | Feature: QR verification and transfer completion
 * Member: Member 4
 * Purpose: Reads the existing Member 3 signed JSON QR format. */
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using MongoDB.Bson;
using SmartSolarMicrogrid.API.DTOs.member4;

namespace SmartSolarMicrogrid.API.Services.member4;

public static class ReservationQrCredential
{
    public static JsonElement Read(string? qrData, string? secret)
    {
        // Bound input and authenticate the exact JSON data bytes signed by the existing generator.
        if (string.IsNullOrWhiteSpace(qrData) || qrData.Length > 8192)
            throw new QrVerificationException(400, "INVALID_QR", "A valid reservation QR code is required.");
        if (string.IsNullOrWhiteSpace(secret))
            throw new QrVerificationException(503, "QR_UNAVAILABLE", "QR verification is temporarily unavailable.");
        try
        {
            using var document = JsonDocument.Parse(qrData, new JsonDocumentOptions { MaxDepth = 4 });
            var root = document.RootElement;
            RequireProperties(root, ["data", "signature"]);
            var data = root.GetProperty("data");
            RequireProperties(data, ["reservationId", "prosumerNic", "stationId", "stationName",
                "slotId", "slotNumber", "bookingDate", "startTime", "endTime", "status", "generatedAt"]);
            foreach (var property in data.EnumerateObject())
            {
                if (property.Name == "slotNumber")
                {
                    if (!property.Value.TryGetInt32(out var number) || number < 1) throw new FormatException();
                }
                else if (property.Value.ValueKind != JsonValueKind.String ||
                         string.IsNullOrWhiteSpace(property.Value.GetString())) throw new FormatException();
            }
            foreach (var key in new[] { "reservationId", "stationId", "slotId" })
                if (!ObjectId.TryParse(data.GetProperty(key).GetString(), out _)) throw new FormatException();
            var signature = Convert.FromBase64String(root.GetProperty("signature").GetString()!);
            var expected = HMACSHA256.HashData(Encoding.UTF8.GetBytes(secret),
                Encoding.UTF8.GetBytes(data.GetRawText()));
            if (!CryptographicOperations.FixedTimeEquals(signature, expected)) throw new FormatException();
            return data.Clone();
        }
        catch (Exception ex) when (ex is JsonException or FormatException or InvalidOperationException or ArgumentException)
        {
            throw new QrVerificationException(400, "INVALID_QR", "This is not an authentic reservation QR code.");
        }
    }

    private static void RequireProperties(JsonElement value, string[] names)
    {
        // Reject ambiguous duplicate keys and unexpected formats instead of accepting the last JSON value.
        if (value.ValueKind != JsonValueKind.Object) throw new FormatException();
        var actual = value.EnumerateObject().Select(p => p.Name).ToArray();
        if (actual.Length != names.Length || actual.Distinct().Count() != names.Length ||
            names.Except(actual).Any()) throw new FormatException();
    }
}
