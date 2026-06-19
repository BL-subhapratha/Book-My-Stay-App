package src.services;

import src.model.BookingResult;
import src.model.Reservation;
import src.model.RoomType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * AllocationService — UC4: Reservation Confirmation & Room Allocation
 *
 * Extends BookingQueueService to assign a unique physical room ID to every
 * confirmed booking. The HashSet acts as an O(1) duplicate-prevention guard,
 * making double-booking structurally impossible.
 *
 * Data Structures:
 *   Set<String>                (HashSet) — globally confirmed room IDs
 *   Map<RoomType, Set<String>>           — per-type pool of available room IDs
 *   Map<String, String>                  — reservationId → assigned roomId
 *
 * Key Concepts:
 *   • HashSet.add() returns false on duplicate  → O(1) conflict detection
 *   • Map<RoomType, Set<String>>                → IDs grouped by room type
 *   • Atomic allocation                         → pool removal + HashSet add
 *                                                  + inventory hold together
 *
 * Flow:
 *   Dequeue request (FIFO — inherited from UC3)
 *     → assertBookable() via SearchService
 *     → Pull room ID from availableRoomPool for this type
 *     → bookedRoomIds.add(roomId) — false means duplicate, abort
 *     → holdRoom() via InventoryService
 *     → Record reservationId → roomId in allocationLog
 */
public class AllocationService extends BookingQueueService {

    // Global set of every confirmed room ID.
    // HashSet.add() returns false if already present — catches any duplicate instantly.
    private final Set<String> bookedRoomIds;

    // Per-type pool of physical room IDs still available for assignment.
    // IDs move from here into bookedRoomIds at confirmation time.
    private final Map<RoomType, Set<String>> availableRoomPool;

    // Audit trail: reservationId → roomId (used by UC5/UC6 downstream)
    private final Map<String, String> allocationLog;

    public AllocationService(InventoryService inventoryService,
                             SearchService    searchService) {
        super(inventoryService, searchService);
        this.bookedRoomIds     = new HashSet<>();
        this.availableRoomPool = new HashMap<>();
        this.allocationLog     = new HashMap<>();
        seedRoomPool();
    }

    // -------------------------------------------------------------------------
    // Pool Seeding
    // -------------------------------------------------------------------------

    /**
     * Pre-populates each room type's physical ID pool.
     *
     * Convention:
     *   SINGLE → S101–S109
     *   DOUBLE → D201–D209
     *   SUITE  → T301–T309
     *
     * Pool is larger than initial inventory count to allow future expansion.
     *
     * Teaches: building a Map<RoomType, Set<String>> — grouping IDs under a key.
     */
    private void seedRoomPool() {
        Set<String> singles = new HashSet<>();
        for (int i = 1; i <= 9; i++) singles.add("S10" + i);
        availableRoomPool.put(RoomType.SINGLE, singles);

        Set<String> doubles = new HashSet<>();
        for (int i = 1; i <= 9; i++) doubles.add("D20" + i);
        availableRoomPool.put(RoomType.DOUBLE, doubles);

        Set<String> suites = new HashSet<>();
        for (int i = 1; i <= 9; i++) suites.add("T30" + i);
        availableRoomPool.put(RoomType.SUITE, suites);
    }

    // -------------------------------------------------------------------------
    // Core Override
    // -------------------------------------------------------------------------

