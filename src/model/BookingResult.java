package src.model;

public final class BookingResult {

    public enum Status {
        CONFIRMED,      // Room held, booking accepted
        DECLINED,       // No rooms available — guest informed
        INVALID_REQUEST // Bad input (null guest, past dates, etc.)
    }

    private final Reservation reservation;
    private final Status       status;
    private final String       message;
    private final double       totalCost;    // 0 if not CONFIRMED

    private BookingResult(Reservation reservation, Status status,
                          String message, double totalCost) {
        this.reservation = reservation;
        this.status      = status;
        this.message     = message;
        this.totalCost   = totalCost;
    }

    // -------------------------------------------------------------------------
    // Static factory methods — named constructors for readability
    // -------------------------------------------------------------------------

    public static BookingResult confirmed(Reservation r, double totalCost) {
        return new BookingResult(r, Status.CONFIRMED,
                "Booking confirmed for " + r.getGuestName() + ".", totalCost);
    }

    public static BookingResult declined(Reservation r, String reason) {
        return new BookingResult(r, Status.DECLINED, reason, 0);
    }

    public static BookingResult invalid(Reservation r, String reason) {
        return new BookingResult(r, Status.INVALID_REQUEST, reason, 0);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Reservation getReservation() { return reservation; }
    public Status      getStatus()      { return status; }
    public String      getMessage()     { return message; }
    public double      getTotalCost()   { return totalCost; }

    public boolean isConfirmed() { return status == Status.CONFIRMED; }

    @Override
    public String toString() {
        if (isConfirmed()) {
            return String.format("[%s] %s | Total: £%.2f",
                    status, message, totalCost);
        }
        return String.format("[%s] %s", status, message);
    }
}