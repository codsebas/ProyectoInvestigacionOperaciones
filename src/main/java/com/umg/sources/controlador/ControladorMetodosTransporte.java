/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.controlador;

import com.umg.sources.logica.MetodoVogel;
import com.umg.sources.modelo.ModeloMetodosTransporte;
import com.umg.sources.logica.MetodoEsquinaNoroeste;
import com.umg.sources.logica.MetodoCostoMinimo;
import com.umg.sources.modelo.ProblemaTransporte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;

// ✅ imports nuevos para navegación y layout
import com.umg.sources.vistas.VistaMenu;
import org.netbeans.lib.awtextra.AbsoluteLayout;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import javax.swing.*;
import java.awt.*;

public class ControladorMetodosTransporte implements ActionListener, MouseListener, WindowListener {

    private final ModeloMetodosTransporte modelo;

    // ✅ referencia al menú para navegar
    private VistaMenu vistaMenu;

    public ControladorMetodosTransporte(ModeloMetodosTransporte modelo) {
        this.modelo = modelo;
    }

    @Override
    public void actionPerformed(ActionEvent e) { }

    @Override
    public void mouseClicked(MouseEvent e) {
        Object src = e.getSource();

        if (src == modelo.getVista().BtnGenTabla) {
            String sFilas = modelo.getVista().TxtFilas.getText().trim();
            String sCols = modelo.getVista().TxtColumnas.getText().trim();
            if (sFilas.isEmpty() || sCols.isEmpty()) { }
            int nFilas = Integer.parseInt(sFilas);
            int nDemandas = Integer.parseInt(sCols);
            if (nFilas <= 0 || nDemandas <= 0) { }

            generarTablaTransporte(nFilas, nDemandas);

        } else if (src == modelo.getVista().BtnLimpiar) {
            // Limpia tabla
            modelo.getVista().tblDatos.setModel(new DefaultTableModel());
            modelo.getVista().TxtColumnas.setText("");
            modelo.getVista().TxtFilas.setText("");
            modelo.getVista().txtResultado.setText("Resultado");

        } else if (src == modelo.getVista().BtnGenerar) {
            if (tablaOk()) {
                String sel = (String) modelo.getVista().CBBMetodos.getSelectedItem();
                ProblemaTransporte p = getProblemaFromUI();
                String res = "";
                switch (sel) {
                    case "Costo Minimo":
                        res = MetodoCostoMinimo.resolver(p);
                        break;
                    case "Esquina Noroeste":
                        res = MetodoEsquinaNoroeste.resolver(p);
                        break;
                    case "Vogel":
                        res = MetodoVogel.resolver(p);
                }
                modelo.getVista().txtResultado.setText("Resultado: " + res);
            } else {
                // datos inválidos, ya se mostró mensaje
            }

        } else if (src == modelo.getVista().BtnRegresar) {
            // ✅ Navegar a VistaMenu (robusto: contenedor / JFrame / JPanel)
            if (vistaMenu == null) vistaMenu = new VistaMenu();
            cambiarVista(vistaMenu);
            return;
        }
    }

    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
    @Override public void windowOpened(WindowEvent e) { }
    @Override public void windowClosing(WindowEvent e) { }
    @Override public void windowClosed(WindowEvent e) { }
    @Override public void windowIconified(WindowEvent e) { }
    @Override public void windowDeiconified(WindowEvent e) { }
    @Override public void windowActivated(WindowEvent e) { }
    @Override public void windowDeactivated(WindowEvent e) { }

    // ─────────────────────────────────────────────────────────
    // 🔧 Cambiar vista compatible con:
    //    - JPanel público "contenedor" en la vista
    //    - JFrame/JDialog (getContentPane)
    //    - La propia vista si es JPanel
    // ─────────────────────────────────────────────────────────
    private void cambiarVista(JPanel panel) {
        Container cont = obtenerContenedorVista();

        if (!(cont.getLayout() instanceof AbsoluteLayout)) {
            cont.setLayout(new AbsoluteLayout());
        }
        cont.removeAll();

        int w = cont.getWidth() > 0 ? cont.getWidth() : 1230;
        int h = cont.getHeight() > 0 ? cont.getHeight() : 720;

        panel.setSize(w, h);
        panel.setPreferredSize(new Dimension(w, h));
        cont.add(panel, new AbsoluteConstraints(0, 0, w, h));

        cont.revalidate();
        cont.repaint();
    }

    private Container obtenerContenedorVista() {
        Object vista = modelo.getVista();
        // 1) Si la vista expone un JPanel público llamado "contenedor"
        try {
            java.lang.reflect.Field f = vista.getClass().getField("contenedor");
            Object pnl = f.get(vista);
            if (pnl instanceof Container) return (Container) pnl;
        } catch (NoSuchFieldException | IllegalAccessException ignore) { }
        // 2) Si es JFrame/JDialog
        if (vista instanceof JFrame)  return ((JFrame) vista).getContentPane();
        if (vista instanceof JDialog) return ((JDialog) vista).getContentPane();
        // 3) Si es un JPanel u otro Container
        if (vista instanceof Container) return (Container) vista;
        // 4) Fallback extremo
        return new JPanel(new AbsoluteLayout());
    }

