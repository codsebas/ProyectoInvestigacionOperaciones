/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.modelo;

import com.umg.sources.logica.MetodoVogel;
/**
 *
 * @author didhy
 */
public class ModeloVogel {
    
    private ProblemaTransporte problema;
    private MetodoVogel.Resultado resultado;
    private String resumen;

    public ProblemaTransporte getProblema() { return problema; }
    public void setProblema(ProblemaTransporte problema) { this.problema = problema; }

    public MetodoVogel.Resultado getResultado() { return resultado; }
    public void setResultado(MetodoVogel.Resultado resultado) { this.resultado = resultado; }

    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
}

