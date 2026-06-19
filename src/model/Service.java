package src.model;

public final class Service {

    /**
     * Catalogue of available add-on services with fixed prices.
     * Enum used so the set of valid services is closed and type-safe.
     */
    public enum Type {
        BREAKFAST       ("Breakfast Package",     25.00),
        SPA             ("Spa Package",           120.00),
        AIRPORT_PICKUP  ("Airport Pickup",         45.00),
        LATE_CHECKOUT   ("Late Checkout",          30.00),
        ROOM_SERVICE    ("Room Service (24hr)",    15.00),
        LAUNDRY         ("Laundry Service",        20.00);

        private final String displayName;
        private final double pricePerUnit;

        Type(String displayName, double pricePerUnit) {
            this.displayName   = displayName;
            this.pricePerUnit  = pricePerUnit;
        }

        public String getDisplayName() { return displayName; }
        public double getPricePerUnit() { return pricePerUnit; }

        @Override
        public String toString() { return displayName; }
    }

    private final String reservationId;
    private final Type   type;
    private final int    quantity;

    /**
     * @param reservationId  The confirmed reservation this service is attached to
     * @param type           The type of add-on service selected
     * @param quantity       Number of units (e.g. 2 breakfasts for 2 guests)
     */
    public Service(String reservationId, Type type, int quantity) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("Reservation ID cannot be null or blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Service type cannot be null.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        this.reservationId = reservationId;
        this.type          = type;
        this.quantity      = quantity;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getReservationId() { return reservationId; }
    public Type   getType()          { return type; }
    public int    getQuantity()      { return quantity; }

    /**
     * Total cost for this service line: pricePerUnit × quantity.
     */
    public double getTotalPrice() {
        return type.getPricePerUnit() * quantity;
    }

    @Override
    public String toString() {
        return String.format("%-25s x%-2d  @ £%6.2f each  =  £%.2f",
                type.getDisplayName(), quantity,
                type.getPricePerUnit(), getTotalPrice());
    }
}