package com.example.aznagy.paintproplayer.utils;

public class PlayerUtils {

    /**
     * Function to convert milliseconds time to
     * Timer Format
     * Hours:Minutes:Seconds
     */
    public static String milliSecondsToTimer(long milliseconds) {
        String time;
        long hours = (long) (milliseconds / (1000 * 60 * 60));
        long minutes = (long) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (long) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        // Add hours if there
        if (hours > 0) {
            time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            time = String.format("%02d:%02d", minutes, seconds);
        }
        return time;
    }

    public static String milliSecondsToCountdownTimer(long duration, long progress) {
        long milliseconds = duration - progress;
        String time;
        long hours = (long) (milliseconds / (1000 * 60 * 60));
        long minutes = (long) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (long) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        if (hours > 0) {
            time = String.format("-%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            time = String.format("-%02d:%02d", minutes, seconds);
        }
        return time;
    }

    /**
     * Function to get Progress percentage
     *
     * @param currentDuration
     * @param totalDuration
     */
    public static int getProgressPercentage(long currentDuration, long totalDuration) {
        Double percentage;
        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);
        percentage = (((double) currentSeconds) / totalSeconds) * 100;
        return percentage.intValue();
    }

    /**
     * Function to change progress to timer
     *
     * @param progress      -
     * @param totalDuration returns current duration in milliseconds
     */
    public static int progressToTimer(int progress, int totalDuration) {
        int currentDuration;
        currentDuration = (int) ((((double) progress) / 100) * (((double) totalDuration) / 1000));
        return currentDuration * 1000;
    }
}