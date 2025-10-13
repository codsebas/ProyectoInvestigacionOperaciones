/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.logica;

import com.umg.sources.modelo.ProblemaTransporte;
import java.util.Arrays;

/**
 *
 * @author keyor
 */
public class MetodoCostoMinimo {
    public static String resolver(ProblemaTransporte p) {
        int m = p.m(), n = p.n();
        double[][] x = new double[m][n]; 

        double[] of = Arrays.copyOf(p.oferta, m);
        double[] de = Arrays.copyOf(p.demanda, n);

    
        while (true) {
            double minCosto = Double.MAX_VALUE;
            int bestI = -1, bestJ = -1;

            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (of[i] > 0 && de[j] > 0) {
                        if (p.costos[i][j] < minCosto) {
                            minCosto = p.costos[i][j];
                            bestI = i;
                            bestJ = j;
                        }
                    }
                }
            }

            if (bestI == -1) break; 

            double asigna = Math.min(of[bestI], de[bestJ]);
            x[bestI][bestJ] = asigna;

            of[bestI] -= asigna;
            de[bestJ] -= asigna;
          
        }

        double costo = 0.0;
        StringBuilder detalle = new StringBuilder();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (x[i][j] > 0) 
                costo += x[i][j] * p.costos[i][j];
            }
        }

        detalle.append(String.format("Metodo Costo Minimo: Costo total = %.4f", costo));
        return detalle.toString();
    }
    
}
