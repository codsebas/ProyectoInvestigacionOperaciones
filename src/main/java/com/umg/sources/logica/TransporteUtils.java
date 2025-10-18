package com.umg.sources.logica;

import com.umg.sources.modelo.ProblemaTransporte;
import java.util.Arrays;

public final class TransporteUtils {
    private TransporteUtils() {}
    public static final double EPS = 1e-9;

 
    public static ProblemaTransporte balancearSiHaceFalta(ProblemaTransporte p) {
        double S = 0, D = 0;
        for (double v : p.oferta)  S += v;
        for (double v : p.demanda) D += v;
        if (Math.abs(S - D) < EPS) return p;

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
    }


    public static double costoTotal(double[][] x, ProblemaTransporte p) {
        double total = 0.0;
        for (int i = 0; i < p.m(); i++)
            for (int j = 0; j < p.n(); j++)
                total += x[i][j] * p.costos[i][j];
        return total;
    }
}
