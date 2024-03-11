package piano;

import piano.domain.Room;
import piano.domain.Timeslot;

import java.util.ArrayList;
import java.util.List;

public class Combination {
    public Room location;
    public Timeslot timeSlot;


    public Combination(Room location) {
        this.location = location;
    }
}