    /**
     * Overrides processNext() to inject room ID allocation between the
     * availability check and the base class confirmation step.
     *
     * Steps:
     *   1. peekNext()            — inspect head without removing (non-destructive)
     *   2. assertBookable()      — availability guard via SearchService
     *   3. Pull roomId from pool — remove tentatively from availableRoomPool
     *   4. bookedRoomIds.add()   — false = duplicate → restore pool and decline
     *   5. super.processNext()   — base class dequeues + holds inventory + logs result
     *   6. If base declined      — restore roomId back to pool and bookedRoomIds
     *   7. If base confirmed     — record reservationId → roomId in allocationLog
     */
    @Override
    public BookingResult processNext() {
        Reservation reservation = peekNext();

        if (reservation == null) {
            System.out.println("[Queue  ◀ EMPTY    ] No pending reservations to process.");
            return null;
        }

        RoomType type = reservation.getRoomType();

        // Step 2 — Availability check using the protected accessor
        try {
            getSearchService().assertBookable(type);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Not available — let base class dequeue and decline normally
            return super.processNext();
        }

        // Step 3 — Pull a room ID from the available pool for this type
        Set<String> pool = availableRoomPool.get(type);
        if (pool == null || pool.isEmpty()) {
            System.out.printf("[Allocation] Room ID pool exhausted for %s%n",
                    type.getDisplayName());
            return super.processNext();
        }

        String roomId = pool.iterator().next();
        pool.remove(roomId);    // tentatively remove from pool

        // Step 4 — HashSet.add() returns false if already booked (duplicate guard)
        boolean isUnique = bookedRoomIds.add(roomId);
        if (!isUnique) {
            pool.add(roomId);   // restore — do not lose this ID
            System.out.printf("[ALLOCATION ERROR] Duplicate ID detected: %s — declining.%n",
                    roomId);
            return super.processNext();
        }

        // Step 5 — Base class dequeues + holds inventory + appends to processedResults
        BookingResult result = super.processNext();

        // Steps 6 & 7 — Reconcile allocation with base class outcome
        if (result != null && result.isConfirmed()) {
            allocationLog.put(reservation.getReservationId(), roomId);
            System.out.printf("[Allocation] Room %-5s assigned to %-10s (Reservation: %s)%n",
                    roomId, reservation.getGuestName(), reservation.getReservationId());
        } else {
            // Base declined for an unexpected reason — return ID to pool
            pool.add(roomId);
            bookedRoomIds.remove(roomId);
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns the physical room ID assigned to a confirmed reservation.
     * Returns null if the reservation was declined or not found.
     */
    public String getRoomId(String reservationId) {
        return allocationLog.get(reservationId);
    }

    /**
     * Returns true if the given room ID is already booked.
     * O(1) — HashSet.contains() lookup.
     */
    public boolean isRoomBooked(String roomId) {
        return bookedRoomIds.contains(roomId);
    }

    /**
     * Returns the full allocationLog for use by downstream services (UC5, UC6).
     */
    public Map<String, String> getAllocationLog() {
        return allocationLog;
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    /**
     * Prints the allocation log and the raw HashSet contents.
     * The HashSet print demonstrates that every ID in it is unique by definition.
     */
    public void printAllocationLog() {
        System.out.println("\n  ┌─────────────────────────────────────────────────────────┐");
        System.out.println("  │              UC4 — Room Allocation Log                  │");
        System.out.printf( "  │  Total rooms in bookedRoomIds (HashSet): %-4d           │%n",
                bookedRoomIds.size());
        System.out.println("  └─────────────────────────────────────────────────────────┘");

        if (allocationLog.isEmpty()) {
            System.out.println("    (No rooms allocated yet.)");
            return;
        }

        System.out.printf("    %-14s  →  %s%n", "Reservation ID", "Room ID");
        System.out.println("    " + "─".repeat(32));
        allocationLog.forEach((resId, roomId) ->
                System.out.printf("    %-14s  →  %s%n", resId, roomId));

        System.out.println("\n    bookedRoomIds (HashSet) = " + bookedRoomIds);
    }

    /**
     * Prints remaining unassigned room IDs per type.
     */
    public void printAvailablePool() {
        System.out.println("\n  [UC4] Remaining available room ID pool:");
        for (RoomType type : RoomType.values()) {
            Set<String> pool = availableRoomPool.getOrDefault(type, new HashSet<>());
            System.out.printf("    %-12s → %s%n", type.getDisplayName(), pool);
        }
    }
}