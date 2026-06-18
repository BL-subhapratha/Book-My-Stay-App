package src.services;

import src.model.Amenity;
import src.model.RoomCatalogue;
import src.model.RoomDetails;
import src.model.RoomInventory;
import src.model.RoomType;
import src.model.SearchCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchService {

    private final RoomInventory  inventory;   // live counts + prices
    private final RoomCatalogue  catalogue;   // static amenities + descriptions

    public SearchService(RoomInventory inventory, RoomCatalogue catalogue) {
        this.inventory = inventory;
        this.catalogue = catalogue;
    }

    // -------------------------------------------------------------------------
    // Primary Search API
    // -------------------------------------------------------------------------

    /**
     * Returns all room types matching the given criteria.
     * Results are sorted by price (ascending) by default.
     *
     * @param criteria Filters to apply; use SearchCriteria.Builder to construct
     * @return Unmodifiable list of matching RoomDetails snapshots
     */
    public List<RoomDetails> search(SearchCriteria criteria) {
        System.out.println("\n[Search] Applying criteria: " + criteria);

        List<RoomDetails> results = new ArrayList<>();

        for (RoomType type : RoomType.values()) {

            // --- Step 1: Snapshot live data (read-only HashMap lookups) ---
            int    count = inventory.getAvailableCount(type);
            double price = inventory.getPrice(type);

            // Skip room types not yet registered in inventory
            if (price < 0) continue;

            // --- Step 2: Build immutable snapshot ---
            RoomDetails details = new RoomDetails(
                    type,
                    price,
                    count,
                    catalogue.getAmenities(type),
                    catalogue.getDescription(type)
            );

            // --- Step 3: Apply filters ---
            if (!matchesCriteria(details, criteria)) continue;

            results.add(details);
        }

        // Sort by ascending price
        results.sort(Comparator.comparingDouble(RoomDetails::getPricePerNight));

        System.out.printf("[Search] Found %d matching room type(s).%n", results.size());
        return Collections.unmodifiableList(results);
    }

    /**
     * Convenience: search with no filters — returns all room types.
     */
    public List<RoomDetails> searchAll() {
        return search(new SearchCriteria.Builder().onlyAvailable(false).build());
    }

    /**
     * Convenience: returns only room types with at least one available room.
     */
    public List<RoomDetails> searchAvailable() {
        return search(new SearchCriteria.Builder().onlyAvailable(true).build());
    }

    /**
     * Looks up a single room type by its exact type.
     * Used by the booking flow to validate a room before reserving.
     *
     * @param type The room type to look up
     * @return RoomDetails snapshot, or null if not in inventory
     */
    public RoomDetails findByType(RoomType type) {
        double price = inventory.getPrice(type);
        if (price < 0) return null; // not registered

        return new RoomDetails(
                type,
                price,
                inventory.getAvailableCount(type),
                catalogue.getAmenities(type),
                catalogue.getDescription(type)
        );
    }

    // -------------------------------------------------------------------------
    // Availability Guard — used by booking layer
    // -------------------------------------------------------------------------

    /**
     * Defensive check called before any reservation attempt.
     * Centralises the "can this room be booked?" logic in one place.
     *
     * @param type Room type the guest wants to book
     * @throws IllegalArgumentException if room type is not in inventory
     * @throws IllegalStateException    if the room type is sold out
     */
    public void assertBookable(RoomType type) {
        RoomDetails details = findByType(type);

        if (details == null) {
            throw new IllegalArgumentException(
                    "Unknown room type: " + type.getDisplayName());
        }
        if (!details.isAvailable()) {
            throw new IllegalStateException(
                    type.getDisplayName() + " is currently sold out. "
                    + "Please choose another room type.");
        }
    }

    // -------------------------------------------------------------------------
    // Display Helpers
    // -------------------------------------------------------------------------

    /**
     * Prints a formatted search results table to stdout.
     *
     * @param results The list returned by search()
     */
    public void printResults(List<RoomDetails> results) {
        if (results.isEmpty()) {
            System.out.println("\n  No rooms match your search criteria.");
            return;
        }

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    BookMyStay — Search Results                           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════╝");

        for (RoomDetails r : results) {
            String availStr = r.isAvailable()
                    ? r.getAvailableCount() + " available"
                    : "SOLD OUT";

            System.out.println();
            System.out.printf("  ┌─ %s ─────────────────────────────────────────%n",
                    r.getType().getDisplayName());
            System.out.printf("  │  Price       : £%.2f / night%n", r.getPricePerNight());
            System.out.printf("  │  Availability: %s%n", availStr);
            System.out.printf("  │  Description : %s%n", r.getDescription());
            System.out.printf("  │  Amenities   : %s%n", formatAmenities(r));
            System.out.println("  └──────────────────────────────────────────────────────────────");
        }
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    /**
     * Applies each active filter from SearchCriteria to a RoomDetails object.
     * All filters must pass (AND logic).
     */
    private boolean matchesCriteria(RoomDetails details, SearchCriteria criteria) {

        // Filter 1: availability gate
        if (criteria.isOnlyAvailable() && !details.isAvailable()) {
            return false;
        }

        // Filter 2: exact room type match
        if (criteria.hasRoomTypeFilter()
                && details.getType() != criteria.getRoomType()) {
            return false;
        }

        // Filter 3: maximum price per night
        if (criteria.hasPriceFilter()
                && details.getPricePerNight() > criteria.getMaxPricePerNight()) {
            return false;
        }

        // Filter 4: required amenities — ALL must be present (AND)
        for (Amenity required : criteria.getRequiredAmenities()) {
            if (!details.hasAmenity(required)) {
                return false;
            }
        }

        return true;
    }

    /** Formats amenity set as a comma-separated string for display. */
    private String formatAmenities(RoomDetails r) {
        if (r.getAmenities().isEmpty()) return "None listed";
        StringBuilder sb = new StringBuilder();
        for (Amenity a : r.getAmenities()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(a.getDisplayName());
        }
        return sb.toString();
    }
}