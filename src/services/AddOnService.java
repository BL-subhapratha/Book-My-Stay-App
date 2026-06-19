package src.services;

import src.model.BookingResult;
import src.model.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AddOnService {

    // Primary data structure: reservation ID → ordered list of selected services
    // LinkedHashMap preserves insertion order for consistent receipt printing
    private final Map<String, List<Service>> servicesByReservation;

    // Reference to confirmed bookings — used to validate before attaching services
    private final List<BookingResult> confirmedBookings;

    public AddOnService(List<BookingResult> confirmedBookings) {
        this.confirmedBookings    = confirmedBookings;
        this.servicesByReservation = new LinkedHashMap<>();
    }

    // -------------------------------------------------------------------------
    // Core Operations
    // -------------------------------------------------------------------------

    /**
     * Attaches an add-on service to a confirmed reservation.
     *
     * One-to-many mapping in action:
     *   computeIfAbsent() creates a new ArrayList the first time a reservation
     *   ID is seen, then add() appends each subsequent service to the same list.
     *   Result: Map<reservationId, [Service, Service, ...]>
     *
     * @param reservationId  ID of a confirmed reservation
     * @param type           The service to add
     * @param quantity       Units required (e.g. 2 breakfasts for 2 guests)
     * @return true if added successfully, false if reservation not confirmed
     */
    public boolean addService(String reservationId, Service.Type type, int quantity) {
        if (!isConfirmed(reservationId)) {
            System.out.printf("[AddOn] ✗ Reservation %-10s not found or not confirmed. "
                    + "Services can only be added to confirmed bookings.%n", reservationId);
            return false;
        }

        Service service = new Service(reservationId, type, quantity);

        // computeIfAbsent — creates the List on first service, reuses it on subsequent ones
        // This is the one-to-many mapping: one key → growing List<Service>
        servicesByReservation
                .computeIfAbsent(reservationId, k -> new ArrayList<>())
                .add(service);

        System.out.printf("[AddOn] ✓ Added to %-10s → %s%n", reservationId, service);
        return true;
    }

    /**
     * Removes the most recently added service for a reservation (last-in, first-out).
     * Useful when a guest changes their mind immediately after selecting.
     *
     * @param reservationId Reservation to remove the last service from
     * @return The removed Service, or null if no services exist
     */
    public Service removeLastService(String reservationId) {
        List<Service> services = servicesByReservation.get(reservationId);

        if (services == null || services.isEmpty()) {
            System.out.printf("[AddOn] Nothing to remove for reservation %s.%n",
                    reservationId);
            return null;
        }

        Service removed = services.remove(services.size() - 1);   // O(1) for ArrayList tail
        System.out.printf("[AddOn] ↩ Removed from %-10s → %s%n", reservationId, removed);
        return removed;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of all services attached to a reservation.
     * Returns an empty list if no services have been added or ID not found.
     */
    public List<Service> getServices(String reservationId) {
        return Collections.unmodifiableList(
                servicesByReservation.getOrDefault(reservationId, Collections.emptyList()));
    }

    /**
     * Calculates the total additional cost of all services for a reservation.
     *
     * @param reservationId Target reservation
     * @return Sum of Service.getTotalPrice() for every attached service
     */
    public double getTotalAddOnCost(String reservationId) {
        return getServices(reservationId)
                .stream()
                .mapToDouble(Service::getTotalPrice)
                .sum();
    }

    /**
     * Returns the full map (unmodifiable) for use by downstream services (UC6).
     */
    public Map<String, List<Service>> getAllServices() {
        return Collections.unmodifiableMap(servicesByReservation);
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    /**
     * Prints an itemised add-on receipt for a single reservation.
     */
    public void printServiceReceipt(String reservationId) {
        List<Service> services = getServices(reservationId);

        System.out.println("\n  ┌─────────────────────────────────────────────────────────┐");
        System.out.printf( "  │  Add-On Receipt  —  Reservation: %-22s│%n", reservationId);
        System.out.println("  └─────────────────────────────────────────────────────────┘");

        if (services.isEmpty()) {
            System.out.println("    (No add-on services selected.)");
            return;
        }

        for (Service s : services) {
            System.out.printf("    • %s%n", s);
        }

        System.out.println("    " + "─".repeat(52));
        System.out.printf( "    Add-On Total: £%.2f%n", getTotalAddOnCost(reservationId));
    }

    /**
     * Prints a summary of all reservations that have add-ons attached.
     * Iterates in insertion order (LinkedHashMap guarantee).
     */
    public void printAllServiceSummaries() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          UC5 — Add-On Services Summary                      ║");
        System.out.println("║  Map<String, List<Service>>  (LinkedHashMap)                ║");
        System.out.println("║  Insertion order preserved — services print as selected     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        if (servicesByReservation.isEmpty()) {
            System.out.println("║  (No add-on services recorded.)");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            return;
        }

        double grandTotal = 0;

        // LinkedHashMap.entrySet() iterates in insertion order
        for (Map.Entry<String, List<Service>> entry : servicesByReservation.entrySet()) {
            String        resId    = entry.getKey();
            List<Service> services = entry.getValue();
            double        subtotal = services.stream()
                                             .mapToDouble(Service::getTotalPrice)
                                             .sum();
            grandTotal += subtotal;

            System.out.printf("║%n║  Reservation: %s%n", resId);
            for (Service s : services) {
                System.out.printf("║    • %s%n", s);
            }
            System.out.printf("║    Subtotal: £%.2f%n", subtotal);
        }

        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  Grand Total (all add-ons): £%-30.2f ║%n", grandTotal);
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    /**
     * Checks whether a given reservation ID exists in the confirmed bookings list.
     */
    private boolean isConfirmed(String reservationId) {
        return confirmedBookings.stream()
                .filter(BookingResult::isConfirmed)
                .anyMatch(r -> r.getReservation().getReservationId().equals(reservationId));
    }
}