/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.modelo;

/**
 *
 * @author sebas
 */
public class ProblemaAsignacion {
    public final double[][] costos; 
    public final double[] oferta;   
    public final double[] demanda;  
    public final String[] filas;  
    public final String[] columnas; 

    public ProblemaAsignacion(double[][] costos, double[] oferta, double[] demanda,
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
