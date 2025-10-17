package com.umg.sources.logica;

import com.umg.sources.modelo.ProblemaTransporte;
import java.util.Arrays;
import java.util.Locale;

public class MetodoVogel {

    // -------- API pública (lo que usa tu switch) --------
    public static String resolver(ProblemaTransporte p) {
        Resultado r = calcular(p);
        return String.format(Locale.ROOT, "Vogel: costo total = %.4f", r.costoTotal);
    }

    // -------- Resultado detallado (opcional por si quieres mostrar la matriz) --------
    public static final class Resultado {
        public final double[][] asignacion; // respecto al problema balanceado
        public final double costoTotal;
        public final String[] filas;        // nombres de filas (balanceadas)
        public final String[] columnas;     // nombres de columnas (balanceadas)
        Resultado(double[][] x, double costo, String[] filas, String[] columnas) {
            this.asignacion = x;
            this.costoTotal = costo;
            this.filas = filas;
            this.columnas = columnas;
        }
    }

    // -------- Núcleo de VAM --------
    public static Resultado calcular(ProblemaTransporte p0) {
        //ProblemaTransporte p = balancearSiHaceFalta(p0);
        ProblemaTransporte p = TransporteUtils.balancearSiHaceFalta(p0);

        final int m = p.m(), n = p.n();
        double[] supply = Arrays.copyOf(p.oferta, m);
        double[] demand = Arrays.copyOf(p.demanda, n);
        double[][] cost = new double[m][n];
        for (int i = 0; i < m; i++) cost[i] = Arrays.copyOf(p.costos[i], n);

        boolean[] rowDone = new boolean[m];
        boolean[] colDone = new boolean[n];
        int activeRows = m, activeCols = n;
        double[][] x = new double[m][n];

        while (activeRows > 0 && activeCols > 0) {
            // 1) Penalidades fila/columna (segundo menor - menor)
            double bestPenalty = -1.0;
            boolean chooseRow = true;
            int idx = -1;
            double tieBestCellCost = Double.POSITIVE_INFINITY; // desempate por menor costo absoluto

            // Filas
            for (int i = 0; i < m; i++) if (!rowDone[i]) {
                double[] two = twoMinsInRow(cost[i], colDone);
                double penalty = (two[1] == Double.POSITIVE_INFINITY) ? 0.0 : (two[1] - two[0]);
                double minCell = two[0];
                if (penalty > bestPenalty || (absEq(penalty, bestPenalty) && minCell < tieBestCellCost)) {
                    bestPenalty = penalty;
                    tieBestCellCost = minCell;
                    chooseRow = true;
                    idx = i;
                }
            }
            // Columnas
            for (int j = 0; j < n; j++) if (!colDone[j]) {
                double[] two = twoMinsInCol(cost, rowDone, j);
                double penalty = (two[1] == Double.POSITIVE_INFINITY) ? 0.0 : (two[1] - two[0]);
                double minCell = two[0];
                if (penalty > bestPenalty || (absEq(penalty, bestPenalty) && minCell < tieBestCellCost)) {
                    bestPenalty = penalty;
                    tieBestCellCost = minCell;
                    chooseRow = false;
                    idx = j;
                }
            }

            if (idx == -1) break; // nada activo

            // 2) Elegir la celda de menor costo en la fila/columna seleccionada
            int iSel, jSel;
            if (chooseRow) { iSel = idx; jSel = argMinInRow(cost[iSel], colDone); }
            else           { jSel = idx; iSel = argMinInCol(cost, rowDone, jSel); }

            // 3) Asignar
            double q = Math.min(supply[iSel], demand[jSel]);
            x[iSel][jSel] += q;
            supply[iSel] -= q;
            demand[jSel] -= q;

            // 4) Cerrar fila/columna
            boolean closeRow = casiCero(supply[iSel]);
            boolean closeCol = casiCero(demand[jSel]);
            if (closeRow) { rowDone[iSel] = true; activeRows--; }
            if (closeCol) { colDone[jSel] = true; activeCols--; }
            // Si ambos 0, se cierran ambos (degeneración permitida en BFS inicial)
        }

        // 5) Costo total sobre el problema (ya balanceado)
        double total = 0.0;
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                total += x[i][j] * p.costos[i][j];

        return new Resultado(x, total, p.filas, p.columnas);
    }

    // -------- Helpers VAM --------
    private static final double EPS = 1e-9;
    private static boolean casiCero(double v) { return Math.abs(v) < EPS; }
    private static boolean absEq(double a, double b) { return Math.abs(a - b) < 1e-12; }

    private static double[] twoMinsInRow(double[] row, boolean[] colDone) {
        double a = Double.POSITIVE_INFINITY, b = Double.POSITIVE_INFINITY;
        for (int j = 0; j < row.length; j++) if (!colDone[j]) {
            double c = row[j];
            if (c < a) { b = a; a = c; }
            else if (c < b) { b = c; }
        }
        return new double[]{a, b};
    }

    private static double[] twoMinsInCol(double[][] cost, boolean[] rowDone, int col) {
        double a = Double.POSITIVE_INFINITY, b = Double.POSITIVE_INFINITY;
        for (int i = 0; i < cost.length; i++) if (!rowDone[i]) {
            double c = cost[i][col];
            if (c < a) { b = a; a = c; }
            else if (c < b) { b = c; }
        }
        return new double[]{a, b};
    }

    private static int argMinInRow(double[] row, boolean[] colDone) {
        double best = Double.POSITIVE_INFINITY; int arg = -1;
        for (int j = 0; j < row.length; j++) if (!colDone[j]) {
            if (row[j] < best) { best = row[j]; arg = j; }
        }
        return arg;
    }

    private static int argMinInCol(double[][] cost, boolean[] rowDone, int col) {
        double best = Double.POSITIVE_INFINITY; int arg = -1;
        for (int i = 0; i < cost.length; i++) if (!rowDone[i]) {
            double c = cost[i][col];
            if (c < best) { best = c; arg = i; }
        }
        return arg;
    }

}