    private void generarTablaTransporte(int nFilasSuministro, int nDemandas) {

        String[] colNames = new String[1 + nDemandas + 1];
        colNames[0] = "";
        for (int i = 1; i <= nDemandas; i++) {
            colNames[i] = "D" + i;
        }
        colNames[colNames.length - 1] = "Oferta";

        final int totalFilas = nFilasSuministro + 1;
        final int totalCols = colNames.length;

        Object[][] data = new Object[totalFilas][totalCols];

        for (int r = 0; r < nFilasSuministro; r++) {
            data[r][0] = String.valueOf((char) ('A' + r));
        }
        data[totalFilas - 1][0] = "Demanda";

        DefaultTableModel modeloTabla = new DefaultTableModel(data, colNames) {
            @Override
            public boolean isCellEditable(int row, int col) {
                if (col == 0) {
                    return false;
                }
                if (row == totalFilas - 1 && col == totalCols - 1) {
                    return false;
                }
                return true;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 0) ? String.class : Object.class;
            }
        };

        JTable tabla = modelo.getVista().tblDatos;
        tabla.setModel(modeloTabla);
        tabla.setRowHeight(24);

        tabla.setShowGrid(true);

        if (tabla.getColumnModel().getColumnCount() > 0) {
            tabla.getColumnModel().getColumn(0).setMinWidth(70);
            tabla.getColumnModel().getColumn(0).setMaxWidth(100);
            tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        }

    }

    private static boolean esNumero(Object o) {
        if (o == null) {
            return false;
        }
        try {
            Double.parseDouble(o.toString().trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean tablaOk() {
        JTable tbl = modelo.getVista().tblDatos;
        int rows = tbl.getRowCount();
        int cols = tbl.getColumnCount();

        if (rows < 2 || cols < 3) {
            JOptionPane.showMessageDialog(
                    modelo.getVista(),
                    "Define al menos 1 oferta, 1 demanda y costos.",
                    "Datos insuficientes",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        final int ultimaFila = rows - 1;
        final int colOferta  = cols - 1;

        for (int r = 0; r < ultimaFila; r++) {
            for (int c = 1; c < colOferta; c++) {
                Object v = tbl.getValueAt(r, c);
                if (!esNumero(v)) {
                    JOptionPane.showMessageDialog(
                            modelo.getVista(),
                            String.format("Costo inválido en fila %d, columna %d.", r + 1, c + 1),
                            "Dato inválido",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }
                if (Double.parseDouble(v.toString()) < 0) {
                    JOptionPane.showMessageDialog(
                            modelo.getVista(),
                            "Los costos deben ser >= 0.",
                            "Dato inválido",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }
            }
        }

        double sumaOferta = 0.0;
        for (int r = 0; r < ultimaFila; r++) {
            Object v = tbl.getValueAt(r, colOferta);
            if (!esNumero(v)) {
                JOptionPane.showMessageDialog(
                        modelo.getVista(),
                        String.format("Oferta inválida en la fila %d.", r + 1),
                        "Dato inválido",
                        JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
            double val = Double.parseDouble(v.toString());
            if (val < 0) {
                JOptionPane.showMessageDialog(
                        modelo.getVista(),
                        "Las ofertas deben ser >= 0.",
                        "Dato inválido",
                        JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
            sumaOferta += val;
        }

        double sumaDemanda = 0.0;
        for (int c = 1; c < colOferta; c++) {
            Object v = tbl.getValueAt(ultimaFila, c);
            if (!esNumero(v)) {
                JOptionPane.showMessageDialog(
                        modelo.getVista(),
                        String.format("Demanda inválida en la columna %d.", c + 1),
                        "Dato inválido",
                        JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
            double val = Double.parseDouble(v.toString());
            if (val < 0) {
                JOptionPane.showMessageDialog(
                        modelo.getVista(),
                        "Las demandas deben ser >= 0.",
                        "Dato inválido",
                        JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
            sumaDemanda += val;
        }

        final double EPS = 1e-6;
        if (Math.abs(sumaOferta - sumaDemanda) > EPS) {
            JOptionPane.showMessageDialog(
                    modelo.getVista(),
                    String.format(
                            "Problema desbalanceado: Oferta = %.4f, Demanda = %.4f.\n" +
                                    "Se añadirá una fila/columna Dummy (costo 0) automáticamente.",
                            sumaOferta, sumaDemanda),
                    "Aviso",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

        return true;
    }

    private ProblemaTransporte getProblemaFromUI() {
        JTable tbl = modelo.getVista().tblDatos;
        int rows = tbl.getRowCount();
        int cols = tbl.getColumnCount();

        int m = rows - 1;
        int n = cols - 2;

        double[][] costos = new double[m][n];
        double[] oferta = new double[m];
        double[] demanda = new double[n];

        String[] filas = new String[m];
        String[] columnas = new String[n];

        for (int c = 0; c < n; c++) {
            columnas[c] = tbl.getColumnName(c + 1);
        }

        for (int r = 0; r < m; r++) {
            filas[r] = String.valueOf(tbl.getValueAt(r, 0));
            for (int c = 0; c < n; c++) {
                costos[r][c] = Double.parseDouble(tbl.getValueAt(r, c + 1).toString());
            }
            oferta[r] = Double.parseDouble(tbl.getValueAt(r, cols - 1).toString());
        }

        for (int c = 0; c < n; c++) {
            demanda[c] = Double.parseDouble(tbl.getValueAt(rows - 1, c + 1).toString());
        }

        return new ProblemaTransporte(costos, oferta, demanda, filas, columnas);
    }
}