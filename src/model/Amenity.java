package src.model;

public enum Amenity {
    WIFI("Free WiFi"),
    TV("Flat-Screen TV"),
    AIR_CONDITIONING("Air Conditioning"),
    MINI_BAR("Mini Bar"),
    SAFE("In-Room Safe"),
    BATHTUB("Soaking Bathtub"),
    KING_BED("King-Size Bed"),
    TWIN_BEDS("Twin Beds"),
    SEA_VIEW("Sea View"),
    LOUNGE("Private Lounge"),
    BREAKFAST("Complimentary Breakfast"),
    PARKING("Free Parking");

    private final String displayName;

    Amenity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}