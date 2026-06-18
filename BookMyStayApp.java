import src.model.Amenity;
import src.model.BookingResult;
import src.model.RoomCatalogue;
import src.model.RoomDetails;
import src.model.RoomInventory;
import src.model.RoomType;
import src.model.Reservation;
import src.model.SearchCriteria;
import src.services.BookingQueueService;
import src.services.InventoryService;
import src.services.SearchService;

import java.time.LocalDate;
import java.util.List;

/**
 * BookMyStayApp — Driver demonstrating UC1 + UC2 + UC3.
 *
 * UC3 scenarios:
 *   A. Normal FIFO processing — requests served in arrival order
 *   B. High-traffic burst — many requests enqueued before any is processed
 *   C. Sell-out scenario — late arrivals declined when inventory exhausted
 *   D. Mixed room types — each type's inventory tracked independently
 *   E. Peek / inspection — queue state visible without dequeuing
 */
public class BookMyStayApp {

    public static void main(String[] args) {

        // =====================================================================
        // SHARED INFRASTRUCTURE
        // =====================================================================
        RoomInventory      inventory  = new RoomInventory();
        RoomCatalogue      catalogue  = new RoomCatalogue();
        InventoryService   adminSvc   = new InventoryService(inventory);
        SearchService      searchSvc  = new SearchService(inventory, catalogue);
        BookingQueueService queueSvc  = new BookingQueueService(adminSvc, searchSvc);

        LocalDate today = LocalDate.now();

        // =====================================================================
        // UC1 — Seed inventory (limited counts to force sell-out scenarios)
        // =====================================================================
        section("UC1: Inventory Setup");
        adminSvc.setupRoomType(RoomType.SINGLE, 2,  89.99);
        adminSvc.setupRoomType(RoomType.DOUBLE, 3, 149.99);
        adminSvc.setupRoomType(RoomType.SUITE,  1, 299.99);
        adminSvc.showInventory();

        // =====================================================================
        // UC2 — Guest searches before booking
        // =====================================================================
        section("UC2: Guest Search");
        searchSvc.printResults(searchSvc.searchAvailable());

        // =====================================================================
        // UC3-A: Normal FIFO — requests processed one at a time
        // =====================================================================
        section("UC3-A: Normal FIFO Processing");

        System.out.println("Two guests submit requests sequentially:");
        queueSvc.enqueueRequest(new Reservation("Alice",   RoomType.SINGLE, today.plusDays(1), today.plusDays(3)));
        queueSvc.enqueueRequest(new Reservation("Bob",     RoomType.DOUBLE, today.plusDays(2), today.plusDays(5)));
        queueSvc.printQueueStatus();

        System.out.println("\nProcessing requests in FIFO order:");
        queueSvc.processNext();   // Alice first (she arrived first)
        queueSvc.processNext();   // Bob second
        queueSvc.processNext();   // Queue now empty

        // =====================================================================
        // UC3-B: High-traffic burst — many enqueued before any are processed
        // =====================================================================
        section("UC3-B: High-Traffic Burst — 6 Requests, 1 Suite Available");

        System.out.println("Peak demand: 4 guests try to book suites simultaneously.");
        System.out.println("All enqueued first (simulating concurrent arrivals):\n");
        queueSvc.enqueueRequest(new Reservation("Carol",   RoomType.SUITE,  today.plusDays(1), today.plusDays(4)));
        queueSvc.enqueueRequest(new Reservation("Dave",    RoomType.SUITE,  today.plusDays(1), today.plusDays(3)));
        queueSvc.enqueueRequest(new Reservation("Eve",     RoomType.SUITE,  today.plusDays(2), today.plusDays(6)));
        queueSvc.enqueueRequest(new Reservation("Frank",   RoomType.SUITE,  today.plusDays(3), today.plusDays(5)));
        queueSvc.enqueueRequest(new Reservation("Grace",   RoomType.SINGLE, today.plusDays(1), today.plusDays(2)));
        queueSvc.enqueueRequest(new Reservation("Henry",   RoomType.DOUBLE, today.plusDays(4), today.plusDays(7)));

        System.out.println("\nPeek at next to be served (without removing):");
        Reservation next = queueSvc.peekNext();
        System.out.println("  → " + next);

        System.out.println("\nProcessing all 6 in FIFO order:");
        queueSvc.processAll();

        // =====================================================================
        // UC3-C: Sell-out confirmation — inventory reflects all holds
        // =====================================================================
        section("UC3-C: Post-Burst Inventory Check");
        adminSvc.showInventory();

        System.out.println("\nGuest attempts to search for available Suites after burst:");
        SearchCriteria suiteOnly = new SearchCriteria.Builder()
                .roomType(RoomType.SUITE)
                .onlyAvailable(true)
                .build();
        List<RoomDetails> suiteResults = searchSvc.search(suiteOnly);
        if (suiteResults.isEmpty()) {
            System.out.println("  → No suites available. (All sold out by earlier guests.)");
        }

        // =====================================================================
        // UC3-D: Late arrival on sold-out room — DECLINED gracefully
        // =====================================================================
        section("UC3-D: Late Arrival — Sold-Out Room");

        System.out.println("A late guest submits a Suite request (1 in queue, 0 available):");
        queueSvc.enqueueRequest(new Reservation("Iris", RoomType.SUITE, today.plusDays(2), today.plusDays(4)));
        queueSvc.processNext();

        // =====================================================================
        // FULL PROCESSING SUMMARY
        // =====================================================================
        section("Full Booking Processing Summary");
        queueSvc.printProcessingSummary();

        System.out.println("\n=== Use Case 3 Complete ===");
    }

    private static void section(String title) {
        System.out.println("\n====================================================");
        System.out.printf( "  %s%n", title);
        System.out.println("====================================================");
    }
}