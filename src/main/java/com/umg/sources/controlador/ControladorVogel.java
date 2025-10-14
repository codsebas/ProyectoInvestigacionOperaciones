/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.controlador;

import com.umg.sources.logica.MetodoVogel;
import com.umg.sources.modelo.ModeloVogel;
import com.umg.sources.modelo.ProblemaTransporte;

import java.util.Arrays;

/**
 *
 * @author didhy
 */
public class ControladorVogel {
    
        private final ModeloVogel modelo;

    public ControladorVogel(final ModeloVogel modelo) {
        this.modelo = modelo;
    }

    public void ejecutar(final ProblemaTransporte p) {
        modelo.setProblema(p);
        final MetodoVogel.Resultado r = MetodoVogel.calcular(p);
        modelo.setResultado(r);
        modelo.setResumen(String.format("Vogel: costo total = %.4f", r.costoTotal));
    }

    // Demo independiente (puedes borrarlo si ya lo llamas desde tu UI)
    public static void main(String[] args) {
        double[][] costos = {
            {2, 3, 1},
            {5, 4, 8},
            {5, 6, 8}
        };
        double[] oferta  = {70, 60, 25};
        double[] demanda = {60, 40, 55};
        String[] filas   = {"A", "B", "C"};
        String[] columnas= {"D1", "D2", "D3"};

        ProblemaTransporte p = new ProblemaTransporte(costos, oferta, demanda, filas, columnas);
        ModeloVogel m = new ModeloVogel();
        ControladorVogel c = new ControladorVogel(m);
        c.ejecutar(p);

        System.out.println(m.getResumen());
        for (double[] row : m.getResultado().asignacion) {
            System.out.println(Arrays.toString(row));
        }
    }
}