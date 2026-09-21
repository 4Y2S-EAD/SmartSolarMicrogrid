using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.member3
{
    public class UpdateReservationDto
    {
        [Required(ErrorMessage = "Station ID is required.")]
        public string StationId { get; set; } = null!;

        [Required(ErrorMessage = "Slot ID is required.")]
        public string SlotId { get; set; } = null!;

        [Required(ErrorMessage = "Booking date is required.")]
        public DateTime BookingDate { get; set; }

        [Required(ErrorMessage = "Start time is required.")]
        [RegularExpression(@"^(0?[1-9]|1[0-2]):[0-5][0-9]\s?(AM|PM)$", ErrorMessage = "Start time must be in 12-hour format")]
        public string StartTime { get; set; } = null!;

        [Required(ErrorMessage = "End time is required.")]
        [RegularExpression(@"^(0?[1-9]|1[0-2]):[0-5][0-9]\s?(AM|PM)$", ErrorMessage = "End time must be in 12-hour format")]
        public string EndTime { get; set; } = null!;
    }
}