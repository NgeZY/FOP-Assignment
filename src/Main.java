import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public class Main {
    public static void main(String[] args){
        LocalDate today = LocalDate.now();
        LocalDate firstDateOfYear = today.with(TemporalAdjusters.firstDayOfYear());

        //Only get enum (Monday, Tuesday...)
        //getDayOfWeek returns the day of the LocalDate given
        DayOfWeek day = firstDateOfYear.getDayOfWeek();

        //day.getValue get the first day of year in integer but 7 for Sunday so need to do modulus
        int firstDayOfYear = day.getValue() % 7;
        int[] daysOfEachMonth = {
                31,
                isLeapYear(today.getYear()) ? 29 : 28,
                31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        };

        int startDayOfCurrentMonth = 0;
        for(int i = 0; i < today.getMonthValue(); i++){
            startDayOfCurrentMonth += daysOfEachMonth[i];
        }

        startDayOfCurrentMonth = startDayOfCurrentMonth % 7;

        printCurrentMonth(today.getMonth().name(), today.getYear(), daysOfEachMonth[today.getMonthValue()-1], startDayOfCurrentMonth);
    }

    public static boolean isLeapYear(int year){
        return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0);
    }

    public static void printCurrentMonth(String month, int year, int days, int startDayOfCurrentMonth){
        System.out.println("          " + month + " " + year);
        System.out.println("-----------------------------------");
        System.out.println("Sun Mon Tue Wed Thu Fri Sat");

        for(int i = 0; i < startDayOfCurrentMonth; i++){
            System.out.print("    ");
        }

        for(int i = 1; i <= days; i++){
            System.out.printf("%4d", i);
            if((i + startDayOfCurrentMonth) % 7 == 0)
                System.out.println();
        }

        System.out.println("\n-----------------------------------\n");
    }
}
