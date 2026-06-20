package src.services;

import src.model.BookingResult;
import src.model.Reservation;
import src.model.RoomType;
import src.model.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportingService {

    // Complete ordered history of every confirmed reservation.
    // ArrayList preserves insertion (= confirmation) order — index 0 is the
    // very first booking ever confirmed by the system.
    private final List<Reservation> bookingHistory;

    // Soft-delete tracking: reservationId → cancelled flag.
    // Cancelled bookings stay in bookingHistory (audit trail intact) but
    // are excluded from "active" reports.
    private final Map<String, Boolean> cancelledReservations;

    public ReportingService() {
        this.bookingHistory        = new ArrayList<>();
        this.cancelledReservations = new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Recording
    // -------------------------------------------------------------------------

    /**
     * Appends a confirmed reservation to the permanent booking history.
     * Should be called immediately after a BookingResult is CONFIRMED.
     *
     * @param reservation The confirmed reservation to record
     */
    public void recordBooking(Reservation reservation) {
        bookingHistory.add(reservation);   // O(1) amortised append
        System.out.printf("[History] ✓ Recorded → %s%n", reservation);
    }

    /**
     * Convenience overload: records a booking directly from a BookingResult,
     * but only if it was actually confirmed.
     *
     * @param result The result returned by the booking pipeline
     * @return true if recorded, false if the result was not CONFIRMED
     */
    public boolean recordIfConfirmed(BookingResult result) {
        if (result == null || !result.isConfirmed()) {
            return false;
        }
        recordBooking(result.getReservation());
        return true;
    }

    // -------------------------------------------------------------------------
    // Cancellation & Review
    // -------------------------------------------------------------------------

    /**
     * Marks a reservation as cancelled without removing it from history.
     * This preserves the audit trail — admins can see what was booked and
     * later cancelled, which is essential for dispute resolution.
     *
     * @param reservationId ID of the reservation to cancel
     * @return true if found and cancelled, false if not found in history
     */
    public boolean cancelBooking(String reservationId) {
        boolean exists = bookingHistory.stream()
                .anyMatch(r -> r.getReservationId().equals(reservationId));

        if (!exists) {
            System.out.printf("[History] ✗ Cannot cancel — reservation %s not found.%n",
                    reservationId);
            return false;
        }

        cancelledReservations.put(reservationId, true);
        System.out.printf("[History] ⊘ Cancelled reservation %s (record retained for audit).%n",
                reservationId);
        return true;
    }

    /**
     * Checks whether a given reservation has been cancelled.
     */
    public boolean isCancelled(String reservationId) {
        return cancelledReservations.getOrDefault(reservationId, false);
    }

    /**
     * Retrieves a single reservation by ID for review.
     *
     * @param reservationId ID to look up
     * @return The Reservation, or null if not found in history
     */
    public Reservation findById(String reservationId) {
        return bookingHistory.stream()
                .filter(r -> r.getReservationId().equals(reservationId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds all reservations made by a given guest (case-insensitive).
     * Useful for customer support — "show me everything this guest booked."
     */
    public List<Reservation> findByGuest(String guestName) {
        List<Reservation> matches = new ArrayList<>();
        for (Reservation r : bookingHistory) {
            if (r.getGuestName().equalsIgnoreCase(guestName)) {
                matches.add(r);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    // -------------------------------------------------------------------------
    // Reporting
    // -------------------------------------------------------------------------

    /**
     * Returns the full, unmodifiable booking history in chronological order.
     */
    public List<Reservation> getHistory() {
        return Collections.unmodifiableList(bookingHistory);
    }

    /**
     * Returns only active (non-cancelled) reservations.
     */
    public List<Reservation> getActiveBookings() {
        List<Reservation> active = new ArrayList<>();
        for (Reservation r : bookingHistory) {
            if (!isCancelled(r.getReservationId())) {
                active.add(r);
            }
        }
        return Collections.unmodifiableList(active);
    }

    /**
     * Counts how many bookings were made for each room type.
     * Demonstrates simple aggregation over the List<Reservation>.
     */
    public Map<RoomType, Integer> countByRoomType() {
        Map<RoomType, Integer> counts = new HashMap<>();
        for (Reservation r : bookingHistory) {
            counts.merge(r.getRoomType(), 1, Integer::sum);
        }
        return counts;
    }

    /**
     * Returns all reservations with a check-in date on or after the given date.
     * Useful for "upcoming stays" reports.
     */
    public List<Reservation> findUpcoming(LocalDate fromDate) {
        List<Reservation> upcoming = new ArrayList<>();
        for (Reservation r : bookingHistory) {
            if (!r.getCheckIn().isBefore(fromDate) && !isCancelled(r.getReservationId())) {
                upcoming.add(r);
            }
        }
        return Collections.unmodifiableList(upcoming);
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    /**
     * Prints the complete booking history in chronological (insertion) order.
     */
    public void printFullHistory() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          UC6 — Complete Booking History (List<Reservation>) ║");
        System.out.printf( "║  Total records: %-46d ║%n", bookingHistory.size());
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        if (bookingHistory.isEmpty()) {
            System.out.println("║  (No bookings recorded yet.)");
        } else {
            int idx = 1;
            for (Reservation r : bookingHistory) {
                String tag = isCancelled(r.getReservationId()) ? "[CANCELLED]" : "[ACTIVE]   ";
                System.out.printf("║  %-3d %s %s%n", idx++, tag, r);
            }
        }
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    /**
     * Prints a summary report: counts by room type, active vs cancelled.
     */
    public void printSummaryReport() {
        int total      = bookingHistory.size();
        int cancelled  = (int) bookingHistory.stream()
                .filter(r -> isCancelled(r.getReservationId()))
                .count();
        int active     = total - cancelled;

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                UC6 — Booking Summary Report                 ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  Total bookings ever made : %-35d ║%n", total);
        System.out.printf( "║  Active bookings          : %-35d ║%n", active);
        System.out.printf( "║  Cancelled bookings       : %-35d ║%n", cancelled);
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Bookings by Room Type:");

        Map<RoomType, Integer> byType = countByRoomType();
        for (RoomType type : RoomType.values()) {
            int count = byType.getOrDefault(type, 0);
            System.out.printf("║    %-12s : %-38d ║%n", type.getDisplayName(), count);
        }
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    /**
     * Prints all bookings for a specific guest — used for customer support lookups.
     */
    public void printGuestHistory(String guestName) {
        List<Reservation> matches = findByGuest(guestName);

        System.out.printf("%n  [Guest History] %s — %d booking(s) found:%n",
                guestName, matches.size());
        if (matches.isEmpty()) {
            System.out.println("    (No bookings found for this guest.)");
            return;
        }
        for (Reservation r : matches) {
            String tag = isCancelled(r.getReservationId()) ? "[CANCELLED]" : "[ACTIVE]   ";
            System.out.printf("    %s %s%n", tag, r);
        }
    }
}