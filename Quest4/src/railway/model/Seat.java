package railway.model;

// A seat identified by its carriage number and its number inside that carriage
public final class Seat implements Comparable<Seat> {
    private final int carriage;
    private final int number;

    public Seat(int carriage, int number) {
        this.carriage = carriage;
        this.number = number;
    }

    public int getCarriage() { return carriage; }
    public int getNumber() { return number; }

    public String toCsv() {
        return carriage + "-" + number;
    }

    public static Seat parse(String text) {
        String[] p = text.trim().split("-");
        return new Seat(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    @Override
    public int compareTo(Seat o) {
        return carriage != o.carriage ? Integer.compare(carriage, o.carriage)
                                      : Integer.compare(number, o.number);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Seat && ((Seat) o).carriage == carriage && ((Seat) o).number == number;
    }

    @Override
    public int hashCode() {
        return carriage * 1000 + number;
    }

    // Label shown to the officer
    @Override
    public String toString() {
        return "C" + carriage + "-" + number;
    }
}
