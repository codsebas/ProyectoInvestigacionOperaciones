package com.umg.sources.logica;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

public class LogicaSimplex {

    public static class Resultado {
        public final DefaultTableModel tablaFinal;
        public final List<DefaultTableModel> history;
        public final String mensaje;
        public final double x, y, z;
        public final boolean ok;

        public Resultado(DefaultTableModel tablaFinal, List<DefaultTableModel> history,
                         String mensaje, double x, double y, double z, boolean ok) {
            this.tablaFinal = tablaFinal;
            this.history = history;
            this.mensaje = mensaje;
            this.x = x; this.y = y; this.z = z;
            this.ok = ok;
        }
    }

    public Resultado calcularSimplex(boolean maximize, String objetivoRaw, List<String> restriccionesRaw){
        try {
            
            double[] c = NaturalParser_IO.parseObjective(objetivoRaw);

            // 2) Restricciones
            List<LPSolver2D_IO.Constraint> cons = new ArrayList<>();
            for (String r : restriccionesRaw) {
                if (r == null || r.trim().isEmpty()) continue;

            
                List<NaturalParser_IO.ParsedConstraint> joint = NaturalParser_IO.tryParseJointLowerBound(r);
                if (joint != null && !joint.isEmpty()) {
                    for (var pc : joint)
                        cons.add(new LPSolver2D_IO.Constraint(pc.a, pc.b, pc.c, pc.type));
                    continue;
                }

                NaturalParser_IO.ParsedConstraint pc = NaturalParser_IO.parseConstraint(r);
                cons.add(new LPSolver2D_IO.Constraint(pc.a, pc.b, pc.c, pc.type));
            }
            if (cons.isEmpty())
                throw new IllegalArgumentException("Debe ingresar al menos una restricción.");

            // 3) Correr Simplex (Max o Min)
            LPSolver2D_IO.Sense sense = maximize ? LPSolver2D_IO.Sense.MAX : LPSolver2D_IO.Sense.MIN;
            LPSolver2D_IO.Result s = LPSolver2D_IO.solve(sense, c, cons);

            String msg = s.message + String.format(" (x=%.6f, y=%.6f, Z=%.6f)", s.x, s.y, s.z);
            boolean ok = s.optimal && !s.unbounded;

            return new Resultado(s.tableau, s.history, msg, s.x, s.y, s.z, ok);

        } catch (Exception ex){
            DefaultTableModel vacio = new DefaultTableModel(new Object[]{"x","y","RHS"}, 0);
            List<DefaultTableModel> hist = new ArrayList<>();
            return new Resultado(vacio, hist, "Error: " + ex.getMessage(), Double.NaN, Double.NaN, Double.NaN, false);
        }
    }
}
