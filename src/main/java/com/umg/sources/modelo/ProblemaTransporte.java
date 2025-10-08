/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.modelo;

/**
 *
 * @author sebas
 */
public class ProblemaTransporte {
    public final double[][] costos; // m x n (sin etiquetas)
    public final double[] oferta;   // m
    public final double[] demanda;  // n
    public final String[] filas;    // etiquetas A.. (opcional)
    public final String[] columnas; // D1..Dn (opcional)

    public ProblemaTransporte(double[][] costos, double[] oferta, double[] demanda,
                              String[] filas, String[] columnas) {
        this.costos = costos;
        this.oferta = oferta;
        this.demanda = demanda;
        this.filas = filas;
        this.columnas = columnas;
    }

    public int m() { return oferta.length; }
    public int n() { return demanda.length; }
}
