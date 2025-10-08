package com.umg.sources.logica;

import com.umg.sources.modelo.ProblemaTransporte;
import java.util.Arrays;

public class MetodoEsquinaNoroeste {

    /**
     * Resuelve por Esquina Noroeste y retorna un resumen String (p.ej. costo total).
     */
    public static String resolver(ProblemaTransporte p) {
        int m = p.m(), n = p.n();
        double[][] x = new double[m][n]; // asignaciones

        double[] of = Arrays.copyOf(p.oferta, m);
        double[] de = Arrays.copyOf(p.demanda, n);

        int i = 0, j = 0;
        while (i < m && j < n) {
            double asigna = Math.min(of[i], de[j]);
            x[i][j] = asigna;
            of[i] -= asigna;
            de[j] -= asigna;
            if (of[i] == 0 && de[j] == 0) {
                // Convención: si ambos quedan 0, avanza fila (también válido avanzar columna, define tu regla fija)
                i++;
            } else if (of[i] == 0) {
                i++;
            } else { // de[j] == 0
                j++;
            }
        }

        // costo total
        double costo = 0.0;
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                costo += x[r][c] * p.costos[r][c];
            }
        }

        // Resumen corto. Puedes enriquecerlo con el detalle de asignaciones si gustas.
        return String.format("Esquina Noroeste: costo total = %.4f", costo);
    }
}
