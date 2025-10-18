package com.umg.sources.logica;

import com.umg.sources.modelo.ProblemaTransporte;
import java.util.Arrays;
import java.util.Locale;

public class MetodoCostoMinimo {

    public static String resolver(ProblemaTransporte p0) {
        ProblemaTransporte p = TransporteUtils.balancearSiHaceFalta(p0);

        int m = p.m(), n = p.n();
        double[][] x = new double[m][n];

        double[] of = Arrays.copyOf(p.oferta, m);
        double[] de = Arrays.copyOf(p.demanda, n);

        while (true) {
            double minCosto = Double.POSITIVE_INFINITY;
            int bestI = -1, bestJ = -1;

            for (int i = 0; i < m; i++) if (of[i] > TransporteUtils.EPS) {
                for (int j = 0; j < n; j++) if (de[j] > TransporteUtils.EPS) {
                    if (p.costos[i][j] < minCosto) {
                        minCosto = p.costos[i][j];
                        bestI = i; bestJ = j;
                    }
                }
            }

            if (bestI == -1) break; 

            double q = Math.min(of[bestI], de[bestJ]);
            x[bestI][bestJ] += q;
            of[bestI] -= q;
            de[bestJ] -= q;
        }

        double costo = TransporteUtils.costoTotal(x, p);
        return String.format(Locale.ROOT, "Metodo Costo Minimo: costo total = %.4f", costo);
    }
}
