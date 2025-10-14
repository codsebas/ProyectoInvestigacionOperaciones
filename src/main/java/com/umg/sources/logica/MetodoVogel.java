/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.logica;

import com.umg.sources.modelo.ProblemaTransporte;
import java.util.Arrays;

/**
 *
 * @author didhy
 */
public class MetodoVogel {
    
        public static final class Resultado {
        public final double[][] asignacion; // respecto al problema balanceado
        public final double costoTotal;
        public final String[] filas;
        public final String[] columnas;

        Resultado(double[][] x, double costo, String[] filas, String[] columnas) {
            this.asignacion = x;
            this.costoTotal = costo;
            this.filas = filas;
            this.columnas = columnas;
        }
}
        
    public static Resultado calcular(ProblemaTransporte p0) {
        ProblemaTransporte p = balancearSiHaceFalta(p0);

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
            // 1) Penalidades por fila/columna
            double bestPenalty = -1.0;
            boolean chooseRow = true;
            int idx = -1;
            double tieBestCellCost = Double.POSITIVE_INFINITY; // para desempatar con menor costo absoluto

            // filas
            for (int i = 0; i < m; i++) if (!rowDone[i]) {
                double[] two = twoMinsInRow(cost[i], colDone);
                double penalty = (two[1] == Double.POSITIVE_INFINITY) ? 0.0 : (two[1] - two[0]);
                double minCell = two[0];
                if (penalty > bestPenalty || (Math.abs(penalty - bestPenalty) < 1e-12 && minCell < tieBestCellCost)) {
                    bestPenalty = penalty;
                    tieBestCellCost = minCell;
                    chooseRow = true;
                    idx = i;
                }
            }
            // columnas
            for (int j = 0; j < n; j++) if (!colDone[j]) {
                double[] two = twoMinsInCol(cost, rowDone, j);
                double penalty = (two[1] == Double.POSITIVE_INFINITY) ? 0.0 : (two[1] - two[0]);
                double minCell = two[0];
                if (penalty > bestPenalty || (Math.abs(penalty - bestPenalty) < 1e-12 && minCell < tieBestCellCost)) {
                    bestPenalty = penalty;
                    tieBestCellCost = minCell;
                    chooseRow = false;
                    idx = j;
                }
            }

            if (idx == -1) break; // nada más por asignar

            // 2) Elegir celda de menor costo sobre la fila/columna seleccionada
            int iSel, jSel;
            if (chooseRow) {
                iSel = idx;
                jSel = argMinInRow(cost[iSel], colDone);
            } else {
                jSel = idx;
                iSel = argMinInCol(cost, rowDone, jSel);
            }

            // 3) Asignar
            double q = Math.min(supply[iSel], demand[jSel]);
            x[iSel][jSel] += q;
            supply[iSel] -= q;
            demand[jSel] -= q;

            // 4) Cierre de fila/columna
            boolean closeRow = casiCero(supply[iSel]);
            boolean closeCol = casiCero(demand[jSel]);

            if (closeRow) { rowDone[iSel] = true; activeRows--; }
            if (closeCol) { colDone[jSel] = true; activeCols--; }

            // Si ambos quedaron en 0, cerramos ambos (degeneración permitida)
        }

        // 5) Costo total con la matriz balanceada usada
        double total = 0.0;
        for (int i = 0; i < p.m(); i++)
            for (int j = 0; j < p.n(); j++)
                total += x[i][j] * p.costos[i][j];

        return new Resultado(x, total, p.filas, p.columnas);
    }

    public static String resolver(ProblemaTransporte p) {
        Resultado r = calcular(p);
        return String.format("Vogel: costo total = %.4f", r.costoTotal);
    }

    // ---------- Helpers ----------
    private static boolean casiCero(double v) { return Math.abs(v) < 1e-9; }

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

    private static ProblemaTransporte balancearSiHaceFalta(ProblemaTransporte p) {
        double S = 0, D = 0;
        for (double v : p.oferta) S += v;
        for (double v : p.demanda) D += v;
        if (Math.abs(S - D) < 1e-9) return p;

        if (S > D) {
            int n2 = p.n() + 1;
            double[] demanda = Arrays.copyOf(p.demanda, n2);
            demanda[n2 - 1] = S - D;

            double[][] costos = new double[p.m()][n2];
            for (int i = 0; i < p.m(); i++) {
                System.arraycopy(p.costos[i], 0, costos[i], 0, p.n());
                costos[i][n2 - 1] = 0.0;
            }

            String[] cols = (p.columnas == null) ? null : Arrays.copyOf(p.columnas, n2);
            if (cols != null) cols[n2 - 1] = "Dummy";

            return new ProblemaTransporte(costos, p.oferta, demanda, p.filas, cols);
        } else {
            int m2 = p.m() + 1;
            double[] oferta = Arrays.copyOf(p.oferta, m2);
            oferta[m2 - 1] = D - S;

            double[][] costos = new double[m2][p.n()];
            for (int i = 0; i < p.m(); i++) costos[i] = Arrays.copyOf(p.costos[i], p.n());
            Arrays.fill(costos[m2 - 1], 0.0);

            String[] filas = (p.filas == null) ? null : Arrays.copyOf(p.filas, m2);
            if (filas != null) filas[m2 - 1] = "Dummy";

            return new ProblemaTransporte(costos, oferta, p.demanda, filas, p.columnas);
        }
        }}
        
    
