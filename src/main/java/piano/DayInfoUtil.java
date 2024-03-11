package piano;

import piano.domain.Lesson;
import piano.domain.Timeslot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public class DayInfoUtil {

    public static void getStartEnd(List<Lesson> lessonList, DayOfWeek dayOfWeek, List<Timeslot> timeSlotList) {
        // Find list of timeslots when you have to work
        LocalTime start = null;
        LocalTime end = null;

        for (Lesson lesson : lessonList) {
            List<Combination> comb = Global.map.get(lesson.getStudent());
            for (Combination c : comb) {
                if (c.timeSlot.getDayOfWeek().equals(dayOfWeek)) {
                    if (start == null || start.isAfter(c.timeSlot.getStartTime())) {
                        start = c.timeSlot.getStartTime();
                    }
                    if (end == null || end.isBefore(c.timeSlot.getEndTime())) {
                        end = c.timeSlot.getEndTime();
                    }
                }
            }
        }

        if (start != null && end != null) {
            LocalTime current = start;
            while (current.isBefore(end)) {
                timeSlotList.add(new Timeslot(dayOfWeek, current, current.plusMinutes(5)));
                current = current.plusMinutes(5);
            }
        }
    }
}
