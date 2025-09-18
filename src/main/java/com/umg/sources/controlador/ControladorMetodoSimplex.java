package com.umg.sources.controlador;

import com.umg.sources.logica.LPSolver2D;
import com.umg.sources.logica.LogicaSimplex;
import com.umg.sources.logica.NaturalParser;
import com.umg.sources.modelo.ModeloMetodoSimplex;
import com.umg.sources.vistas.VistaMetodoSimplex;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ControladorMetodoSimplex implements ActionListener, MouseListener {

    private final ModeloMetodoSimplex modelo;

    public ControladorMetodoSimplex(ModeloMetodoSimplex modelo) {
        this.modelo = modelo;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // no-op (combo Max/Min)
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        VistaMetodoSimplex v = modelo.getVista();
        Object src = e.getComponent();

        if (src == v.BtnMenu) {
            System.out.println("Menu");
            return;
        }

        if (src == v.BtnLimpiar) {
            v.TxtZ.setText("");
            v.TxtRestriccion1.setText("");
            v.TxtRestriccion2.setText("");
            v.TxtRestriccion3.setText("");
            v.TxtRestriccion4.setText("");
            v.TxtResultado.setText("");

            // Limpia tabla y gráfica
            v.mostrarIteraciones(new ArrayList<com.umg.sources.logica.LogicaSimplex.Iteration>());
            v.dibujar(new double[][]{{1, 0}, {0, 1}}, new double[]{0, 0}, new char[]{'>', '>'}, null, null);
            return;
        }

        if (src == v.BtnGenerar) {
            try {
                // 1) Objetivo (ej: "3x + 5y")
                double[] c = NaturalParser.parseObjective(v.TxtZ.getText());
                double c1 = c[0], c2 = c[1];

                // 2) Restricciones (hasta 4)
                List<LPSolver2D.Constraint> cons = new ArrayList<LPSolver2D.Constraint>();
                String[] raws = new String[]{
                        v.TxtRestriccion1.getText(),
                        v.TxtRestriccion2.getText(),
                        v.TxtRestriccion3.getText(),
                        v.TxtRestriccion4.getText()
                };

                for (int i = 0; i < raws.length; i++) {
                    String r = raws[i];
                    if (r == null || r.trim().isEmpty()) continue;
                    // Ojo: el tipo concreto devuelto por NaturalParser.parseConstraint debe existir.
                    // En tu proyecto lo usas como 'p.a, p.b, p.c, p.type'.
                    // Si el nombre es diferente, cámbialo aquí.
                    NaturalParser.ParsedConstraint p = NaturalParser.parseConstraint(r);
                    cons.add(new LPSolver2D.Constraint(p.a, p.b, p.c, p.type));
                }

                if (cons.isEmpty()) {
                    JOptionPane.showMessageDialog(v, "Ingresa al menos una restricción.",
                            "Faltan datos", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 3) Tipo: Max/Min
                boolean maximize = true; // por defecto max
                Object sel = v.CmbOpciones.getSelectedItem();
                if (sel != null) {
                    String s = sel.toString().toLowerCase(Locale.ROOT);
                    maximize = s.contains("max");
                }

                // 4) Resolver con Simplex (Fase I + II)
                LogicaSimplex.Result res = LogicaSimplex.solveFrom2D(cons, c1, c2, maximize);

                // 5) Tabla de iteraciones
                v.mostrarIteraciones(res.steps);

                // 6) Resultado
                if (!res.feasible) {
                    v.TxtResultado.setText("Infactible");
                } else if (res.unbounded) {
                    v.TxtResultado.setText("No acotado");
                } else {
                    double x1 = (res.x != null && res.x.length > 0) ? res.x[0] : 0.0;
                    double x2 = (res.x != null && res.x.length > 1) ? res.x[1] : 0.0;
                    v.TxtResultado.setText(String.format(Locale.US,
                            "Óptimo: Z=%.4f en (x=%.4f, y=%.4f)", res.z, x1, x2));
                }

                // 7) Graficar (2D)
                int m = cons.size();
                double[][] A = new double[m][2];
                double[] b = new double[m];
                char[] signs = new char[m];

                for (int i = 0; i < m; i++) {
                    LPSolver2D.Constraint ci = cons.get(i);
                    A[i][0] = ci.a;
                    A[i][1] = ci.b;
                    b[i] = ci.c;

                    char sign;
                    switch (ci.type) {
                        case LE:
                            sign = '<';
                            break;
                        case GE:
                            sign = '>';
                            break;
                        case EQ:
                        default:
                            sign = '=';
                            break;
                    }
                    signs[i] = sign;
                }

                double[] cObj = (!res.feasible || res.unbounded) ? null : new double[]{c1, c2};
                double[] xopt = (res.feasible && !res.unbounded) ? res.x : null;
                v.dibujar(A, b, signs, cObj, xopt);

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(v,
                        "Objetivo inválido. Escribe, ej.:  3x + 5y",
                        "Error de entrada", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(v, "Error inesperado: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
}
