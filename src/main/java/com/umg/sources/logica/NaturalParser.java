package com.umg.sources.logica;

import java.text.Normalizer;

public class NaturalParser {

    // Normaliza: unicode raro -> ASCII simple, colapsa espacios, etc.
    private static String normalize(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFKC);
        t = t.replace("−","-")           // menos unicode
                .replace("×","*");          // por si meten símbolo de multiplicación
        // deja solo lo necesario (números, x/y, signos, punto/coma, *, espacios)
        t = t.replaceAll("[^0-9xXyY+\\-\\.,\\*<>= ]", " ");
        t = t.trim().replaceAll("\\s+", " ");
        return t;
    }

    private static double toNumber(String s) {
        if (s == null) return 0.0;
        s = s.trim().replace(',', '.');
        if (s.isEmpty() || s.equals("+")) return 1.0;
        if (s.equals("-")) return -1.0;
        return Double.parseDouble(s);
    }

    /**
     * Parse de un lado tipo "120x + 200y", "x - y", "-0,5y + x", "120 x + 200 y", "2*x + 3*y"
     * SIN regex: reemplaza '-' por '+-' y separa por '+'. Soporta espacios y '*'.
     * Devuelve [coefX, coefY].
     */
    private static double[] parseXYSide(String rawSide) {
        String side = normalize(rawSide);

        // 1) quita espacios alrededor de x/y y de '*'
        side = side.replaceAll("\\s*\\*\\s*", "*")   // "2 * x" -> "2*x"
                .replaceAll("\\s*[xX]\\b", "x")  // "  x" -> "x"
                .replaceAll("\\s*[yY]\\b", "y")
                .replaceAll("\\bx\\s*", "x")
                .replaceAll("\\by\\s*", "y");

        // 2) convierte "2*x" en "2x"
        side = side.replaceAll("(\\d(?:[\\.\\,]\\d+)?)\\*([xy])", "$1$2");

        // 3) tokeniza por "+" manejando signos negativos como "+-"
        side = side.replace("-", "+-");
        String[] tokens = side.split("\\+");

        double cx = 0.0, cy = 0.0;
        int hits = 0;

        for (String tk : tokens) {
            String term = tk.trim();
            if (term.isEmpty()) continue;

            // ¿es término de x?
            int ix = term.indexOf('x');
            if (ix >= 0) {
                String coef = term.substring(0, ix).trim();
                double v = toNumber(coef);
                cx += v;
                hits++;
                continue;
            }
            // ¿es término de y?
            int iy = term.indexOf('y');
            if (iy >= 0) {
                String coef = term.substring(0, iy).trim();
                double v = toNumber(coef);
                cy += v;
                hits++;
                continue;
            }
            // si llega aquí es un término que no contiene x ni y (lo ignoramos)
        }

        if (hits == 0)
            throw new IllegalArgumentException("No se encontró ninguna variable x o y en: \"" + rawSide + "\"");

        return new double[]{cx, cy};
    }

    /** Objetivo SOLO como "2x + 3y" (sin 'Z ='). Devuelve [c1, c2]. */
    public static double[] parseObjective(String raw){
        if (raw == null || raw.trim().isEmpty())
            throw new IllegalArgumentException("Objetivo vacío.");
        return parseXYSide(raw);
    }

    // --------- Restricciones ---------

    public static class ParsedConstraint {
        public final double a, b, c;
        public final LPSolver2D.Type type;
        public ParsedConstraint(double a, double b, double c, LPSolver2D.Type type){
            this.a=a; this.b=b; this.c=c; this.type=type;
        }
    }

    /** Acepta: "2x + y <= 10", "x - y = 3", "-y >= 2,5", también ≤ ≥ y coma decimal. */
    public static ParsedConstraint parseConstraint(String raw){
        if (raw == null || raw.trim().isEmpty())
            throw new IllegalArgumentException("Restricción vacía.");

        String s = normalize(raw).replace("≤","<=").replace("≥",">=");

        String op;
        if (s.contains("<=")) op = "<=";
        else if (s.contains(">=")) op = ">=";
        else if (s.contains("="))  op = "=";
        else throw new IllegalArgumentException("Falta operador (<=, >= o =) en: \"" + raw + "\"");

        String[] parts = s.split("\\Q" + op + "\\E", 2);
        if (parts.length != 2)
            throw new IllegalArgumentException("Formato inválido: \"" + raw + "\"");

        double[] lhs = parseXYSide(parts[0]);

        double rhs;
        try {
            rhs = Double.parseDouble(parts[1].trim().replace(',', '.'));
        } catch (NumberFormatException ex){
            throw new IllegalArgumentException("RHS numérico inválido en: \"" + raw + "\"");
        }

        LPSolver2D.Type t = switch (op) {
            case "<=" -> LPSolver2D.Type.LE;
            case ">=" -> LPSolver2D.Type.GE;
            default   -> LPSolver2D.Type.EQ;
        };

        return new ParsedConstraint(lhs[0], lhs[1], rhs, t);
    }
}
