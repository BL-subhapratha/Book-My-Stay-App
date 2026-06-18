package src.services;

import src.model.BookingResult;
import src.model.Reservation;
import src.model.RoomType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class BookingQueueService {

    // The central FIFO queue — every booking request enters here
    private final Queue<Reservation>  bookingQueue;

    // Downstream services
    private final InventoryService    inventoryService;
    private final SearchService       searchService;

    // Audit log of all processed results (confirmed + declined)
    private final List<BookingResult> processedResults;

    public BookingQueueService(InventoryService inventoryService,
                               SearchService searchService) {
        this.bookingQueue     = new ArrayDeque<>();
        this.inventoryService = inventoryService;
        this.searchService    = searchService;
        this.processedResults = new ArrayList<>();
    }

    // =========================================================================
    // ENQUEUE — Guest submits a booking request
    // =========================================================================

    /**
     * Accepts a booking request and places it at the TAIL of the queue.
     *
     * This is the only write operation guests perform. No inventory is
     * touched here — enqueuing is a pure ordering operation.
     *
     * @param reservation The guest's booking request
     * @throws IllegalArgumentException if reservation is null
     */
    public void enqueueRequest(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Cannot enqueue a null reservation.");
        }

        bookingQueue.offer(reservation);   // O(1) — adds to tail

        System.out.printf("[Queue  ▶ ENQUEUED ] %s | Position: #%d%n",
                reservation, bookingQueue.size());
    }

    // =========================================================================
    // PROCESS — Dequeue and attempt to confirm one reservation
    // =========================================================================

    /**
     * Dequeues the HEAD reservation (oldest request) and attempts to confirm it.
     *
     * Processing steps:
     *   1. poll() — remove from head; O(1)
     *   2. assertBookable() — read-only availability check
     *   3. holdRoom() — atomic inventory decrement (only on success)
     *   4. Return BookingResult (CONFIRMED or DECLINED)
     *
     * If no rooms are available the request is DECLINED but still removed from
     * the queue — it is not re-queued. In a real system declined guests would
     * be notified and could submit a new request.
     *
     * @return BookingResult, or null if the queue is empty
     */
    public BookingResult processNext() {
        Reservation next = bookingQueue.poll();   // O(1) — removes from head

        if (next == null) {
            System.out.println("[Queue  ◀ EMPTY    ] No pending reservations to process.");
            return null;
        }

        System.out.printf("%n[Queue  ◀ PROCESSING] %s%n", next);

        BookingResult result = attemptConfirm(next);
        processedResults.add(result);

        String icon = result.isConfirmed() ? "✓ CONFIRMED" : "✗ DECLINED ";
        System.out.printf("[Queue    %s] %s%n", icon, result);

        return result;
    }

    /**
     * Drains the entire queue, processing every pending request in FIFO order.
     * Useful for batch processing at end of peak window.
     *
     * @return Unmodifiable list of all results in processing order
     */
    public List<BookingResult> processAll() {
        System.out.printf("%n[Queue  ◀ BATCH    ] Processing all %d queued request(s)...%n",
                bookingQueue.size());

        List<BookingResult> batchResults = new ArrayList<>();
        while (!bookingQueue.isEmpty()) {
            BookingResult r = processNext();
            if (r != null) batchResults.add(r);
        }

        System.out.printf("[Queue    BATCH END] Processed %d request(s). Queue empty: %b%n",
                batchResults.size(), bookingQueue.isEmpty());

        return Collections.unmodifiableList(batchResults);
    }

    // =========================================================================
    // INSPECTION — Read-only queue state
    // =========================================================================

    /**
     * Peeks at the next request to be processed without removing it.
     * O(1) — does not alter queue state.
     */
    public Reservation peekNext() {
        return bookingQueue.peek();
    }

    /** Current number of pending requests. */
    public int queueDepth() {
        return bookingQueue.size();
    }

    /** True if there are no pending requests. */
    public boolean isEmpty() {
        return bookingQueue.isEmpty();
    }

    /** All results from previously processed requests (confirmed + declined). */
    public List<BookingResult> getProcessedResults() {
        return Collections.unmodifiableList(processedResults);
    }

    /** Prints a summary of the pending queue (without dequeuing). */
    public void printQueueStatus() {
        System.out.println("\n  ┌─── Pending Booking Queue ─────────────────────────────");
        if (bookingQueue.isEmpty()) {
            System.out.println("  │   (empty)");
        } else {
            int pos = 1;
            for (Reservation r : bookingQueue) {        // iterates without removing
                System.out.printf("  │  #%-2d %s%n", pos++, r);
            }
        }
        System.out.printf("  └─── Total pending: %d ────────────────────────────────%n",
                bookingQueue.size());
    }

    /** Prints a summary of all processed results so far. */
    public void printProcessingSummary() {
        long confirmed = processedResults.stream().filter(BookingResult::isConfirmed).count();
        long declined  = processedResults.size() - confirmed;

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              Booking Processing Summary                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  Total processed : %-39d ║%n", processedResults.size());
        System.out.printf( "║  Confirmed       : %-39d ║%n", confirmed);
        System.out.printf( "║  Declined        : %-39d ║%n", declined);
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        for (BookingResult r : processedResults) {
            String icon   = r.isConfirmed() ? "[OK]" : "[--]";
            String guest  = r.getReservation().getGuestName();
            String type   = r.getReservation().getRoomType().getDisplayName();
            String status = r.getStatus().toString();
            System.out.printf("║  %s %-18s %-12s %-15s ║%n",
                    icon, guest, type, status);
        }
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    // =========================================================================
    // Private — attempt to hold a room and confirm the reservation
    // =========================================================================

    private BookingResult attemptConfirm(Reservation r) {
        RoomType type = r.getRoomType();

        // Step 1: Read-only availability check (SearchService — no mutation)
        try {
            searchService.assertBookable(type);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return BookingResult.declined(r, e.getMessage());
        }

        // Step 2: Atomic inventory hold (InventoryService — single decrement)
        inventoryService.holdRoom(type);

        // Step 3: Calculate total cost
        double pricePerNight = inventoryService.getPrice(type);
        double totalCost     = r.estimatedTotal(pricePerNight);

        return BookingResult.confirmed(r, totalCost);
    }
}