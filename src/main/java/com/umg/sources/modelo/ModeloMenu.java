/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.modelo;
import com.umg.sources.vistas.VistaMenu;
/**
 *
 * @author sebas
 */
public class ModeloMenu {
    VistaMenu vista;

    public ModeloMenu() {
    }

    public ModeloMenu(VistaMenu vista) {
        this.vista = vista;
    }

    public VistaMenu getVista() {
        return vista;
    }

    public void setVista(VistaMenu vista) {
        this.vista = vista;
    }
    
    
}
