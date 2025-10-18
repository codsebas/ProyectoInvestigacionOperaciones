package com.umg.sources.logica;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LPSolver2D_IO {

    public enum Type  { LE, GE, EQ }
    public enum Sense { MAX, MIN }

    public static class Constraint {
        public final double a, b, c;
        public final Type type;
        public Constraint(double a, double b, double c, Type type){
            this.a=a; this.b=b; this.c=c; this.type=type;
        }
    }

    public static class Result {
        public final boolean optimal;
        public final boolean unbounded;
        public final double x, y, z;
        public final DefaultTableModel tableau;
        public final List<DefaultTableModel> history;
        public final List<int[]> pivots;
        public final String message;

        public Result(boolean optimal, boolean unbounded, double x, double y, double z,
                      DefaultTableModel tableau, List<DefaultTableModel> history,
                      List<int[]> pivots, String message){
            this.optimal=optimal; this.unbounded=unbounded;
            this.x=x; this.y=y; this.z=z;
            this.tableau=tableau; this.history=history; this.pivots=pivots;
            this.message=message;
        }
    }

    
    public static Result solve(Sense sense, double[] objective, List<Constraint> cons){
        if (objective == null || objective.length != 2)
            throw new IllegalArgumentException("El objetivo debe ser [c1, c2].");
        if (cons == null || cons.isEmpty())
            throw new IllegalArgumentException("Debe proveer al menos una restricción.");

        final double M   = 1e6;
        final double EPS = 1e-12;

        
        List<String> varNames = new ArrayList<>();
        varNames.add("x"); varNames.add("y");

       
        List<double[]> rows = new ArrayList<>();
        for (Constraint r : cons) rows.add(new double[]{ r.a, r.b, r.c });

      
        boolean wasMin = (sense == Sense.MIN);
        double c1 = objective[0], c2 = objective[1];
        if (wasMin) { c1 = -c1; c2 = -c2; } // MIN -> MAX
        double[] z = new double[]{ -c1, -c2, 0.0 };

   
        List<Integer> basis = new ArrayList<>();

  
        for (int i = 0; i < cons.size(); i++) {
            Constraint r = cons.get(i);

            if (r.type == Type.LE) {
                varNames.add("s" + (countPrefix(varNames, "s") + 1));
                z = addColumn(rows, z, 0.0);                 
                int colS = varNames.size() - 1;
                rows.set(i, setAt(rows.get(i), colS, 1.0));
                basis.add(colS);

            } else if (r.type == Type.GE) {
                varNames.add("s" + (countPrefix(varNames, "s") + 1));
                z = addColumn(rows, z, 0.0);
                int colS = varNames.size() - 1;
                rows.set(i, setAt(rows.get(i), colS, -1.0));

                varNames.add("a" + (countPrefix(varNames, "a") + 1));
                z = addColumn(rows, z, 0.0);
                int colA = varNames.size() - 1;
                rows.set(i, setAt(rows.get(i), colA, 1.0));
                basis.add(colA);

            } else { // EQ
                varNames.add("a" + (countPrefix(varNames, "a") + 1));
                z = addColumn(rows, z, 0.0);
                int colA = varNames.size() - 1;
                rows.set(i, setAt(rows.get(i), colA, 1.0));
                basis.add(colA);
            }
        }

        // Penalización en Z para artificiales
        for (int j = 0; j < varNames.size(); j++)
            if (varNames.get(j).startsWith("a")) z[j] = -M;

        // Limpiar Z si hay artificiales en base
        for (int i = 0; i < rows.size(); i++) {
            int bj = basis.get(i);
            if (varNames.get(bj).startsWith("a")) {
                double[] ri = rows.get(i);
                for (int j = 0; j < z.length; j++) z[j] += M * ri[j];
            }
        }

        // Historial
        List<DefaultTableModel> history = new ArrayList<>();
        List<int[]> pivots = new ArrayList<>();
        history.add(buildTable(varNames, rows, z)); // estado inicial

        // Simplex
        int maxIter = 500;
        while (true) {
            int enter = argMin(z, 0, z.length - 1);
            if (z[enter] >= -1e-9) break; // óptimo

            // Razón mínima
            int leave = -1; double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < rows.size(); i++) {
                double[] ri = rows.get(i);
                double aij = ri[enter];
                if (aij > EPS) {
                    double ratio = ri[ri.length - 1] / aij; // RHS / aij
                    if (ratio < best) { best = ratio; leave = i; }
                }
            }
            if (leave == -1) {
                return new Result(false, true, Double.NaN, Double.NaN, Double.NaN,
                        buildTable(varNames, rows, z), history, pivots, "Problema no acotado.");
            }

           
            pivots.add(new int[]{leave, enter});
            pivot(rows, z, leave, enter, EPS);
            basis.set(leave, enter);

            history.add(buildTable(varNames, rows, z));
            if (--maxIter <= 0) break;
        }

        double[] sol = new double[varNames.size()];
        Arrays.fill(sol, 0.0);
        for (int i = 0; i < rows.size(); i++) {
            int bj = basis.get(i);
            sol[bj] = rows.get(i)[rows.get(i).length - 1]; // RHS
        }
        int ix = varNames.indexOf("x");
        int iy = varNames.indexOf("y");
        double x = ix >= 0 ? sol[ix] : 0.0;
        double y = iy >= 0 ? sol[iy] : 0.0;

        double zVal = z[z.length - 1];
        if (wasMin) zVal = -zVal;

        for (int i = 0; i < rows.size(); i++) {
            int bj = basis.get(i);
            if (varNames.get(bj).startsWith("a") && rows.get(i)[rows.get(i).length - 1] > 1e-6) {
                return new Result(false, false, x, y, zVal,
                        buildTable(varNames, rows, z), history, pivots, "Modelo infactible.");
            }
        }

        return new Result(true, false, x, y, zVal,
                buildTable(varNames, rows, z), history, pivots, "Óptimo encontrado.");
    }

    // ---------------- utilidades internas ----------------

    /** Inserta NUEVA columna (antes del RHS) en todas las filas y devuelve z con esa nueva columna. */
    private static double[] addColumn(List<double[]> rows, double[] z, double defaultVal){
        for (int i = 0; i < rows.size(); i++)
            rows.set(i, insertBeforeRHS(rows.get(i), defaultVal));
        return insertBeforeRHS(z, 0.0);
    }

 
    private static double[] insertBeforeRHS(double[] row, double val){
        int n = row.length;
        double rhs = row[n - 1];
        double[] out = Arrays.copyOf(row, n + 1);
        out[n] = rhs;     
        out[n - 1] = val;  
        return out;
    }

   
    private static double[] setAt(double[] row, int col, double val){
        double[] out = Arrays.copyOf(row, row.length);
        out[col] = val;
        return out;
    }

    private static int countPrefix(List<String> names, String prefix){
        int c = 0; for (String s : names) if (s.startsWith(prefix)) c++; return c;
    }

    private static int argMin(double[] a, int from, int toExclusive){
        int arg = from; double best = a[from];
        for (int j = from + 1; j < toExclusive; j++) if (a[j] < best) { best = a[j]; arg = j; }
        return arg;
    }

    private static void pivot(List<double[]> rows, double[] z, int r, int c, double EPS){
        int n = rows.get(0).length;
        double[] R = rows.get(r);
        double piv = R[c];

        
        double[] Rn = Arrays.copyOf(R, n);
        for (int j = 0; j < n; j++) Rn[j] /= piv;
        rows.set(r, Rn);

        
        for (int i = 0; i < rows.size(); i++){
            if (i == r) continue;
            double[] Ri = rows.get(i);
            double f = Ri[c];
            if (Math.abs(f) > EPS){
                double[] Rnew = Arrays.copyOf(Ri, n);
                for (int j = 0; j < n; j++) Rnew[j] -= f * Rn[j];
                rows.set(i, Rnew);
            }
        }

       
        double fz = z[c];
        if (Math.abs(fz) > EPS){
            for (int j = 0; j < n; j++) z[j] -= fz * Rn[j];
        }
    }

    private static DefaultTableModel buildTable(List<String> varNames, List<double[]> rows, double[] z){
        String[] cols = new String[varNames.size() + 1];
        for (int i = 0; i < varNames.size(); i++) cols[i] = varNames.get(i);
        cols[cols.length - 1] = "RHS";

        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (double[] r : rows) model.addRow(toObj(r));
        model.addRow(toObj(z)); // fila Z
        return model;
    }

    private static Object[] toObj(double[] a){
        Object[] o = new Object[a.length];
        for (int i = 0; i < a.length; i++) o[i] = round6(a[i]);
        return o;
    }

    private static double round6(double v){
        return Math.abs(v) < 1e-12 ? 0.0 : Math.round(v * 1e6) / 1e6;
    }
}
