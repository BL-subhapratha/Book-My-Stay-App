package src.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class RoomDetails {

    private final RoomType      type;
    private final double        pricePerNight;
    private final int           availableCount;
    private final Set<Amenity>  amenities;
    private final String        description;

    public RoomDetails(RoomType type,
                       double pricePerNight,
                       int availableCount,
                       Set<Amenity> amenities,
                       String description) {
        this.type           = type;
        this.pricePerNight  = pricePerNight;
        this.availableCount = availableCount;
        // Defensive copy — caller's set mutations won't affect this object
        this.amenities      = Collections.unmodifiableSet(EnumSet.copyOf(amenities));
        this.description    = description;
    }

    // -------------------------------------------------------------------------
    // Read-only accessors — no setters anywhere
    // -------------------------------------------------------------------------

    public RoomType     getType()           { return type; }
    public double       getPricePerNight()  { return pricePerNight; }
    public int          getAvailableCount() { return availableCount; }
    public Set<Amenity> getAmenities()      { return amenities; }
    public String       getDescription()   { return description; }

    /** Convenience: true if at least one room is available. */
    public boolean isAvailable() {
        return availableCount > 0;
    }

    /** Convenience: true if this room type offers a specific amenity. */
    public boolean hasAmenity(Amenity amenity) {
        return amenities.contains(amenity);
    }

    @Override
    public String toString() {
        return String.format("RoomDetails{type=%s, price=£%.2f, available=%d}",
                type.getDisplayName(), pricePerNight, availableCount);
    }
}