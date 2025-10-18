package com.umg.sources.logica;

import com.umg.sources.modelo.ProblemaTransporte;
import java.util.Arrays;
import java.util.Locale;

public class MetodoEsquinaNoroeste {

    public static String resolver(ProblemaTransporte p0) {
        ProblemaTransporte p = TransporteUtils.balancearSiHaceFalta(p0);

        int m = p.m(), n = p.n();
        double[][] x = new double[m][n];

        double[] of = Arrays.copyOf(p.oferta, m);
        double[] de = Arrays.copyOf(p.demanda, n);

        int i = 0, j = 0;
        while (i < m && j < n) {
            double asigna = Math.min(of[i], de[j]);
            x[i][j] += asigna;
            of[i] -= asigna;
            de[j] -= asigna;

            boolean filaCero = Math.abs(of[i]) < TransporteUtils.EPS;
            boolean colCero  = Math.abs(de[j]) < TransporteUtils.EPS;

            if (filaCero && colCero) { i++; }    
            else if (filaCero)        { i++; }
            else                      { j++; }
        }

        double costo = TransporteUtils.costoTotal(x, p);
        return String.format(Locale.ROOT, "Esquina Noroeste: costo total = %.4f", costo);
    }
}
