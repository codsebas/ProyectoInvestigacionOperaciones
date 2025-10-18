package com.umg.sources.logica;

import com.umg.sources.modelo.ProblemaAsignacion;
import java.util.Arrays;

public class LogicaModeloAsignacion {

    private static final double INF = 1e12;  
    private static final double DUMMY_COST = 0.0; 

   
    public static String resolver(ProblemaAsignacion p) {
        // -------- Validaciones --------
        if (p == null || p.costos == null || p.costos.length == 0 || p.costos[0].length == 0) {
            return "Asignación: datos de entrada vacíos o nulos.";
        }
        int m = p.costos.length;
        int n = p.costos[0].length;
        for (int i = 1; i < m; i++) {
            if (p.costos[i].length != n) {
                return "Asignación: la matriz de costos no es rectangular.";
            }
        }

        // -------- Copia y saneo --------
        double[][] costos = copiar(p.costos);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (!Double.isFinite(costos[i][j]) || costos[i][j] >= INF / 2) {
                    costos[i][j] = INF; 
                }
            }
        }

        // -------- Cuadrar si hace falta --------
        int k = Math.max(m, n);
        double[][] c = new double[k][k];
        for (int i = 0; i < k; i++) Arrays.fill(c[i], DUMMY_COST);
        for (int i = 0; i < m; i++) System.arraycopy(costos[i], 0, c[i], 0, n);

      
        int[] asign = hungarianMin(c); 

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m; i++) {
            int j = asign[i];
            if (j < 0 || j >= n) continue; 

            String fila = (p.filas != null && i < p.filas.length && p.filas[i] != null && !p.filas[i].isEmpty())
                    ? p.filas[i]
                    : String.valueOf(i + 1);

            String col = (p.columnas != null && j < p.columnas.length && p.columnas[j] != null && !p.columnas[j].isEmpty())
                    ? p.columnas[j]
                    : String.valueOf((char) ('A' + j));

            double cost = p.costos[i][j];
            String costStr = (Math.rint(cost) == cost) ? String.valueOf((long) cost) : String.format("%.4f", cost);

            if (sb.length() > 0) sb.append(", ");
            sb.append(fila).append("-").append(col).append(" = ").append(costStr);
        }
        return sb.toString();
    }

    // ===== Utilidades =====
    private static double[][] copiar(double[][] a) {
        double[][] b = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) b[i] = Arrays.copyOf(a[i], a[i].length);
        return b;
    }

    
    private static int[] hungarianMin(double[][] a) {
        int n = a.length;
        for (double[] fila : a) {
            if (fila.length != n) throw new IllegalArgumentException("La matriz debe ser cuadrada.");
        }

        double[][] cost = copiar(a);

     
        for (int i = 0; i < n; i++) {
            double min = Arrays.stream(cost[i]).min().orElse(0.0);
            if (!Double.isFinite(min)) min = 0.0;
            for (int j = 0; j < n; j++) cost[i][j] -= min;
        }
       
        for (int j = 0; j < n; j++) {
            double min = Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++) min = Math.min(min, cost[i][j]);
            if (!Double.isFinite(min)) min = 0.0;
            for (int i = 0; i < n; i++) cost[i][j] -= min;
        }

        int[] u = new int[n + 1], v = new int[n + 1], p = new int[n + 1], way = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minv = new double[n + 1];
            boolean[] used = new boolean[n + 1];
            Arrays.fill(minv, Double.POSITIVE_INFINITY);
            Arrays.fill(used, false);

            do {
                used[j0] = true;
                int i0 = p[j0], j1 = 0;
                double delta = Double.POSITIVE_INFINITY;

                for (int j = 1; j <= n; j++) {
                    if (used[j]) continue;
                    double cur = cost[i0 - 1][j - 1] - u[i0] - v[j];
                    if (cur < minv[j]) { minv[j] = cur; way[j] = j0; }
                    if (minv[j] < delta) { delta = minv[j]; j1 = j; }
                }
                for (int j = 0; j <= n; j++) {
                    if (used[j]) { u[p[j]] += delta; v[j] -= delta; }
                    else { minv[j] -= delta; }
                }
                j0 = j1;
            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] match = new int[n];
        for (int j = 1; j <= n; j++) if (p[j] > 0) match[p[j] - 1] = j - 1;
        return match;
    }
}
