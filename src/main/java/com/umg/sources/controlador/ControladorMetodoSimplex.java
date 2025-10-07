package com.umg.sources.controlador;

import com.umg.sources.logica.LogicaSimplex;
import com.umg.sources.modelo.ModeloMetodoSimplex;
import com.umg.sources.vistas.VistaMetodoSimplex;   

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
        if(e.getActionCommand().equals(modelo.getVista().CmbOpciones.getActionCommand())){

        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getComponent().equals(modelo.getVista().BtnGenerar)) {
            boolean maximize = modelo.getVista().CmbOpciones.getSelectedItem().toString()
                    .toLowerCase(Locale.ROOT).contains("max");

            if (!validarBlancos()) {
                mensajeBlancos();
                return;
            }

            String objetivo = modelo.getVista().TxtZ.getText().trim();
            List<String> restricciones = new ArrayList<>();
            restricciones.add(modelo.getVista().TxtRestriccion1.getText());
            restricciones.add(modelo.getVista().TxtRestriccion2.getText());
            restricciones.add(modelo.getVista().TxtRestriccion3.getText());
            restricciones.add(modelo.getVista().TxtRestriccion4.getText());
            restricciones.add(modelo.getVista().TxtRestriccion5.getText());

            LogicaSimplex.Resultado res = new LogicaSimplex().calcularSimplex(maximize, objetivo, restricciones);

            JTabbedPane tabs = new JTabbedPane();
            for (int i = 0; i < res.history.size(); i++) {
                JTable t = new JTable(res.history.get(i));
                ajustarTamañoTabla(t);
                String titulo = (i == 0) ? "Inicial" : "Iteración " + i;
                tabs.addTab(titulo, new JScrollPane(t));
            }

            modelo.getVista().PanelTabla.removeAll();
            modelo.getVista().PanelTabla.setLayout(new BorderLayout());
            modelo.getVista().PanelTabla.add(tabs, BorderLayout.CENTER);
            modelo.getVista().PanelTabla.revalidate();
            modelo.getVista().PanelTabla.repaint();

            modelo.getVista().TxtResultado.setText(
                    String.format("z=%.6f, x=%.6f, y=%.6f", res.z, res.x, res.y)
            );

        } else if (e.getComponent().equals(modelo.getVista().BtnLimpiar)) {
            limpiar();
        } else if (e.getComponent().equals(modelo.getVista().BtnMenu)) {
            // navegación...
        }
    }


    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }

    private void limpiar(){
        modelo.getVista().CmbOpciones.setSelectedIndex(0);
        modelo.getVista().TxtZ.setText("");
        modelo.getVista().TxtRestriccion1.setText("");
        modelo.getVista().TxtRestriccion3.setText("");
        modelo.getVista().TxtRestriccion2.setText("");
        modelo.getVista().TxtRestriccion4.setText("");
        modelo.getVista().TxtRestriccion5.setText("");
        modelo.getVista().TxtResultado.setText("");
        modelo.getVista().PanelTabla.removeAll();
        modelo.getVista().PanelTabla.revalidate();
        modelo.getVista().PanelTabla.repaint();
    }

    private void ajustarTamañoTabla(JTable tabla) {
        int rowHeight = tabla.getRowHeight();
        int rowCount = tabla.getRowCount();
        int headerHeight = tabla.getTableHeader().getPreferredSize().height;

        int alturaTotal = (rowHeight * rowCount) + headerHeight;
        tabla.setPreferredScrollableViewportSize(new Dimension(tabla.getPreferredSize().width, alturaTotal));
    }

    private boolean validarBlancos(){
        boolean ok =
        !modelo.getVista().TxtZ.getText().trim().isEmpty() &&
            (!modelo.getVista().TxtRestriccion1.getText().trim().isEmpty() ||
                !modelo.getVista().TxtRestriccion2.getText().trim().isEmpty() ||
                !modelo.getVista().TxtRestriccion3.getText().trim().isEmpty() ||
                !modelo.getVista().TxtRestriccion4.getText().trim().isEmpty() ||
                !modelo.getVista().TxtRestriccion5.getText().trim().isEmpty()
            );
        return ok;
    }

    private void mensajeBlancos(){
        JOptionPane.showMessageDialog(
                modelo.getVista(),
                "Uno o más campos están en blanco.",
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
        if(modelo.getVista().TxtZ.getText().trim().isEmpty()){
            modelo.getVista().TxtZ.requestFocus();
        } else {
            modelo.getVista().TxtRestriccion1.requestFocus();
        }
    }
}