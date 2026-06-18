package src.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RoomInventory {

    // Maps room type → number of available rooms
    private final Map<RoomType, Integer> availableRooms;

    // Maps room type → price per night (in GBP)
    private final Map<RoomType, Double> roomPrices;

    /**
     * Constructs an empty inventory.
     * Room types must be explicitly added via addRoomType().
     */
    public RoomInventory() {
        this.availableRooms = new HashMap<>();
        this.roomPrices     = new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Inventory Setup
    // -------------------------------------------------------------------------

    /**
     * Adds a new room type with its count and price, or updates an existing one.
     *
     * @param type   The room type (SINGLE, DOUBLE, SUITE)
     * @param count  Number of rooms of this type available (must be >= 0)
     * @param price  Price per night in GBP (must be > 0)
     * @throws IllegalArgumentException if count < 0 or price <= 0
     */
    public void addRoomType(RoomType type, int count, double price) {
        validateCount(count);
        validatePrice(price);

        availableRooms.put(type, count);
        roomPrices.put(type, price);

        System.out.printf("[Inventory] Added/Updated: %-12s | Rooms: %3d | Price: £%.2f/night%n",
                type.getDisplayName(), count, price);
    }

    // -------------------------------------------------------------------------
    // Dynamic Updates
    // -------------------------------------------------------------------------

    /**
     * Updates the available count for an existing room type.
     * Use this for restocking or corrections.
     *
     * @param type     Room type to update
     * @param newCount New count (>= 0)
     * @throws IllegalArgumentException if room type not found or count invalid
     */
    public void updateRoomCount(RoomType type, int newCount) {
        ensureRoomTypeExists(type);
        validateCount(newCount);

        int oldCount = availableRooms.get(type);
        availableRooms.put(type, newCount);

        System.out.printf("[Inventory] Updated count for %-12s: %d → %d%n",
                type.getDisplayName(), oldCount, newCount);
    }

    /**
     * Updates the price per night for an existing room type.
     *
     * @param type     Room type to reprice
     * @param newPrice New price per night (> 0)
     * @throws IllegalArgumentException if room type not found or price invalid
     */
    public void updateRoomPrice(RoomType type, double newPrice) {
        ensureRoomTypeExists(type);
        validatePrice(newPrice);

        double oldPrice = roomPrices.get(type);
        roomPrices.put(type, newPrice);

        System.out.printf("[Inventory] Updated price for %-12s: £%.2f → £%.2f%n",
                type.getDisplayName(), oldPrice, newPrice);
    }

    /**
     * Decrements the available count by 1 when a room is reserved.
     * Called by the booking service to hold inventory atomically.
     *
     * @param type Room type being reserved
     * @throws IllegalStateException    if no rooms of this type are available
     * @throws IllegalArgumentException if room type not registered
     */
    public void decrementRoom(RoomType type) {
        ensureRoomTypeExists(type);

        int current = availableRooms.get(type);
        if (current <= 0) {
            throw new IllegalStateException(
                    "No available rooms for type: " + type.getDisplayName());
        }
        availableRooms.put(type, current - 1);
    }

    /**
     * Increments the available count by 1 when a booking is cancelled.
     *
     * @param type Room type being released
     * @throws IllegalArgumentException if room type not registered
     */
    public void incrementRoom(RoomType type) {
        ensureRoomTypeExists(type);
        availableRooms.put(type, availableRooms.get(type) + 1);
    }

    // -------------------------------------------------------------------------
    // Availability Queries — O(1) HashMap lookups
    // -------------------------------------------------------------------------

    /**
     * Returns the number of rooms currently available for a given type.
     *
     * @param type The room type to query
     * @return available count, or 0 if type not registered
     */
    public int getAvailableCount(RoomType type) {
        return availableRooms.getOrDefault(type, 0);
    }

    /**
     * Returns the price per night for a given room type.
     *
     * @param type The room type to query
     * @return price per night, or -1.0 if type not registered
     */
    public double getPrice(RoomType type) {
        return roomPrices.getOrDefault(type, -1.0);
    }

    /**
     * Checks if at least one room of the given type is available.
     *
     * @param type Room type to check
     * @return true if available count > 0
     */
    public boolean isAvailable(RoomType type) {
        return getAvailableCount(type) > 0;
    }

    /**
     * Returns an unmodifiable snapshot of all availability data.
     * Safe to share externally without risking mutation.
     */
    public Map<RoomType, Integer> getAllAvailability() {
        return Collections.unmodifiableMap(availableRooms);
    }

    /**
     * Returns an unmodifiable snapshot of all pricing data.
     */
    public Map<RoomType, Double> getAllPrices() {
        return Collections.unmodifiableMap(roomPrices);
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    /**
     * Prints a formatted inventory status table to stdout.
     */
    public void printInventoryStatus() {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║           BookMyStay — Room Inventory Status     ║");
        System.out.println("╠══════════════╦═════════════╦════════════════════╣");
        System.out.printf( "║ %-12s ║ %-11s ║ %-18s ║%n", "Room Type", "Available", "Price/Night");
        System.out.println("╠══════════════╬═════════════╬════════════════════╣");

        for (RoomType type : RoomType.values()) {
            if (availableRooms.containsKey(type)) {
                int    count  = availableRooms.get(type);
                double price  = roomPrices.get(type);
                String status = count > 0 ? String.valueOf(count) : "SOLD OUT";

                System.out.printf("║ %-12s ║ %-11s ║ £%-17.2f ║%n",
                        type.getDisplayName(), status, price);
            }
        }

        System.out.println("╚══════════════╩═════════════╩════════════════════╝");
    }

    // -------------------------------------------------------------------------
    // Validation Helpers
    // -------------------------------------------------------------------------

    private void validateCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Room count cannot be negative: " + count);
        }
    }

    private void validatePrice(double price) {
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero: " + price);
        }
    }

    private void ensureRoomTypeExists(RoomType type) {
        if (!availableRooms.containsKey(type)) {
            throw new IllegalArgumentException(
                    "Room type not registered in inventory: " + type.getDisplayName());
        }
    }
}