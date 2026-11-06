package railway.model;

// Direction of travel of a train on its route
public enum Direction {
    OUTBOUND("OUT", "Outbound"),      // away from Colombo
    INBOUND("IN", "Return");          // back towards Colombo

    private final String code;
    private final String label;

    Direction(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static Direction fromCode(String code) {
        for (Direction d : values()) {
            if (d.code.equalsIgnoreCase(code.trim())) return d;
        }
        throw new IllegalArgumentException("Unknown direction code: " + code);
    }
}
