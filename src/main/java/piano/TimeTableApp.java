package piano;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.SolverEventListener;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import piano.domain.Lesson;
import piano.domain.Room;
import piano.domain.TimeTable;
import piano.domain.Timeslot;
import piano.solver.TimeTableConstraintProvider;

import java.io.File;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class TimeTableApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeTableApp.class);

    public static void main(String[] args) throws Exception {


        SolverConfig config = new SolverConfig()
                .withSolutionClass(TimeTable.class)
                .withEntityClasses(Lesson.class)
                .withConstraintProviderClass(TimeTableConstraintProvider.class)

                // The solver runs only for 5 seconds on this small dataset.
                // It's recommended to run for at least 5 minutes ("5m") otherwise.
                //.withTerminationSpentLimit(Duration.ofHours(9));
                .withTerminationSpentLimit(Duration.ofSeconds(3));
        config.setEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE);

        SolverFactory<TimeTable> solverFactory = SolverFactory.create(config);

        // Load the problem
        TimeTable problem = generateDemoDataFile();

        // Solve the problem
        Solver<TimeTable> solver = solverFactory.buildSolver();

        solver.addEventListener(new SolverEventListener<TimeTable>() {
            @Override
            public void bestSolutionChanged(BestSolutionChangedEvent<TimeTable> bestSolutionChangedEvent) {
                printTimetableFile(bestSolutionChangedEvent.getNewBestSolution());
                LOGGER.info("Score: " + bestSolutionChangedEvent.getNewBestScore());
            }
        });

        TimeTable solution = solver.solve(problem);

        // Visualize the solution
        printTimetableFile(solution);
    }

    public static TimeTable generateDemoDataFile() throws Exception {

        List<Room> locationList = new ArrayList<>();
        locationList.add(new Room("Innsbruck"));
        locationList.add(new Room("Sistrans"));

        List<Lesson> lessonList = new ArrayList<>();
        long id = 0;

        File file = new File("/Users/daniel/Documents/dev/CSP/CSP_WD/src/main/resources/plan.txt");
        Scanner sc = new Scanner(file);

        String student = null;
        Room location = null;
        List<Combination> combinations = new ArrayList<>();

        DayOfWeek dayOfWeek = DayOfWeek.MONDAY;

        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (line.isEmpty()) {
                lessonList.add(new Lesson(id++, "Piano", student, "Year 1"));
                student = null;
                location = null;
                combinations = new ArrayList<>();
                continue;
            }

            if (line.startsWith("+")) {
                student = line.substring(1);
                Global.map.put(student, combinations);
                continue;
            }
            if (line.startsWith(".")) {
                Global.durations.put(student, Integer.parseInt(line.substring(1)));
                continue;
            }
            if (line.startsWith("-")) {
                location = new Room(line.substring(1));
                continue;
            }
            String[] dayStr = line.split(" ");

            if (dayStr[0].equals("MO")) {
                dayOfWeek = DayOfWeek.MONDAY;
            }
            if (dayStr[0].equals("TU")) {
                dayOfWeek = DayOfWeek.TUESDAY;
            }
            if (dayStr[0].equals("WE")) {
                dayOfWeek = DayOfWeek.WEDNESDAY;
            }
            if (dayStr[0].equals("TH")) {
                dayOfWeek = DayOfWeek.THURSDAY;
            }
            if (dayStr[0].equals("FR")) {
                dayOfWeek = DayOfWeek.FRIDAY;
            }
            if (dayStr[0].equals("SA")) {
                dayOfWeek = DayOfWeek.SATURDAY;
            }
            if (dayStr[0].equals("SU")) {
                dayOfWeek = DayOfWeek.SUNDAY;
            }
            String[] fromTo = dayStr[1].split("-");
            LocalTime from = LocalTime.of(Integer.parseInt(fromTo[0].split(":")[0]), Integer.parseInt(fromTo[0].split(":")[1]));
            LocalTime to = LocalTime.of(Integer.parseInt(fromTo[1].split(":")[0]), Integer.parseInt(fromTo[1].split(":")[1]));

            combinations.add(new Combination(location));
            combinations.get(combinations.size()-1).timeSlot = new Timeslot(dayOfWeek, from, to);
        }

        List<Timeslot> timeSlotList = new ArrayList<>();

        LocalTime begin = LocalTime.of(8, 0);
        while(begin.getHour() < 19) {
            timeSlotList.add(new Timeslot(DayOfWeek.MONDAY, begin));
            timeSlotList.add(new Timeslot(DayOfWeek.TUESDAY, begin));
            timeSlotList.add(new Timeslot(DayOfWeek.WEDNESDAY, begin));
            timeSlotList.add(new Timeslot(DayOfWeek.THURSDAY, begin));
            timeSlotList.add(new Timeslot(DayOfWeek.FRIDAY, begin));
            timeSlotList.add(new Timeslot(DayOfWeek.SATURDAY, begin));
            timeSlotList.add(new Timeslot(DayOfWeek.SUNDAY, begin));
            begin = begin.plusMinutes(5);
        }

        return new TimeTable(timeSlotList, locationList, lessonList);
    }

    private static void printTimetableFile(TimeTable timeTable) {

        List<Lesson> lessons = timeTable.getLessonList();
        lessons.sort(new Comparator<Lesson>() {
            @Override
            public int compare(Lesson o1, Lesson o2) {
                if (o1.getTimeslot().getDayOfWeek().compareTo(o2.getTimeslot().getDayOfWeek()) < 0) {
                    return -1;
                }
                if (o1.getTimeslot().getDayOfWeek().compareTo(o2.getTimeslot().getDayOfWeek()) > 0) {
                    return 1;
                }
                if (o1.getTimeslot().getStartTime().equals(o2.getTimeslot().getStartTime())) {
                    return 0;
                }
                return o1.getTimeslot().getStartTime().isBefore(o2.getTimeslot().getStartTime()) ? -1 : 1;
            }
        });

        LOGGER.info("-------");
        for (Lesson lesson : lessons) {
            LOGGER.info(lesson.getStudent() + ": " + lesson.getTimeslot().getStartTime() + "-" + lesson.getTimeslot().getStartTime().plusMinutes(Global.durations.get(lesson.getStudent())) + " (" + lesson.getTimeslot().getDayOfWeek() + ") " + lesson.getRoom().getName());
        }
    }
}
