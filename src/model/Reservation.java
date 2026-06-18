package src.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class Reservation {

    /** Unique identifier — generated at request time, not at confirmation. */
    private final String    reservationId;

    private final String    guestName;
    private final RoomType  roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    /** Timestamp of when the request entered the system — drives FIFO ordering. */
    private final long      requestedAtMillis;

    public Reservation(String guestName, RoomType roomType,
                       LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut);

        this.reservationId     = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.guestName         = guestName;
        this.roomType          = roomType;
        this.checkIn           = checkIn;
        this.checkOut          = checkOut;
        this.requestedAtMillis = System.currentTimeMillis();
    }

    // -------------------------------------------------------------------------
    // Derived properties
    // -------------------------------------------------------------------------

    /** Number of nights between check-in and check-out. */
    public long getNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public double estimatedTotal(double pricePerNight) {
        return getNights() * pricePerNight;
    }

    // -------------------------------------------------------------------------
    // Accessors (no setters — immutable)
    // -------------------------------------------------------------------------

    public String    getReservationId()      { return reservationId; }
    public String    getGuestName()          { return guestName; }
    public RoomType  getRoomType()           { return roomType; }
    public LocalDate getCheckIn()            { return checkIn; }
    public LocalDate getCheckOut()           { return checkOut; }
    public long      getRequestedAtMillis()  { return requestedAtMillis; }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and check-out dates cannot be null.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(
                    "Check-out must be after check-in. Got: " + checkIn + " → " + checkOut);
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s → %s (%d night%s)",
                reservationId,
                guestName,
                roomType.getDisplayName(),
                checkIn, checkOut,
                getNights(), getNights() == 1 ? "" : "s");
    }
}