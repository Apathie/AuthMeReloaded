package fr.xephi.authme.util;

import fr.xephi.authme.ConsoleLogger;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Utility class for various operations used in the codebase.
 */
public final class Utils {

    /** Number of milliseconds in a minute. */
    public static final long MILLIS_PER_MINUTE = 60_000L;
    /** Number of milliseconds in an hour. */
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    // Utility class
    private Utils() {
    }

    /**
     * Compile Pattern sneaky without throwing Exception.
     *
     * @param pattern pattern string to compile
     *
     * @return the given regex compiled into Pattern object.
     */
    public static Pattern safePatternCompile(String pattern) {
        try {
            return Pattern.compile(pattern);
        } catch (Exception e) {
            ConsoleLogger.warning("Failed to compile pattern '" + pattern + "' - defaulting to allowing everything");
            return Pattern.compile(".*?");
        }
    }

    /**
     * Returns whether the class exists in the current class loader.
     *
     * @param className the class name to check
     *
     * @return true if the class is loaded, false otherwise
     */
    public static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Null-safe way to check whether a collection is empty or not.
     *
     * @param coll The collection to verify
     * @return True if the collection is null or empty, false otherwise
     */
    public static boolean isCollectionEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    /**
     * Return the available core count of the JVM.
     *
     * @return the core count
     */
    public static int getCoreCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static Duration convertMillisToSuitableUnit(long duration) {
        TimeUnit targetUnit;
        if (duration > 1000L * 60L * 60L * 24L) {
            targetUnit = TimeUnit.DAYS;
        } else if (duration > 1000L * 60L * 60L) {
            targetUnit = TimeUnit.HOURS;
        } else if (duration > 1000L * 60L) {
            targetUnit = TimeUnit.MINUTES;
        } else if (duration > 1000L) {
            targetUnit = TimeUnit.SECONDS;
        } else {
            targetUnit = TimeUnit.MILLISECONDS;
        }

        return new Duration(targetUnit, duration);
    }

    public static final class Duration {

        private final long duration;
        private final TimeUnit unit;

        Duration(TimeUnit targetUnit, long durationMillis) {
            this(targetUnit, durationMillis, TimeUnit.MILLISECONDS);
        }

        Duration(TimeUnit targetUnit, long sourceDuration, TimeUnit sourceUnit) {
            this.duration = targetUnit.convert(sourceDuration, sourceUnit);
            this.unit = targetUnit;
        }
    }

}
