/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.controlador;
import com.umg.sources.modelo.ModeloMenu;
import com.umg.sources.vistas.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;
/**
 *
 * @author sebas
 */
public class ControladorMenu implements ActionListener, MouseListener{
    
    ModeloMenu modelo;
    VistaMenu vista;
    VistaPrincipal vistaPrincipal;

    public ControladorMenu(ModeloMenu modelo, VistaMenu vista, VistaPrincipal vistaPrincipal) {
        this.modelo = modelo;
        this.vista = vista;
        this.vistaPrincipal = vistaPrincipal;
        
        vista.getBtnMetGrafico().addMouseListener(this);
        vista.getBtnMetSimplex().addMouseListener(this);
    }
    
    private void cambiarVista(JPanel panel) {
        panel.setSize(1230, 720);
        panel.setLocation(0, 0);
        vista.contenedor.removeAll();
        vista.contenedor.add(panel, BorderLayout.CENTER);
        vista.contenedor.revalidate();
        vista.contenedor.repaint();
    }
    
    
    @Override
    public void actionPerformed(ActionEvent ae) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getComponent().equals(vista.BtnMetGrafico)){
            VistaMetodoGrafico panel = new VistaMetodoGrafico();
            vistaPrincipal.cambiarPanel(panel);
        } else if(e.getComponent().equals(vista.BtnMetSimplex)){
            VistaMetodoSimplex panel = new VistaMetodoSimplex();
            vistaPrincipal.cambiarPanel(panel);
        }
        
    }

    @Override
    public void mousePressed(MouseEvent me) {
    }

    @Override
    public void mouseReleased(MouseEvent me) {
    }

    @Override
    public void mouseEntered(MouseEvent me) {
    }

    @Override
    public void mouseExited(MouseEvent me) {
    }
    
}
