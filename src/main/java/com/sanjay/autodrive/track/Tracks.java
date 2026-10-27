package com.sanjay.autodrive.track;

import java.util.List;
import static com.sanjay.autodrive.track.TrackLayout.Point;

public final class Tracks {
    public static final List<TrackLayout> ALL = List.of(
        new TrackLayout("rookie", "Rookie Circuit", "Long straight, sweeping right, chicane, hairpin and linked corners", 0, 0, points(
            0,0, 70,0, 120,8, 146,35, 143,70, 115,92, 85,90, 67,109, 88,133,
            123,139, 143,157, 132,179, 99,188, 55,172, 28,142, 4,155,
            -24,143, -39,111, -16,79, -34,46, -34,12)),
        new TrackLayout("technical", "Switchback Park", "Alternating hairpins, two chicanes and a tight technical sector", 300, 0, points(
            0,0, 75,0, 122,10, 143,37, 134,66, 103,79, 70,63, 43,67,
            39,94, 65,109, 109,107, 138,132, 126,163, 91,177,
            60,151, 33,147, 20,176, -10,182, -36,160, -36,121, -15,89, -34,54, -34,15)),
        new TrackLayout("grand_prix", "Grand Prix Gardens", "Fast sweepers, a braking hairpin and a flowing S section; held-out evaluation layout", 600, 0, points(
            0,0, 80,0, 137,10, 165,42, 156,79, 122,96, 98,79, 69,83,
            69,111, 108,132, 150,142, 165,168, 143,195, 99,200,
            63,176, 35,187, 4,175, -5,143, -38,125, -52,89, -32,60, -38,23))
    );
    private Tracks() {}
    public static TrackLayout get(String id) {
        return ALL.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }
    private static List<Point> points(double... coordinates) {
        java.util.ArrayList<Point> result = new java.util.ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) result.add(new Point(coordinates[i], coordinates[i + 1]));
        return List.copyOf(result);
    }
}
