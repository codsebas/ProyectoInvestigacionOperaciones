package com.umg.sources.logica;

import java.util.*;

public class LPSolver2D {

    public enum Type { LE, GE, EQ } // <=, >=, =

    public static class Constraint {
        public final double a, b, c;
        public final Type type;
        public Constraint(double a, double b, double c, Type type) {
            this.a = a; this.b = b; this.c = c; this.type = type;
        }
        Constraint toLE() { // GE -> multiplico por -1 para comparar como <=
            return (type == Type.GE) ? new Constraint(-a, -b, -c, Type.LE) : this;
        }
        Line asLine() { return new Line(a, b, c); }
    }

    static class Line { final double a, b, c; Line(double a, double b, double c){this.a=a;this.b=b;this.c=c;} }

    public static class Result {
        public final boolean feasible, unbounded;
        public final double x, y, z;
        public final String message;
        Result(boolean feasible, boolean unbounded, double x, double y, double z, String message){
            this.feasible=feasible; this.unbounded=unbounded; this.x=x; this.y=y; this.z=z; this.message=message;
        }
    }

    private static final double EPS = 1e-9;

    private static boolean satisfies(List<Constraint> cons, double x, double y){
        // no negatividad
        if (x < -1e-7 || y < -1e-7) return false;
        for (Constraint c : cons){
            if (c.type == Type.EQ){
                if (Math.abs(c.a*x + c.b*y - c.c) > 1e-7) return false;
            } else {
                Constraint le = c.toLE();
                if (le.a*x + le.b*y - le.c > 1e-7) return false; // <=
            }
        }
        return true;
    }

    private static Optional<double[]> intersect(Line l1, Line l2){
        double det = l1.a*l2.b - l2.a*l1.b;
        if (Math.abs(det) < EPS) return Optional.empty(); // paralelas
        double x = (l1.c*l2.b - l2.c*l1.b) / det;
        double y = (l1.a*l2.c - l2.a*l1.c) / det;
        return Optional.of(new double[]{x, y});
    }

    /** Resuelve max/min Z=c1*x+c2*y con hasta 4 restricciones y x>=0,y>=0 */
    public static Result solve(List<Constraint> cons, double c1, double c2, boolean maximize){
        // BORDES de todas las restricciones
        List<Line> lines = new ArrayList<>();
        for (Constraint c : cons) lines.add((c.type==Type.GE? c.toLE() : c).asLine());
        // Ejes
        lines.add(new Line(1,0,0)); // x = 0
        lines.add(new Line(0,1,0)); // y = 0

        // Candidatos = intersecciones de pares de líneas + origen
        Set<String> seen = new HashSet<>();
        List<double[]> candidates = new ArrayList<>();
        for (int i=0;i<lines.size();i++){
            for (int j=i+1;j<lines.size();j++){
                var p = intersect(lines.get(i), lines.get(j));
                p.ifPresent(pt -> {
                    String key = String.format(java.util.Locale.US, "%.9f|%.9f", pt[0], pt[1]);
                    if (seen.add(key)) candidates.add(pt);
                });
            }
        }
        candidates.add(new double[]{0,0});

        double bestZ = maximize ? -Double.MAX_VALUE : Double.MAX_VALUE;
        double bx = Double.NaN, by = Double.NaN;
        boolean any = false;

        for (double[] pt : candidates){
            double x = pt[0], y = pt[1];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            if (satisfies(cons, x, y)){
                any = true;
                double z = c1*x + c2*y;
                if ((maximize && z > bestZ + 1e-9) || (!maximize && z < bestZ - 1e-9)){
                    bestZ = z; bx = x; by = y;
                }
            }
        }

        if (!any) return new Result(false, false, Double.NaN, Double.NaN, Double.NaN, "Infactible");
        if (!Double.isFinite(bestZ)) return new Result(true, true, Double.NaN, Double.NaN, Double.NaN, "No acotado");
        return new Result(true, false, bx, by, bestZ,
                String.format(java.util.Locale.US, "x=%.4f, y=%.4f, Z=%.4f", bx, by, bestZ));
    }
}