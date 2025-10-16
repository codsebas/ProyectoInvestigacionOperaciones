/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.controlador;

import com.umg.sources.logica.LogicaModeloAsignacion;
import com.umg.sources.modelo.ModeloAsignacion;
import com.umg.sources.modelo.ProblemaAsignacion;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;

/**
 *
 * @author sebas
 */
public class ControladorModeloAsignacion implements ActionListener, MouseListener, WindowListener {

    private final ModeloAsignacion modelo;

    public ControladorModeloAsignacion(ModeloAsignacion modelo) {
        this.modelo = modelo;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Object src = e.getSource();

        if (src == modelo.getVista().BtnGenTabla) {
            String sFilas = modelo.getVista().TxtFilas.getText().trim();
            String sCols = modelo.getVista().TxtColumnas.getText().trim();
            if (sFilas.isEmpty() || sCols.isEmpty()) {
                /* mensaje y return */ }
            int nFilas = Integer.parseInt(sFilas);    // suministros (A,B,C,...)
            int nDemandas = Integer.parseInt(sCols);  // D1..Dn

            if (nFilas <= 0 || nDemandas <= 0) {
                /* mensaje y return */ }

            generarTablaAsignacion(nFilas, nDemandas);

        } else if (src == modelo.getVista().BtnLimpiar) {
            // Limpia tabla
            modelo.getVista().tblDatos.setModel(new DefaultTableModel());
            modelo.getVista().TxtColumnas.setText("");
            modelo.getVista().TxtFilas.setText("");
            modelo.getVista().txtResultado.setText("Resultado");

        } else if (src == modelo.getVista().BtnGenerar) {
            if (tablaOk()) {

                ProblemaAsignacion p = getProblemaFromUI();
                String res = "";
                res = LogicaModeloAsignacion.resolver(p);
                modelo.getVista().txtResultado.setText("Resultado: " + res);

            } else {

            }
        } else if (src == modelo.getVista().BtnRegresar) {

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

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    private void generarTablaAsignacion(int nFilas, int nCols) {
        // Encabezados: ["", A, B, C, ...]
        String[] colNames = new String[1 + nCols];
        colNames[0] = ""; // etiqueta de filas
        for (int c = 1; c <= nCols; c++) {
            colNames[c] = String.valueOf((char) ('A' + (c - 1))); // A, B, C, ...
        }

        Object[][] data = new Object[nFilas][colNames.length];
        for (int r = 0; r < nFilas; r++) {
            data[r][0] = String.valueOf(r + 1); // 1, 2, 3, ...
        }

        DefaultTableModel modeloTabla = new DefaultTableModel(data, colNames) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != 0; // solo editar costos
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
            tabla.getColumnModel().getColumn(0).setMinWidth(60);
            tabla.getColumnModel().getColumn(0).setMaxWidth(80);
            tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
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
        if (rows < 1 || cols < 2) {
            JOptionPane.showMessageDialog(modelo.getVista(),
                    "Define al menos 1 fila y 1 columna.", "Datos insuficientes",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        // validar celdas de costos: filas 0..rows-1, columnas 1..cols-1
        for (int r = 0; r < rows; r++) {
            for (int c = 1; c < cols; c++) {
                Object v = tbl.getValueAt(r, c);
                if (!esNumero(v)) {
                    JOptionPane.showMessageDialog(modelo.getVista(),
                            String.format("Costo inválido en fila %d, columna %d.", r + 1, c),
                            "Dato inválido", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                double val = Double.parseDouble(v.toString());
                if (val < 0) {
                    JOptionPane.showMessageDialog(modelo.getVista(),
                            "Los costos deben ser >= 0.", "Dato inválido", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Extrae costos, oferta y demanda desde la JTable respetando tu layout.
     */
    private ProblemaAsignacion getProblemaFromUI() {
        JTable tbl = modelo.getVista().tblDatos;
        int m = tbl.getRowCount();
        int n = tbl.getColumnCount() - 1; // quitamos la col de etiquetas

        double[][] costos = new double[m][n];
        double[] oferta = new double[m];
        double[] demanda = new double[n];

        String[] filas = new String[m];
        String[] columnas = new String[n];

        // columnas A.. (nombres de JTable)
        for (int c = 0; c < n; c++) {
            columnas[c] = tbl.getColumnName(c + 1);
        }
        // filas 1..m y costos
        for (int r = 0; r < m; r++) {
            filas[r] = String.valueOf(tbl.getValueAt(r, 0)); // "1", "2", ...
            for (int c = 0; c < n; c++) {
                costos[r][c] = Double.parseDouble(tbl.getValueAt(r, c + 1).toString());
            }
            oferta[r] = 1.0; // asignación 1–a–1
        }
        for (int c = 0; c < n; c++) {
            demanda[c] = 1.0;
        }

        return new ProblemaAsignacion(costos, oferta, demanda, filas, columnas);
    }

}
