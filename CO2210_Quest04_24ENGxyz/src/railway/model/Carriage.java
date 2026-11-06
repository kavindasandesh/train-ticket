package railway.model;

/** One carriage of a train: its number, class and number of seats. */
public class Carriage {
    private final int number;
    private final SeatClass seatClass;
    private final int seatCount;

    public Carriage(int number, SeatClass seatClass, int seatCount) {
        this.number = number;
        this.seatClass = seatClass;
        this.seatCount = seatCount;
    }

    public int getNumber() { return number; }
    public SeatClass getSeatClass() { return seatClass; }
    public int getSeatCount() { return seatCount; }

    public boolean hasSeat(Seat s) {
        return s.getCarriage() == number && s.getNumber() >= 1 && s.getNumber() <= seatCount;
    }

    @Override
    public String toString() {
        return "Carriage " + number + " (" + seatClass.getDisplayName() + ", " + seatCount + " seats)";
    }
}
