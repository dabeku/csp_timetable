package piano.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;

@PlanningSolution
public class TimeTable {

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Timeslot> timeslotList;
    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Room> roomList;
    @PlanningEntityCollectionProperty
    private List<Lesson> lessonList;

    @PlanningScore
    private HardSoftScore score;

    // No-arg constructor required for Timefold
    public TimeTable() {
    }

    public TimeTable(List<Timeslot> timeslotList, List<Room> roomList, List<Lesson> lessonList) {
        this.timeslotList = timeslotList;
        this.roomList = roomList;
        this.lessonList = lessonList;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public List<Timeslot> getTimeslotList() {
        return timeslotList;
    }

    public List<Room> getRoomList() {
        return roomList;
    }

    public List<Lesson> getLessonList() {
        return lessonList;
    }

    public HardSoftScore getScore() {
        return score;
    }

}
