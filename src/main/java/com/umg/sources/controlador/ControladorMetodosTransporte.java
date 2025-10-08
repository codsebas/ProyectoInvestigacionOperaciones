/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.controlador;

import com.umg.sources.modelo.ModeloMetodosTransporte;
import com.umg.sources.logica.MetodoEsquinaNoroeste;
import com.umg.sources.modelo.ProblemaTransporte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;

/**
 *
 * @author sebas
 */
public class ControladorMetodosTransporte implements ActionListener, MouseListener, WindowListener {

    private final ModeloMetodosTransporte modelo;

    public ControladorMetodosTransporte(ModeloMetodosTransporte modelo) {
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
                        modelo.getVista().txtResultado.setText(sel);
                        break;
                    case "Esquina Noroeste":
                        res = MetodoEsquinaNoroeste.resolver(p);
                        break;
                    case "Vogel":
                        modelo.getVista().txtResultado.setText(sel);
                        break;
                }
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

    private void generarTablaTransporte(int nFilasSuministro, int nDemandas) {
        // Construir encabezados: ["", D1..Dn, "Oferta"]
        String[] colNames = new String[1 + nDemandas + 1];
        colNames[0] = ""; // columna de etiquetas de filas
        for (int i = 1; i <= nDemandas; i++) {
            colNames[i] = "D" + i;
        }
        colNames[colNames.length - 1] = "Oferta";

        final int totalFilas = nFilasSuministro + 1; // +1 por fila "Demanda"
        final int totalCols = colNames.length;

        Object[][] data = new Object[totalFilas][totalCols];

        for (int r = 0; r < nFilasSuministro; r++) {
            data[r][0] = String.valueOf((char) ('A' + r)); // A, B, C...
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
            JOptionPane.showMessageDialog(modelo.getVista(),
                    "Define al menos 1 oferta, 1 demanda y costos.", "Datos insuficientes",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        int ultimaFila = rows - 1;     // fila "Demanda"
        int colOferta = cols - 1;     // última columna "Oferta"

        // 1) Validar costos (todas las celdas internas, excepto fila Demanda y col Oferta)
        for (int r = 0; r < ultimaFila; r++) { // filas de oferta
            for (int c = 1; c < colOferta; c++) {
                Object v = tbl.getValueAt(r, c);
                if (!esNumero(v)) {
                    JOptionPane.showMessageDialog(modelo.getVista(),
                            String.format("Costo inválido en fila %d, columna %d.", r + 1, c + 1),
                            "Dato inválido", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (Double.parseDouble(v.toString()) < 0) {
                    JOptionPane.showMessageDialog(modelo.getVista(),
                            "Los costos deben ser >= 0.", "Dato inválido", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }

        // 2) Validar OFERTAS (columna final, exceptuando fila Demanda)
        double sumaOferta = 0.0;
        for (int r = 0; r < ultimaFila; r++) {
            Object v = tbl.getValueAt(r, colOferta);
            if (!esNumero(v)) {
                JOptionPane.showMessageDialog(modelo.getVista(),
                        String.format("Oferta inválida en la fila %d.", r + 1),
                        "Dato inválido", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            double val = Double.parseDouble(v.toString());
            if (val < 0) {
                JOptionPane.showMessageDialog(modelo.getVista(),
                        "Las ofertas deben ser >= 0.", "Dato inválido", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            sumaOferta += val;
        }

        // 3) Validar DEMANDAS (fila final "Demanda", columnas 1..colOferta-1)
        double sumaDemanda = 0.0;
        for (int c = 1; c < colOferta; c++) {
            Object v = tbl.getValueAt(ultimaFila, c);
            if (!esNumero(v)) {
                JOptionPane.showMessageDialog(modelo.getVista(),
                        String.format("Demanda inválida en la columna %d.", c + 1),
                        "Dato inválido", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            double val = Double.parseDouble(v.toString());
            if (val < 0) {
                JOptionPane.showMessageDialog(modelo.getVista(),
                        "Las demandas deben ser >= 0.", "Dato inválido", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            sumaDemanda += val;
        }

        // 4) Chequear balance
        double EPS = 1e-6;
        if (Math.abs(sumaOferta - sumaDemanda) > EPS) {
            JOptionPane.showMessageDialog(modelo.getVista(),
                    String.format("Problema desbalanceado: Oferta = %.4f, Demanda = %.4f",
                            sumaOferta, sumaDemanda),
                    "Desbalance", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * Extrae costos, oferta y demanda desde la JTable respetando tu layout.
     */
    private ProblemaTransporte getProblemaFromUI() {
        JTable tbl = modelo.getVista().tblDatos;
        int rows = tbl.getRowCount();
        int cols = tbl.getColumnCount();

        int m = rows - 1;  // sin la fila "Demanda"
        int n = cols - 2;  // sin col de etiquetas (0) ni "Oferta" (última)

        double[][] costos = new double[m][n];
        double[] oferta = new double[m];
        double[] demanda = new double[n];

        // etiquetas (opcional)
        String[] filas = new String[m];
        String[] columnas = new String[n];

        // columnas D1..Dn están en 1..n
        for (int c = 0; c < n; c++) {
            columnas[c] = tbl.getColumnName(c + 1);
        }

        // llenar costos y oferta
        for (int r = 0; r < m; r++) {
            filas[r] = String.valueOf(tbl.getValueAt(r, 0)); // etiqueta fila
            for (int c = 0; c < n; c++) {
                costos[r][c] = Double.parseDouble(tbl.getValueAt(r, c + 1).toString());
            }
            oferta[r] = Double.parseDouble(tbl.getValueAt(r, cols - 1).toString());
        }

        // demandas (fila "Demanda")
        for (int c = 0; c < n; c++) {
            demanda[c] = Double.parseDouble(tbl.getValueAt(rows - 1, c + 1).toString());
        }

        return new ProblemaTransporte(costos, oferta, demanda, filas, columnas);
    }

}
