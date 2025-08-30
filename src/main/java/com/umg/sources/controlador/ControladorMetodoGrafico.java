package com.umg.sources.controlador;

import com.umg.sources.logica.LPSolver2D;
import com.umg.sources.logica.NaturalParser;
import com.umg.sources.vistas.*;
import com.umg.sources.modelo.ModeloMetodoGrafico;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class ControladorMetodoGrafico implements ActionListener, MouseListener {

    ModeloMetodoGrafico modelo;
    VistaMetodoGrafico vista;
    VistaMenu vistaMenu;

    public ControladorMetodoGrafico(ModeloMetodoGrafico modelo) {
        this.modelo = modelo;
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
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals(modelo.getVista().CmbOpciones.getActionCommand())){
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        var v = modelo.getVista(); // atajo

        if (e.getComponent().equals(v.BtnMenu)) {
            System.out.println("Menu");
            return;
        }

        if (e.getComponent().equals(v.BtnLimpiar)) {
            v.TxtZ.setText("");
            v.TxtRestriccion1.setText("");
            v.TxtRestriccion2.setText("");
            v.TxtRestriccion3.setText("");
            v.TxtRestriccion4.setText("");
            v.TxtResultado.setText("");
            return;
        }

        if (e.getComponent().equals(v.BtnGenerar)) {
            try {
                // 1) Objetivo: ejemplo "2x + 3y"

                System.out.println("Z RAW=[" + v.TxtZ.getText() + "]");
                for (char ch : v.TxtZ.getText().toCharArray()) {
                    System.out.print((int) ch + " ");
                }
                System.out.println();
                double[] c = NaturalParser.parseObjective(v.TxtZ.getText());
                double c1 = c[0], c2 = c[1];

                // 2) Restricciones (hasta 4). Ignora vacías. Valida formato.
                List<LPSolver2D.Constraint> cons = new ArrayList<>();
                String[] raws = new String[]{
                        v.TxtRestriccion1.getText(),
                        v.TxtRestriccion2.getText(),
                        v.TxtRestriccion3.getText(),
                        v.TxtRestriccion4.getText()
                };
                for (int i=0;i<raws.length;i++){
                    String r = raws[i];
                    if (r == null || r.isBlank()) continue;
                    try {
                        var p = NaturalParser.parseConstraint(r);
                        cons.add(new LPSolver2D.Constraint(p.a, p.b, p.c, p.type));
                    } catch (IllegalArgumentException exR){
                        // marca error puntual y aborta
                        JOptionPane.showMessageDialog(v,
                                "Restricción " + (i+1) + " inválida:\n" + exR.getMessage(),
                                "Error de entrada", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                if (cons.isEmpty()){
                    JOptionPane.showMessageDialog(v, "Ingresa al menos una restricción.",
                            "Faltan datos", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 3) Max/Min desde el combo
                boolean maximize = v.CmbOpciones.getSelectedItem().toString()
                        .toLowerCase(Locale.ROOT).contains("max");

                // 4) Resolver
                var res = LPSolver2D.solve(cons, c1, c2, maximize);

                // 5) Mostrar resultado en TxtResultado
                if (!res.feasible) {
                    v.TxtResultado.setText("Infactible");
                } else if (res.unbounded) {
                    v.TxtResultado.setText("No acotado");
                } else {
                    v.TxtResultado.setText(
                            String.format(Locale.US, "x=%.4f, y=%.4f, Z=%.4f", res.x, res.y, res.z)
                    );
                }

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(v,
                        "Objetivo inválido. Escribe, ej.:  2x + 3y",
                        "Error de entrada", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(v, "Error inesperado: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
