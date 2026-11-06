package railway.model;

/** The customer on whose behalf the ticketing officer makes a booking. */
public class Passenger {
    private final String idNumber;
    private String name;
    private String contactNumber;

    public Passenger(String idNumber, String name, String contactNumber) {
        this.idNumber = idNumber.toUpperCase();
        this.name = name;
        this.contactNumber = contactNumber;
    }

    public String getIdNumber() { return idNumber; }
    public String getName() { return name; }
    public String getContactNumber() { return contactNumber; }

    public void setName(String name) { this.name = name; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    @Override
    public String toString() {
        return name + " (ID " + idNumber + ", Tel " + contactNumber + ")";
    }
}
