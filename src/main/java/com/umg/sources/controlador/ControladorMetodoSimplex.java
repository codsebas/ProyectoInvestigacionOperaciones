package com.umg.sources.controlador;

import com.umg.sources.logica.MetodoSimplex;
import com.umg.sources.modelo.ModeloMetodoSimplex;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 👇 IMPORTS NUEVOS
import com.umg.sources.vistas.VistaMenu;
import org.netbeans.lib.awtextra.AbsoluteLayout;
import org.netbeans.lib.awtextra.AbsoluteConstraints;

public class ControladorMetodoSimplex implements ActionListener, MouseListener {

    private final ModeloMetodoSimplex modelo;
    private final MetodoSimplex metodo = new MetodoSimplex();

    // 👇 NUEVO: referencia al menú
    private VistaMenu vistaMenu;

    public ControladorMetodoSimplex(ModeloMetodoSimplex modelo) {
        this.modelo = modelo;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(modelo.getVista().CmbOpciones.getActionCommand())) {
            // no-op
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Object src = e.getComponent();

        if (src.equals(modelo.getVista().BtnGenerar)) {
            boolean maximize = modelo.getVista().CmbOpciones.getSelectedItem().toString()
                    .toLowerCase(Locale.ROOT).contains("max");

            if (!maximize) {
                JOptionPane.showMessageDialog(
                        modelo.getVista(),
                        "Actualmente sólo está implementada la versión de maximización.",
                        "Aviso",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            if (!validarBlancos()) {
                mensajeBlancos();
                return;
            }

            String objetivo = modelo.getVista().TxtZ.getText().trim();
            String restricciones = construirRestricciones();

            modelo.getVista().PanelTabla.removeAll();
            modelo.getVista().PanelTabla.setLayout(new BorderLayout());

            try {
                List<DefaultTableModel> historial = metodo.resolver(objetivo, restricciones);

                JTabbedPane tabs = new JTabbedPane();

                if (historial != null && !historial.isEmpty()) {
                    for (int i = 0; i < historial.size(); i++) {
                        JTable t = new JTable(historial.get(i));
                        ajustarTamañoTabla(t);
                        String titulo = (i == 0) ? "Inicial" : "Iteración " + i;
                        tabs.addTab(titulo, new JScrollPane(t));
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            modelo.getVista(),
                            "No se generaron tablas. Revisa el formato de entrada.",
                            "Sin resultados",
                            JOptionPane.WARNING_MESSAGE
                    );
                }

                JTable tFinal = new JTable(metodo.tablaFinal());
                ajustarTamañoTabla(tFinal);
                tabs.addTab("Final", new JScrollPane(tFinal));

                modelo.getVista().PanelTabla.add(tabs, BorderLayout.CENTER);
                modelo.getVista().PanelTabla.revalidate();
                modelo.getVista().PanelTabla.repaint();

                modelo.getVista().TxtResultado.setText(construirResumenResultado(tFinal));

            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(
                        modelo.getVista(),
                        "Error de formato: " + iae.getMessage(),
                        "Entrada inválida",
                        JOptionPane.ERROR_MESSAGE
                );
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(
                        modelo.getVista(),
                        "Error al resolver: " + ex.getMessage(),
                        "Error en Simplex",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } else if (src.equals(modelo.getVista().BtnLimpiar)) {
            limpiar();

        } else if (src.equals(modelo.getVista().BtnMenu)) {
            // ✅ NAVEGAR A VistaMenu manteniendo AbsoluteLayout
            if (vistaMenu == null) vistaMenu = new VistaMenu();
            cambiarVista(vistaMenu);
            return;
        }
    }

    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }

    // ===================== Helpers de UI =====================

    private void limpiar() {
        modelo.getVista().CmbOpciones.setSelectedIndex(0);
        modelo.getVista().TxtZ.setText("");
        modelo.getVista().TxtRestriccion1.setText("");
        modelo.getVista().TxtRestriccion2.setText("");
        modelo.getVista().TxtRestriccion3.setText("");
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
        tabla.setPreferredScrollableViewportSize(
                new Dimension(tabla.getPreferredSize().width, Math.max(alturaTotal, 150))
        );
    }

    private boolean validarBlancos() {
        return !modelo.getVista().TxtZ.getText().trim().isEmpty() &&
                (!modelo.getVista().TxtRestriccion1.getText().trim().isEmpty() ||
                 !modelo.getVista().TxtRestriccion2.getText().trim().isEmpty() ||
                 !modelo.getVista().TxtRestriccion3.getText().trim().isEmpty() ||
                 !modelo.getVista().TxtRestriccion4.getText().trim().isEmpty() ||
                 !modelo.getVista().TxtRestriccion5.getText().trim().isEmpty());
    }

    private void mensajeBlancos() {
        JOptionPane.showMessageDialog(
                modelo.getVista(),
                "Uno o más campos están en blanco.",
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
        if (modelo.getVista().TxtZ.getText().trim().isEmpty()) {
            modelo.getVista().TxtZ.requestFocus();
        } else {
            modelo.getVista().TxtRestriccion1.requestFocus();
        }
    }

    private String construirRestricciones() {
        List<String> res = new ArrayList<>();
        agregarSiNoVacio(res, modelo.getVista().TxtRestriccion1.getText());
        agregarSiNoVacio(res, modelo.getVista().TxtRestriccion2.getText());
        agregarSiNoVacio(res, modelo.getVista().TxtRestriccion3.getText());
        agregarSiNoVacio(res, modelo.getVista().TxtRestriccion4.getText());
        agregarSiNoVacio(res, modelo.getVista().TxtRestriccion5.getText());
        return String.join(";", res);
    }

    private void agregarSiNoVacio(List<String> lista, String valor) {
        if (valor != null) {
            String v = valor.trim();
            if (!v.isEmpty()) lista.add(v);
        }
    }

    private String construirResumenResultado(JTable tablaFinal) {
        try {
            DefaultTableModel m = (DefaultTableModel) tablaFinal.getModel();
            if (m.getRowCount() == 0) return "";

            int colZ = m.getColumnCount() - 1;
            String zStr = safeCell(m.getValueAt(0, colZ));
            double z = parseNumber(zStr);

            String x1 = null, x2 = null;
            if (m.getColumnCount() > 0) x1 = safeCell(m.getValueAt(0, 0));
            if (m.getColumnCount() > 1) x2 = safeCell(m.getValueAt(0, 1));

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Z=%.6f", z));
            if (x1 != null) {
                double vx1 = parseNumber(x1);
                sb.append(String.format(", x=%.6f", vx1));
            }
            if (x2 != null) {
                double vx2 = parseNumber(x2);
                sb.append(String.format(", y=%.6f", vx2));
            }

            return sb.toString();
        } catch (Exception ex) {
            try {
                DefaultTableModel m = (DefaultTableModel) tablaFinal.getModel();
                int colZ = m.getColumnCount() - 1;
                String zStr = safeCell(m.getValueAt(0, colZ));
                double z = parseNumber(zStr);
                return String.format("Z=%.6f", z);
            } catch (Exception ignore) {
                return "";
            }
        }
    }

    private String safeCell(Object v) {
        return v == null ? "0" : v.toString().replace(",", ".").trim();
    }

    private double parseNumber(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
            String t = s.replaceAll("[^0-9+\\-\\.Ee]", "");
            return Double.parseDouble(t.isEmpty() ? "0" : t);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔧 NUEVO: cambiarVista que funciona con AbsoluteLayout
    //     - Si existe un campo público "contenedor" en la vista, lo usa
    //     - Si no, cae al getContentPane() (JFrame)
    // ─────────────────────────────────────────────────────────────
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

    // Intenta usar un JPanel público llamado "contenedor"; si no, usa el content pane
    private Container obtenerContenedorVista() {
        Object vista = modelo.getVista();
        try {
            java.lang.reflect.Field f = vista.getClass().getField("contenedor");
            Object pnl = f.get(vista);
            if (pnl instanceof Container) return (Container) pnl;
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
            // cae al contentPane
        }
        // Si la vista es un JFrame:
        if (vista instanceof JFrame) return ((JFrame) vista).getContentPane();
        // Si la vista es un JDialog:
        if (vista instanceof JDialog) return ((JDialog) vista).getContentPane();
        // Último recurso: crear un panel contenedor en caliente (no debería pasar)
        return new JPanel(new AbsoluteLayout());
    }
}