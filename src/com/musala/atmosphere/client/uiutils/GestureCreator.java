package com.musala.atmosphere.client.uiutils;

import com.musala.atmosphere.client.geometry.Point;
import com.musala.atmosphere.commons.beans.SwipeDirection;
import com.musala.atmosphere.commons.gesture.Anchor;
import com.musala.atmosphere.commons.gesture.Gesture;
import com.musala.atmosphere.commons.gesture.Timeline;
import com.musala.atmosphere.commons.util.Pair;

/**
 * Serves to create {@link Gesture} later performed by the gesture player.
 * 
 * @author delyan.dimitrov
 * 
 */
public class GestureCreator {
    private static int DOUBLE_TAP_INTERVAL = 150;

    private static int PINCH_DURATION = 500;

    private static int SWIPE_INTERVAL = 250;

    private static int SWIPE_DISTANCE = 75;

    /**
     * Creates a double tap {@link Gesture} on the passed point.
     * 
     * @param x
     *        - the x coordinate of the tap point
     * @param y
     *        - the y coordinate of the tap point
     * @return a {@link Gesture} that is a double tap
     */
    public static Gesture createDoubleTap(float x, float y) {
        Gesture doubleTap = new Gesture();

        Timeline firstTapTimeline = new Timeline();
        Anchor firstTap = new Anchor(x, y, 0);
        firstTapTimeline.add(firstTap);
        doubleTap.add(firstTapTimeline);

        Timeline secondTapTimeline = new Timeline();
        Anchor secondTap = new Anchor(x, y, DOUBLE_TAP_INTERVAL);
        secondTapTimeline.add(secondTap);
        doubleTap.add(secondTapTimeline);

        return doubleTap;
    }

    /**
     * Creates a pinch in {@link Gesture}.
     * 
     * @param firstFingerInitial
     *        - the initial position of the first finger performing the gesture
     * @param secondFingerInitial
     *        - the initial position of the second finger performing the gesture
     * @return a pinch in {@link Gesture}
     */
    public static Gesture createPinchIn(Point firstFingerInitial, Point secondFingerInitial) {
        // calculating the point where the two fingers will meet
        int toX = (firstFingerInitial.getX() + secondFingerInitial.getX()) / 2;
        int toY = (firstFingerInitial.getY() + secondFingerInitial.getY()) / 2;
        Point to = new Point(toX, toY);

        Timeline firstFingerMovement = createScrollMovement(firstFingerInitial, to, PINCH_DURATION);
        Timeline secondFingerMovement = createScrollMovement(secondFingerInitial, to, PINCH_DURATION);

        Gesture pinchIn = new Gesture();
        pinchIn.add(firstFingerMovement);
        pinchIn.add(secondFingerMovement);

        return pinchIn;
    }

    /**
     * Creates a pinch out {@link Gesture}.
     * 
     * @param firstFingerEnd
     *        - the point where the first finger will be at the end of the pinch
     * @param secondFingerEnd
     *        - the point where the second finger will be at the end of the pinch
     * @return a pinch out {@link Gesture}
     */
    public static Gesture createPinchOut(Point firstFingerEnd, Point secondFingerEnd) {
        // calculating the start point of the gesture
        int fromX = (firstFingerEnd.getX() + secondFingerEnd.getX()) / 2;
        int fromY = (firstFingerEnd.getY() + secondFingerEnd.getY()) / 2;
        Point from = new Point(fromX, fromY);

        Timeline firstFingerMovement = createScrollMovement(from, firstFingerEnd, PINCH_DURATION);
        Timeline secondFingerMovement = createScrollMovement(from, secondFingerEnd, PINCH_DURATION);

        Gesture pinchOut = new Gesture();
        pinchOut.add(firstFingerMovement);
        pinchOut.add(secondFingerMovement);

        return pinchOut;
    }

    /**
     * Creates a swipe gesture {@link Gesture} from passed point in passed direction.
     * 
     * @param startPoint
     *        - the start point of the swipe
     * @param swipeDirection
     *        - the direction of the swipe
     * @param resolution
     *        - this is a pair which present the resolution of the screen
     * @return a {@link Gesture} that is a swipe
     */
    public static Gesture createSwipe(Point startPoint, SwipeDirection swipeDirection, Pair<Integer, Integer> resolution) {
        int endX = startPoint.getX();
        int endY = startPoint.getY();

        switch (swipeDirection) {
            case UP:
                endY = Math.max(endY - SWIPE_DISTANCE, 0);
                break;
            case DOWN:
                endY = Math.min(endY + SWIPE_DISTANCE, resolution.getValue());
                break;
            case LEFT:
                endX = Math.max(endX - SWIPE_DISTANCE, 0);
                break;
            case RIGHT:
                endX = Math.min(endX + SWIPE_DISTANCE, resolution.getKey());
                break;
        }

        Point endPoint = new Point(endX, endY);
        Timeline swipeTimeline = createScrollMovement(startPoint, endPoint, SWIPE_INTERVAL);

        Gesture swipe = new Gesture();
        swipe.add(swipeTimeline);

        return swipe;
    }

    /**
     * Defines a {@link Timeline} for a scroll between two points with a given duration.
     * 
     * @param from
     *        - the initial point of the motion
     * @param to
     *        - the end point of the motion
     * @param duration
     *        - the duration of the scroll motion in milliseconds
     * @return a {@link Timeline} representing a scroll motion
     */
    private static Timeline createScrollMovement(Point from, Point to, int duration) {
        Timeline scroll = new Timeline();

        Anchor startPoint = new Anchor(from.getX(), from.getY(), 0);
        scroll.add(startPoint);
        Anchor endPoint = new Anchor(to.getX(), to.getY(), duration);
        scroll.add(endPoint);

        return scroll;
    }
}