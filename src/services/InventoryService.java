package src.services;

import src.model.RoomInventory;
import src.model.RoomType;

public class InventoryService {

    private final RoomInventory inventory;

    public InventoryService(RoomInventory inventory) {
        this.inventory = inventory;
    }

    // -------------------------------------------------------------------------
    // Admin Operations
    // -------------------------------------------------------------------------

    /**
     * Initialises a room type with count and price.
     * Entry point for Hotel Admin use case.
     */
    public void setupRoomType(RoomType type, int count, double pricePerNight) {
        inventory.addRoomType(type, count, pricePerNight);
    }

    /**
     * Restocks a room type (e.g., after renovation or new wing opening).
     * Adds delta rooms on top of the current count.
     *
     * @param type  Room type to restock
     * @param delta Number of additional rooms to add (must be > 0)
     */
    public void restockRooms(RoomType type, int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("Restock delta must be positive: " + delta);
        }
        int current = inventory.getAvailableCount(type);
        inventory.updateRoomCount(type, current + delta);
        System.out.printf("[Admin] Restocked %d %s room(s). New total: %d%n",
                delta, type.getDisplayName(), current + delta);
    }

    /**
     * Updates the nightly price for a room type.
     * Typically used during seasonal pricing adjustments.
     */
    public void repriceRoom(RoomType type, double newPrice) {
        inventory.updateRoomPrice(type, newPrice);
    }

    // -------------------------------------------------------------------------
    // Guest / Booking Availability
    // -------------------------------------------------------------------------

    /**
     * Checks if a room type is available for booking.
     */
    public boolean checkAvailability(RoomType type) {
        boolean available = inventory.isAvailable(type);
        System.out.printf("[Availability] %-12s → %s%n",
                type.getDisplayName(), available ? "AVAILABLE" : "UNAVAILABLE");
        return available;
    }

    /**
     * Returns the price per night for a given room type.
     * Returns -1 if the room type is not registered.
     */
    public double getPrice(RoomType type) {
        return inventory.getPrice(type);
    }

    /**
     * Displays the full inventory status table.
     */
    public void showInventory() {
        inventory.printInventoryStatus();
    }

    /**
     * Package-private: used by BookingService to atomically hold a room.
     */
    public void holdRoom(RoomType type) {
        inventory.decrementRoom(type);
    }

    /**
     * Package-private: used by BookingService to release a room on cancellation.
     */
    public void releaseRoom(RoomType type) {
        inventory.incrementRoom(type);
    }
}