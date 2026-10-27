package com.sanjay.autodrive.track;

import java.util.ArrayList;
import java.util.List;

/** Closed Catmull-Rom centerline, shared by construction, checkpoints and later observations. */
public record TrackLayout(String id, String title, String purpose, int originX, int originZ, List<Point> anchors) {
    public static final int SURFACE_Y = -60;
    public static final double ROAD_HALF_WIDTH = 6;
    public record Point(double x, double z) {
        public double distance(Point p) { return Math.hypot(x - p.x, z - p.z); }
    }
    public record Sample(double x, double z, double distance, double yaw) {}

    public List<Sample> samples() {
        List<Point> dense = new ArrayList<>();
        int n = anchors.size();
        for (int i = 0; i < n; i++) {
            Point a = anchors.get((i + n - 1) % n), b = anchors.get(i), c = anchors.get((i + 1) % n), d = anchors.get((i + 2) % n);
            int steps = Math.max(16, (int) Math.ceil(b.distance(c) * 2));
            for (int j = 0; j < steps; j++) {
                double t = (double) j / steps;
                dense.add(new Point(interpolate(a.x, b.x, c.x, d.x, t) + originX,
                        interpolate(a.z, b.z, c.z, d.z, t) + originZ));
            }
        }
        List<Sample> result = new ArrayList<>();
        double distance = 0;
        for (int i = 0; i < dense.size(); i++) {
            Point p = dense.get(i), next = dense.get((i + 1) % dense.size());
            if (i > 0) distance += p.distance(dense.get(i - 1));
            result.add(new Sample(p.x, p.z, distance, Math.atan2(-(next.x - p.x), next.z - p.z)));
        }
        return List.copyOf(result);
    }
    public List<Sample> checkpoints() {
        List<Sample> result = new ArrayList<>();
        double nextDistance = 0;
        for (Sample sample : samples()) {
            if (sample.distance >= nextDistance) { result.add(sample); nextDistance += 35; }
        }
        // Do not place the final checkpoint too close to the start/finish.
        if (result.getLast().distance + 20 > samples().getLast().distance) result.removeLast();
        return List.copyOf(result);
    }
    private static double interpolate(double a, double b, double c, double d, double t) {
        return 0.5 * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t * t
                + (-a + 3 * b - 3 * c + d) * t * t * t);
    }
}
