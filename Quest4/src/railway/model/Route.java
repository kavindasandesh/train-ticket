package railway.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// The three routes opened for express train booking

public enum Route {
    COLOMBO_KANDY("CK", "Colombo - Kandy",
            "Colombo Fort", "Ragama", "Gampaha", "Veyangoda", "Polgahawela",
            "Rambukkana", "Peradeniya Junction", "Kandy"),
    COLOMBO_GALLE("CG", "Colombo - Galle",
            "Colombo Fort", "Mount Lavinia", "Panadura", "Kalutara South",
            "Bentota", "Hikkaduwa", "Galle"),
    COLOMBO_KANKESANTHURAI("CJ", "Colombo - Kankesanthurai",
            "Colombo Fort", "Ragama", "Veyangoda", "Polgahawela", "Anuradhapura",
            "Vavuniya", "Kilinochchi", "Jaffna", "Kankesanthurai");

    private final String code;
    private final String displayName;
    private final List<String> stations;

    Route(String code, String displayName, String... stations) {
        this.code = code;
        this.displayName = displayName;
        this.stations = Collections.unmodifiableList(Arrays.asList(stations));
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }

    // Stations ordered from Colombo Fort outwards
    public List<String> getStations() { return stations; }

    public String getOrigin() { return stations.get(0); }
    public String getDestination() { return stations.get(stations.size() - 1); }

    // Index of a station in the outward order
    public int indexOfStation(String name) {
        for (int i = 0; i < stations.size(); i++) {
            if (stations.get(i).equalsIgnoreCase(name.trim())) return i;
        }
        return -1;
    }

    public String getJourneyLabel(Direction d) {
        return d == Direction.OUTBOUND ? getOrigin() + " -> " + getDestination()
                                       : getDestination() + " -> " + getOrigin();
    }

    public static Route fromCode(String code) {
        for (Route r : values()) {
            if (r.code.equalsIgnoreCase(code.trim())) return r;
        }
        throw new IllegalArgumentException("Unknown route code: " + code);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
