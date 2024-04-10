package piano.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import piano.Combination;
import piano.Global;
import piano.domain.Lesson;
import piano.domain.Room;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.toList;

public class TimeTableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard
                noOverlapConstraint(constraintFactory),
                possibleTimeAndPlaceConstraint(constraintFactory),
                locationChangeBreakConstraint(constraintFactory),

                // Soft
                locationStabilityConstraint(constraintFactory),
                consecutiveLessonsConstraint(constraintFactory),
        };
    }

    /**
     * Hard: Lessons can't overlap:
     * - 10:30-10:45
     * - 10:40-11:00
     * is not allowed.
     */
    Constraint noOverlapConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                        .groupBy(l ->l.getTimeslot().getDayOfWeek(), toList())
                .penalize(HardSoftScore.ONE_HARD, (dayOfWeek, lessons) -> {
                    int penalty = 0;
                    lessons.sort((o1, o2) -> {
                        if (o1.getTimeslot().getStartTime().equals(o2.getTimeslot().getStartTime())) {
                            return 0;
                        }
                        return o1.getTimeslot().getStartTime().isBefore(o2.getTimeslot().getStartTime()) ? -1 : 1;
                    });
                    for (int i = 0; i < lessons.size() - 1; i++) {
                        Lesson lesson = lessons.get(i);
                        Lesson nextLesson = lessons.get(i+1);
                        int durationInMin = Global.durations.get(lesson.getStudent());

                        /*
                         * 08:30 + 30min > 08:45
                         */
                        if (lesson.getTimeslot().getStartTime().plusMinutes(durationInMin).isAfter(nextLesson.getTimeslot().getStartTime())) {
                            penalty++;
                        }
                    }
                    return penalty;
                })
                .asConstraint("noOverlapConstraint");
    }

    /**
     * Hard: Every lesson of a student must be within possible time and location
     * - Hannes, MO, Innsbruck, 14:00-16:00
     * Lesson must be between 14:00 and 16:00 and be located in Innsbruck
     */
    Constraint possibleTimeAndPlaceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .groupBy(l ->l.getTimeslot().getDayOfWeek(), toList())
                .penalize(HardSoftScore.ONE_HARD, (dayOfWeek, lessons) -> {
                    int penalty = 0;
                    lessons.sort((o1, o2) -> {
                        if (o1.getTimeslot().getStartTime().equals(o2.getTimeslot().getStartTime())) {
                            return 0;
                        }
                        return o1.getTimeslot().getStartTime().isBefore(o2.getTimeslot().getStartTime()) ? -1 : 1;
                    });
                    for (int i = 0; i < lessons.size(); i++) {
                        Lesson lesson = lessons.get(i);
                        Room location = lesson.getRoom();
                        DayOfWeek day = lesson.getTimeslot().getDayOfWeek();
                        // Contains possible (multiple) time ranges (per day) + location a student 'has time'
                        List<Combination> combinations = Global.map.get(lesson.getStudent());
                        // Duration in min of a single student
                        int durationInMin = Global.durations.get(lesson.getStudent());

                        boolean isTimePossible = false;

                        // Check if time and day are possible
                        for (Combination c : combinations) {
                            if (c.location.getName().equals(location.getName())) {
                                if (c.timeSlot.getDayOfWeek().equals(day)) {
                                    if (!lesson.getTimeslot().getStartTime().isBefore(c.timeSlot.getStartTime()) &&
                                            !lesson.getTimeslot().getStartTime().plusMinutes(durationInMin).isAfter(c.timeSlot.getEndTime())) {
                                        // Day + time + place are possible
                                        isTimePossible = true;
                                    }
                                }
                            }
                        }

                        if (!isTimePossible) {
                            penalty++;
                        }
                    }
                    return penalty;
                })
                .asConstraint("possibleTimeAndPlaceConstraint");
    }

    Constraint locationChangeBreakConstraint(ConstraintFactory constraintFactory) {
        // A teacher prefers to teach in a single room.
        return constraintFactory
                .forEach(Lesson.class)
                .groupBy(l ->l.getTimeslot().getDayOfWeek(), toList())
                .penalize(HardSoftScore.ONE_SOFT, (dayOfWeek, lessons) -> {
                    int penalty = 0;
                    lessons.sort((o1, o2) -> {
                        if (o1.getTimeslot().getStartTime().equals(o2.getTimeslot().getStartTime())) {
                            return 0;
                        }
                        return o1.getTimeslot().getStartTime().isBefore(o2.getTimeslot().getStartTime()) ? -1 : 1;
                    });
                    for (int i = 0; i < lessons.size() - 1; i++) {
                        Lesson lesson = lessons.get(i);
                        Lesson nextLesson = lessons.get(i+1);
                        if (!lesson.getRoom().getName().equals(nextLesson.getRoom().getName())) {
                            // Gap must be between 25 minutes and 45 minutes. Otherwise it's not a valid solution
                            int durationInMin = Global.durations.get(lesson.getStudent());
                            LocalTime before = lesson.getTimeslot().getStartTime().plusMinutes(durationInMin);
                            LocalTime after = nextLesson.getTimeslot().getStartTime();
                            Duration between = Duration.between(before, after);

                            final int MIN_BREAK_IN_MIN = 25;
                            final int MAX_BREAK_IN_MIN = 45;

                            // The closer we come to 0 the more penalty it is
                            if (between.compareTo(Duration.ofMinutes(MIN_BREAK_IN_MIN)) < 0) {
                                long min = between.toMinutes();
                                penalty += (int) (MIN_BREAK_IN_MIN - min);
                            }
                            if (between.compareTo(Duration.ofMinutes(MAX_BREAK_IN_MIN)) > 0) {
                                // The more we have the more penalty
                                long min = between.toMinutes();
                                penalty += (int) min - MAX_BREAK_IN_MIN;
                            }
                        }
                    }
                    return penalty;
                })
                .asConstraint("locationChangeBreakConstraint");
    }

    /**
     * Soft: Location switch: Reward staying at location.
     */
    Constraint locationStabilityConstraint(ConstraintFactory constraintFactory) {
        // A teacher prefers to teach in a single room.
        return constraintFactory
                .forEach(Lesson.class)
                .groupBy(l ->l.getTimeslot().getDayOfWeek(), toList())
                .penalize(HardSoftScore.ONE_SOFT, (dayOfWeek, lessons) -> {
                    lessons.sort((o1, o2) -> {
                        if (o1.getTimeslot().getStartTime().equals(o2.getTimeslot().getStartTime())) {
                            return 0;
                        }
                        return o1.getTimeslot().getStartTime().isBefore(o2.getTimeslot().getStartTime()) ? -1 : 1;
                    });
                    int roomChangeCount = 0;
                    for (int i = 0; i < lessons.size() - 1; i++) {
                        Lesson lesson = lessons.get(i);
                        Lesson nextLesson = lessons.get(i+1);
                        if (!lesson.getRoom().getName().equals(nextLesson.getRoom().getName())) {
                            roomChangeCount++;
                        }
                    }
                    if (roomChangeCount == 2) {
                        return 100;
                    }
                    if (roomChangeCount > 2) {
                        return 1000;
                    }
                    return 0;
                })
                .asConstraint("locationStabilityConstraint");
    }

    /**
     * Reward lessons that are consecutive: Lessons with no gap are rewarded
     */
    Constraint consecutiveLessonsConstraint(ConstraintFactory constraintFactory) {

        return constraintFactory
                .forEach(Lesson.class)
                .groupBy(l ->l.getTimeslot().getDayOfWeek(), toList())
                .reward(HardSoftScore.ONE_SOFT, (dayOfWeek, lessons) -> {
                    int reward = 0;
                    lessons.sort((o1, o2) -> {
                        if (o1.getTimeslot().getStartTime().equals(o2.getTimeslot().getStartTime())) {
                            return 0;
                        }
                        return o1.getTimeslot().getStartTime().isBefore(o2.getTimeslot().getStartTime()) ? -1 : 1;
                    });
                    for (int i = 0; i < lessons.size() - 1; i++) {
                        Lesson lesson = lessons.get(i);
                        Lesson nextLesson = lessons.get(i+1);

                        int durationInMin = Global.durations.get(lesson.getStudent());
                        LocalTime before = lesson.getTimeslot().getStartTime().plusMinutes(durationInMin);
                        LocalTime after = nextLesson.getTimeslot().getStartTime();
                        Duration between = Duration.between(before, after);
                        if (between.compareTo(Duration.ofMinutes(0)) == 0) {
                            reward++;
                        }

                    }
                    return reward;
                })
                .asConstraint("consecutiveLessonsConstraint");
    }
}
