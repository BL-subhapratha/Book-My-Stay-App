package src.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RoomCatalogue {

    private final Map<RoomType, Set<Amenity>> amenityMap;
    private final Map<RoomType, String>       descriptionMap;

    public RoomCatalogue() {
        amenityMap     = new HashMap<>();
        descriptionMap = new HashMap<>();
        seedDefaults();
    }

    private void seedDefaults() {
        // Single Room
        register(RoomType.SINGLE,
                "A cosy, well-appointed single room ideal for solo travellers.",
                EnumSet.of(Amenity.WIFI, Amenity.TV, Amenity.AIR_CONDITIONING,
                           Amenity.SAFE, Amenity.PARKING));

        // Double Room
        register(RoomType.DOUBLE,
                "Spacious double room with a king-size bed, perfect for couples.",
                EnumSet.of(Amenity.WIFI, Amenity.TV, Amenity.AIR_CONDITIONING,
                           Amenity.KING_BED, Amenity.MINI_BAR, Amenity.SAFE,
                           Amenity.BREAKFAST, Amenity.PARKING));

        // Suite
        register(RoomType.SUITE,
                "Luxury suite with panoramic sea views, private lounge, and premium amenities.",
                EnumSet.of(Amenity.WIFI, Amenity.TV, Amenity.AIR_CONDITIONING,
                           Amenity.KING_BED, Amenity.MINI_BAR, Amenity.SAFE,
                           Amenity.BATHTUB, Amenity.SEA_VIEW, Amenity.LOUNGE,
                           Amenity.BREAKFAST, Amenity.PARKING));
    }

    /**
     * Registers or updates a room type's catalogue entry.
     *
     * @param type        Room type
     * @param description Marketing description
     * @param amenities   Set of offered amenities
     */
    public void register(RoomType type, String description, Set<Amenity> amenities) {
        descriptionMap.put(type, description);
        amenityMap.put(type, Collections.unmodifiableSet(EnumSet.copyOf(amenities)));
    }

    /** Returns the amenity set for a room type, or an empty set if unknown. */
    public Set<Amenity> getAmenities(RoomType type) {
        return amenityMap.getOrDefault(type, Collections.emptySet());
    }

    /** Returns the description for a room type. */
    public String getDescription(RoomType type) {
        return descriptionMap.getOrDefault(type, "No description available.");
    }
}