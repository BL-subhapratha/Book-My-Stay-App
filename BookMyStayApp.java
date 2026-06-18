import src.model.Amenity;
import src.model.RoomCatalogue;
import src.model.RoomDetails;
import src.model.RoomInventory;
import src.model.RoomType;
import src.model.SearchCriteria;
import src.services.InventoryService;
import src.services.SearchService;

import java.util.List;

public class BookMyStayApp {

    public static void main(String[] args) {

        // =====================================================================
        // SETUP — shared state (single source of truth)
        // =====================================================================
        RoomInventory    inventory  = new RoomInventory();
        RoomCatalogue    catalogue  = new RoomCatalogue();
        InventoryService adminSvc   = new InventoryService(inventory);
        SearchService    searchSvc  = new SearchService(inventory, catalogue);

        // =====================================================================
        // USE CASE 1 — Admin seeds inventory
        // =====================================================================
        System.out.println("====================================================");
        System.out.println("  UC1: Room Inventory Setup");
        System.out.println("====================================================");

        adminSvc.setupRoomType(RoomType.SINGLE, 10, 89.99);
        adminSvc.setupRoomType(RoomType.DOUBLE, 8,  149.99);
        adminSvc.setupRoomType(RoomType.SUITE,  3,  299.99);
        adminSvc.showInventory();

        // =====================================================================
        // USE CASE 2 — Guest searches
        // =====================================================================
        System.out.println("\n====================================================");
        System.out.println("  UC2: Room Search & Availability Check");
        System.out.println("====================================================");

        // --- Search 1: Browse all rooms (no filters) ---
        System.out.println("\n[Scenario A] Guest browses the full catalogue");
        List<RoomDetails> allRooms = searchSvc.searchAll();
        searchSvc.printResults(allRooms);

        // --- Search 2: Only available rooms ---
        System.out.println("\n[Scenario B] Guest filters to available rooms only");
        List<RoomDetails> available = searchSvc.searchAvailable();
        searchSvc.printResults(available);

        // --- Search 3: Budget filter (max £150/night) ---
        System.out.println("\n[Scenario C] Guest sets a budget of max 150/night");
        SearchCriteria budgetSearch = new SearchCriteria.Builder()
                .maxPricePerNight(150.0)
                .onlyAvailable(true)
                .build();
        searchSvc.printResults(searchSvc.search(budgetSearch));

        // --- Search 4: Amenity filter (must have Breakfast + King Bed) ---
        System.out.println("\n[Scenario D] Guest requires Breakfast and King-Size Bed");
        SearchCriteria amenitySearch = new SearchCriteria.Builder()
                .requiredAmenity(Amenity.BREAKFAST)
                .requiredAmenity(Amenity.KING_BED)
                .onlyAvailable(true)
                .build();
        searchSvc.printResults(searchSvc.search(amenitySearch));

        // --- Search 5: Exact type lookup ---
        System.out.println("\n[Scenario E] Guest looks up the Suite specifically");
        SearchCriteria suiteSearch = new SearchCriteria.Builder()
                .roomType(RoomType.SUITE)
                .onlyAvailable(false)
                .build();
        searchSvc.printResults(searchSvc.search(suiteSearch));

        // =====================================================================
        // Live inventory change mid-session — search reflects it immediately
        // =====================================================================
        System.out.println("\n====================================================");
        System.out.println("  Admin Update: Singles sold out, Suite repriced");
        System.out.println("====================================================");
        inventory.updateRoomCount(RoomType.SINGLE, 0);
        inventory.updateRoomPrice(RoomType.SUITE, 379.99);

        System.out.println("\n[Scenario F] Same budget search after admin update");
        searchSvc.printResults(searchSvc.search(budgetSearch));

        // =====================================================================
        // Availability guard — prevents booking unavailable rooms
        // =====================================================================
        System.out.println("\n====================================================");
        System.out.println("  UC2 Guard: Booking attempt on sold-out room");
        System.out.println("====================================================");

        System.out.println("\nGuest attempts to book a Single Room (now sold out)...");
        try {
            searchSvc.assertBookable(RoomType.SINGLE);
        } catch (IllegalStateException e) {
            System.out.println("[Booking Blocked] " + e.getMessage());
        }

        System.out.println("\nGuest attempts to book a Double Room (still available)...");
        try {
            searchSvc.assertBookable(RoomType.DOUBLE);
            System.out.println("[Booking Allowed] Double Room passed availability check.");
        } catch (IllegalStateException e) {
            System.out.println("[Booking Blocked] " + e.getMessage());
        }
    }
}