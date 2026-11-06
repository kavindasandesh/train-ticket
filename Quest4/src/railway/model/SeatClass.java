package railway.model;

// Travelling classes
public enum SeatClass {
    FIRST("1st", "First class", 3, 1),    
    SECOND("2nd", "Second class", 4, 2), 
    THIRD("3rd", "Third class", 5, 2); 

    private final String shortName;
    private final String displayName;
    private final int seatsPerRow;
    private final int aisleAfter;

    SeatClass(String shortName, String displayName, int seatsPerRow, int aisleAfter) {
        this.shortName = shortName;
        this.displayName = displayName;
        this.seatsPerRow = seatsPerRow;
        this.aisleAfter = aisleAfter;
    }

    public String getShortName() { return shortName; }
    public String getDisplayName() { return displayName; }
    public int getSeatsPerRow() { return seatsPerRow; }
    public int getAisleAfter() { return aisleAfter; }

    public static SeatClass fromName(String text) {
        return valueOf(text.trim().toUpperCase());
    }

    @Override
    public String toString() {
        return displayName;
    }
}
