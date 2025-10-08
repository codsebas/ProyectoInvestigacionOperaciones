/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.modelo;
import com.umg.sources.vistas.VistaMetodosTransporte;
/**
 *
 * @author sebas
 */
public class ModeloMetodosTransporte {
    VistaMetodosTransporte vista;

    public ModeloMetodosTransporte(VistaMetodosTransporte vista) {
        this.vista = vista;
    }

    public ModeloMetodosTransporte() {
    }

    public VistaMetodosTransporte getVista() {
        return vista;
    }

    public void setVista(VistaMetodosTransporte vista) {
        this.vista = vista;
    }
    
    
}
