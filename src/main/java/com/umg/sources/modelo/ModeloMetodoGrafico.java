package com.umg.sources.modelo;
import com.umg.sources.vistas.VistaMetodoGrafico;

public class ModeloMetodoGrafico {
    VistaMetodoGrafico vista;

    public ModeloMetodoGrafico(VistaMetodoGrafico vista) {
        this.vista = vista;
    }

    public VistaMetodoGrafico getVista() {
        return vista;
    }

    public void setVista(VistaMetodoGrafico vista) {
        this.vista = vista;
    }
}
