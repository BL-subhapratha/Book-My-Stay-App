import src.model.RoomInventory;
import src.model.RoomType;
import src.services.InventoryService;

public class BookMyStayApp {

    public static void main(String[] args) {

        // --- Step 1: Initialise the inventory data structure ---
        RoomInventory inventory = new RoomInventory();
        InventoryService service = new InventoryService(inventory);

        System.out.println("=== BookMyStay: Use Case 1 — Room Inventory Setup ===\n");

        // --- Step 2: Hotel Admin seeds room types ---
        // HashMap.put() → O(1) insertion
        service.setupRoomType(RoomType.SINGLE, 10, 89.99);
        service.setupRoomType(RoomType.DOUBLE, 8,  149.99);
        service.setupRoomType(RoomType.SUITE,  3,  299.99);

        // --- Step 3: View initial inventory ---
        service.showInventory();

        // --- Step 4: Guest checks availability ---
        System.out.println("\n=== Guest Availability Checks ===");
        service.checkAvailability(RoomType.SINGLE);
        service.checkAvailability(RoomType.SUITE);

        // --- Step 5: Admin dynamic updates ---
        System.out.println("\n=== Admin: Dynamic Updates ===");

        // Reprice suites for peak season
        service.repriceRoom(RoomType.SUITE, 379.99);

        // Restock singles after room renovation completes
        service.restockRooms(RoomType.SINGLE, 5);

        // --- Step 6: Simulate inventory hold (room reserved by booking service) ---
        System.out.println("\n=== Booking Service: Inventory Hold Simulation ===");
        System.out.println("Holding 2 Double rooms for pending reservations...");
        service.holdRoom(RoomType.DOUBLE);
        service.holdRoom(RoomType.DOUBLE);

        // --- Step 7: Final inventory state ---
        service.showInventory();

        // --- Step 8: Edge case — sell-out scenario ---
        System.out.println("\n=== Edge Case: Sell-Out Scenario ===");
        // Hold all 3 suites
        service.holdRoom(RoomType.SUITE);
        service.holdRoom(RoomType.SUITE);
        service.holdRoom(RoomType.SUITE);

        // Now check suite availability
        service.checkAvailability(RoomType.SUITE);

        // Attempt to hold one more — should throw
        System.out.println("\nAttempting to hold a 4th Suite (should fail)...");
        try {
            service.holdRoom(RoomType.SUITE);
        } catch (IllegalStateException e) {
            System.out.println("[Error Caught] " + e.getMessage());
            System.out.println("→ Overbooking prevented by inventory guard.");
        }

        // --- Step 9: Cancellation releases the room ---
        System.out.println("\n=== Cancellation: Room Released ===");
        service.releaseRoom(RoomType.SUITE);
        service.checkAvailability(RoomType.SUITE);

    }
}