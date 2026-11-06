package railway.model;

import java.time.LocalTime;

// Train halts with the time it departs/arrives
public class Stop {
    private final String station;
    private final LocalTime time;

    public Stop(String station, LocalTime time) {
        this.station = station;
        this.time = time;
    }

    public String getStation() { return station; }
    public LocalTime getTime() { return time; }

    @Override
    public String toString() {
        return station + " " + time.format(Train.TIME_FORMAT);
    }
}
