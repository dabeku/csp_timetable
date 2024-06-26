package piano.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class Lesson {

    @PlanningId
    private Long id;

    private String subject;
    private String student;
    private String studentGroup;

    @PlanningVariable
    private Timeslot timeslot;
    @PlanningVariable
    private Room room;

    // No-arg constructor required for Timefold
    public Lesson() {
    }

    public Lesson(long id, String subject, String student, String studentGroup) {
        this.id = id;
        this.subject = subject;
        this.student = student;
        this.studentGroup = studentGroup;
    }

    public Lesson(long id, String subject, String student, String studentGroup, Timeslot timeslot, Room room) {
        this(id, subject, student, studentGroup);
        this.timeslot = timeslot;
        this.room = room;
    }

    @Override
    public String toString() {
        return subject + "(" + id + ")";
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public Long getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getStudent() {
        return student;
    }

    public String getStudentGroup() {
        return studentGroup;
    }

    public Timeslot getTimeslot() {
        return timeslot;
    }

    public void setTimeslot(Timeslot timeslot) {
        this.timeslot = timeslot;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

}
