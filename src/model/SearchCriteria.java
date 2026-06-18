package src.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class SearchCriteria {

    private final RoomType  roomType;           // null = any type
    private final double    maxPricePerNight;   // 0 = no upper limit
    private final Set<Amenity> requiredAmenities; // empty = no amenity filter
    private final boolean   onlyAvailable;      // true = hide sold-out rooms

    private SearchCriteria(Builder builder) {
        this.roomType           = builder.roomType;
        this.maxPricePerNight   = builder.maxPricePerNight;
        this.requiredAmenities  = Collections.unmodifiableSet(
                builder.requiredAmenities.isEmpty()
                        ? EnumSet.noneOf(Amenity.class)
                        : EnumSet.copyOf(builder.requiredAmenities));
        this.onlyAvailable      = builder.onlyAvailable;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public RoomType      getRoomType()           { return roomType; }
    public double        getMaxPricePerNight()   { return maxPricePerNight; }
    public Set<Amenity>  getRequiredAmenities()  { return requiredAmenities; }
    public boolean       isOnlyAvailable()       { return onlyAvailable; }

    public boolean hasRoomTypeFilter()    { return roomType != null; }
    public boolean hasPriceFilter()       { return maxPricePerNight > 0; }
    public boolean hasAmenityFilter()     { return !requiredAmenities.isEmpty(); }

    @Override
    public String toString() {
        return String.format(
                "SearchCriteria{type=%s, maxPrice=£%.2f, amenities=%s, onlyAvailable=%b}",
                roomType != null ? roomType.getDisplayName() : "Any",
                maxPricePerNight,
                requiredAmenities,
                onlyAvailable);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static final class Builder {

        private RoomType       roomType           = null;
        private double         maxPricePerNight   = 0;
        private Set<Amenity>   requiredAmenities  = EnumSet.noneOf(Amenity.class);
        private boolean        onlyAvailable      = true; // sensible default

        public Builder roomType(RoomType type) {
            this.roomType = type;
            return this;
        }

        public Builder maxPricePerNight(double max) {
            if (max < 0) throw new IllegalArgumentException("Max price cannot be negative.");
            this.maxPricePerNight = max;
            return this;
        }

        public Builder requiredAmenity(Amenity amenity) {
            this.requiredAmenities.add(amenity);
            return this;
        }

        public Builder requiredAmenities(Set<Amenity> amenities) {
            this.requiredAmenities.addAll(amenities);
            return this;
        }

        public Builder onlyAvailable(boolean onlyAvailable) {
            this.onlyAvailable = onlyAvailable;
            return this;
        }

        public SearchCriteria build() {
            return new SearchCriteria(this);
        }
    }
}