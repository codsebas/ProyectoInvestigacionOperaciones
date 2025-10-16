/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.modelo;
import com.umg.sources.vistas.VistaModeloAsignacion;
/**
 *
 * @author sebas
 */
public class ModeloAsignacion {
    VistaModeloAsignacion vista;

    public ModeloAsignacion(VistaModeloAsignacion vista) {
        this.vista = vista;
    }

    public ModeloAsignacion() {
    }

    public VistaModeloAsignacion getVista() {
        return vista;
    }

    public void setVista(VistaModeloAsignacion vista) {
        this.vista = vista;
    }
    
    
}
