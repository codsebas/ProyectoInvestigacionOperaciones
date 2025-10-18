package com.umg.sources.logica;

import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MetodoSimplex {

    // ----------------- Modelo interno -----------------
    private static final double EPS = 1e-9;

    private double[][] T;          
    private int m;                
    private int n;               
    private int nSlack;           
    private int nArtificial;       
    private int colRHS;           
    private int[] basis;           
    private String[] colNames;    
    private String[] rowNames;     
    private double[] cOriginal;    

    private final List<DefaultTableModel> historial = new ArrayList<>();

  
    public List<DefaultTableModel> resolver(String funcionObjetivo, String restricciones) {
      
        cOriginal = parseObjetivo(funcionObjetivo);
        n = cOriginal.length;

        List<Constraint> cons = parseRestricciones(restricciones, n);
        
        cons.removeIf(c -> c.isOnlyNonNeg);

        if (cons.isEmpty())
            throw new IllegalArgumentException("No hay restricciones válidas.");

   
        List<RowBuild> filas = new ArrayList<>();
        nSlack = 0; nArtificial = 0;

        for (Constraint c : cons) {
          
            RowBuild rb = toLeft(c, n);
          
            if (rb.b < -EPS) {
                rb.multiply(-1);
                rb.flipSense();
            }
         
            if (rb.sense == Sense.LE) {
                rb.addSlack(++nSlack, +1);
            } else if (rb.sense == Sense.GE) {
                rb.addSlack(++nSlack, -1);   
                rb.addArtificial(++nArtificial);
            } else { 
                rb.addArtificial(++nArtificial);
            }
            filas.add(rb);
        }

     
        buildTableauPhaseI(filas);

       
        historial.add(toTableModel("Inicial (Fase I)"));

        
        while (pivotOneIteration("Fase I")) {
            historial.add(toTableModel("Iteración (Fase I)"));
        }

       
        double objectivePhaseI = T[0][colRHS];
        if (objectivePhaseI < -1e-6) {
            throw new RuntimeException("Problema INFACTIBLE (Fase I terminó con valor < 0).");
        }

        
        removeArtificialColumnsAndBuildPhaseII();

        historial.add(toTableModel("Inicio (Fase II)"));

       
        while (pivotOneIteration("Fase II")) {
            historial.add(toTableModel("Iteración (Fase II)"));
        }

        return new ArrayList<>(historial);
    }

    public DefaultTableModel tablaFinal() {
        
        int totalVars = colRHS - 1; 
        String[] headers = new String[totalVars + 1];
        int h = 0;
        
        for (int j = 1; j < colRHS; j++) headers[h++] = colNames[j];
        headers[totalVars] = "Z";

        Object[][] data = new Object[1][totalVars + 1];
        Arrays.fill(data[0], 0.0);

       
        for (int i = 1; i <= m; i++) {
            int col = basis[i];
            int idx = col - 1; 
            if (idx >= 0 && idx < totalVars) {
                data[0][idx] = fmt(T[i][colRHS]);
            }
        }

        data[0][totalVars] = fmt(T[0][colRHS]);
        return new DefaultTableModel(data, headers);
    }

  
    private void buildTableauPhaseI(List<RowBuild> filas) {
        m = filas.size();

        int nCols = 1  + n+ nSlack + nArtificial + 1 ;
        colRHS = nCols - 1;

        T = new double[m + 1][nCols];
        colNames = new String[nCols];
        rowNames = new String[m + 1];
        basis = new int[m + 1];

        
        colNames[0] = "Z";
        for (int j = 1; j <= n; j++) colNames[j] = "x" + j;
        for (int k = 1; k <= nSlack; k++) colNames[n + k] = "s" + k;
        for (int a = 1; a <= nArtificial; a++) colNames[n + nSlack + a] = "a" + a;
        colNames[colRHS] = "RHS";

       
        T[0][0] = 1.0;
        for (int a = 1; a <= nArtificial; a++) {
            int colA = n + nSlack + a;
            T[0][colA] = -1.0;
        }
        rowNames[0] = "Z";

        
        int row = 1;
        int slackCount = 0, artCount = 0;

        for (RowBuild rb : filas) {
            
            for (int j = 0; j < n; j++) T[row][1 + j] = rb.coeff[j];

         
            if (rb.slackSign != 0) {
                slackCount++;
                int colS = 1 + n + (slackCount - 1);
                T[row][colS] = rb.slackSign;
                rb.slackIndex = slackCount; 
            }

           
            if (rb.hasArtificial) {
                artCount++;
                int colA = 1 + n + nSlack + (artCount - 1);
                T[row][colA] = 1.0;
                basis[row] = colA;
                rowNames[row] = colNames[colA];
            } else if (rb.slackSign == +1) {
                // base con holgura +1
                int colS = 1 + n + (rb.slackIndex - 1);
                basis[row] = colS;
                rowNames[row] = colNames[colS];
            }

            T[row][colRHS] = rb.b;
            row++;
        }

      
        for (int i = 1; i <= m; i++) {
            int colB = basis[i];
            if (isArtificial(colB)) {
                for (int j = 0; j < nCols; j++) T[0][j] += T[i][j];
            }
        }
    }


    
    private void removeArtificialColumnsAndBuildPhaseII() {
    
        List<Integer> keep = new ArrayList<>();
        for (int j = 0; j < T[0].length; j++) {
            if (j == 0 || j == colRHS || !isArtificial(j)) keep.add(j);
        }

        
        int nColsNew = keep.size();
        double[][] N = new double[m + 1][nColsNew];
        String[] namesNew = new String[nColsNew];

        for (int jj = 0; jj < keep.size(); jj++) {
            int oldJ = keep.get(jj);
            namesNew[jj] = colNames[oldJ];
            for (int i = 0; i <= m; i++) {
                N[i][jj] = T[i][oldJ];
            }
        }

        T = N;
        colNames = namesNew;
        colRHS = nColsNew - 1;

        
        int[] basisNew = new int[m + 1];
        basisNew[0] = 0;
        for (int i = 1; i <= m; i++) {
            int old = basis[i];
            
            for (int jj = 0; jj < keep.size(); jj++) if (keep.get(jj) == old) basisNew[i] = jj;
            rowNames[i] = colNames[basisNew[i]];
        }
        basis = basisNew;

        
        Arrays.fill(T[0], 0.0);
        T[0][0] = 1.0;
        
        for (int j = 1; j < colRHS; j++) {
            String name = colNames[j];
            if (name.startsWith("x")) {
                int idx = Integer.parseInt(name.substring(1)) - 1;
                if (idx >= 0 && idx < cOriginal.length) T[0][j] = -cOriginal[idx];
            }
        }
       
        for (int i = 1; i <= m; i++) {
            int colB = basis[i];
            double coef = T[0][colB];
            if (Math.abs(coef) > EPS) {
                for (int j = 0; j <= colRHS; j++) T[0][j] -= coef * T[i][j];
            }
        }
    }

    
    private boolean pivotOneIteration(String fase) {
       
        int colIn = -1;
        double min = -EPS;
        for (int j = 1; j < colRHS; j++) { 
            if (T[0][j] < min) { min = T[0][j]; colIn = j; }
        }
        if (colIn == -1) return false; 

        
        int rowOut = -1;
        double best = Double.POSITIVE_INFINITY;
        for (int i = 1; i <= m; i++) {
            double a = T[i][colIn];
            if (a > EPS) {
                double ratio = T[i][colRHS] / a;
                if (ratio < best - 1e-12 || (Math.abs(ratio - best) < 1e-12 && bland(colIn, basis[i]))) {
                    best = ratio; rowOut = i;
                }
            }
        }
        if (rowOut == -1) throw new RuntimeException("Solución NO ACOTADA (" + fase + ").");

        
        pivot(rowOut, colIn);
        basis[rowOut] = colIn;
        rowNames[rowOut] = colNames[colIn];
        return true;
    }

    private void pivot(int r, int c) {
        double p = T[r][c];
        for (int j = 0; j <= colRHS; j++) T[r][j] /= p;
        for (int i = 0; i < T.length; i++) if (i != r) {
            double f = T[i][c];
            if (Math.abs(f) > EPS) {
                for (int j = 0; j <= colRHS; j++) T[i][j] -= f * T[r][j];
            }
        }
    }

   
    private boolean bland(int candidateCol, int currentBasisCol) {
        return candidateCol < currentBasisCol;
    }

  
    private DefaultTableModel toTableModel(String titulo) {
        String[] headers = new String[colNames.length + 1];
        headers[0] = "Var. Básica";
        System.arraycopy(colNames, 0, headers, 1, colNames.length);

        Object[][] data = new Object[m + 1][headers.length];
        for (int i = 0; i <= m; i++) {
            data[i][0] = (i == 0) ? "Z" : rowNames[i];
            for (int j = 0; j < colNames.length; j++) {
                data[i][j + 1] = fmt(T[i][j]);
            }
        }
        DefaultTableModel model = new DefaultTableModel(data, headers);
        model.setColumnIdentifiers(headers);
        return model;
    }

    private static String fmt(double v) { return String.format(Locale.ROOT, "%.2f", v); }
    private boolean isArtificial(int col) { return colNames[col] != null && colNames[col].startsWith("a"); }


    private static class Constraint {
        double[] left;   double cLeft;
        double[] right;  double cRight;
        Sense sense;
        boolean isOnlyNonNeg;
    }
    private enum Sense { LE, GE, EQ }

    private double[] parseObjetivo(String s) {
        ParsedExpr e = parseLinearExpr(s, 0);
        return e.coef;
    }

    private List<Constraint> parseRestricciones(String s, int nVars) {
        List<Constraint> out = new ArrayList<>();
        if (s == null || s.trim().isEmpty()) return out;
        String[] parts = s.split(";");
        for (String raw : parts) {
            String r = raw.trim();
            if (r.isEmpty()) continue;

       
            if (r.matches("\\s*x\\d+\\s*>=\\s*0\\s*")) {
                Constraint c = new Constraint();
                c.isOnlyNonNeg = true;
                out.add(c);
                continue;
            }

            Sense sense;
            String[] sides;
            if      (r.contains("<=")) { sense = Sense.LE; sides = r.split("<="); }
            else if (r.contains(">=")) { sense = Sense.GE; sides = r.split(">="); }
            else if (r.contains("="))  { sense = Sense.EQ; sides = r.split("=");  }
            else throw new IllegalArgumentException("Restricción inválida: " + r);

            if (sides.length != 2) throw new IllegalArgumentException("Restricción mal formada: " + r);

            ParsedExpr L = parseLinearExpr(sides[0], nVars);
            ParsedExpr R = parseLinearExpr(sides[1], nVars);

            Constraint c = new Constraint();
            c.left  = L.coef;  c.cLeft  = L.c0;
            c.right = R.coef;  c.cRight = R.c0;
            c.sense = sense;
            out.add(c);
        }
        return out;
    }

    private static class ParsedExpr {
        double[] coef; 
        double c0;      
    }

    private ParsedExpr parseLinearExpr(String raw, int nVarsHint) {
        String s = raw.replace(" ", "").toLowerCase(Locale.ROOT);
      
        s = s.replace("-", "+-");
        if (s.startsWith("+")) s = s.substring(1);

       
        String[] terms = s.isEmpty() ? new String[0] : s.split("\\+");

        int maxVar = n; 
       
        if (nVarsHint == 0) {
            Pattern pVar = Pattern.compile("([+-]?\\d*(?:\\.\\d+)?)\\*?x(\\d+)");
            for (String t : terms) {
                Matcher m = pVar.matcher(t);
                if (m.matches()) {
                    int idx = Integer.parseInt(m.group(2));
                    if (idx > maxVar) maxVar = idx;
                }
            }
        }
        int size = Math.max(nVarsHint, Math.max(1, maxVar));
        double[] coef = new double[size];
        double c0 = 0.0;

        Pattern pVar = Pattern.compile("([+-]?\\d*(?:\\.\\d+)?)\\*?x(\\d+)");
        Pattern pConst = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)");

        for (String t : terms) {
            if (t.isEmpty()) continue;
            Matcher mv = pVar.matcher(t);
            if (mv.matches()) {
                String cStr = mv.group(1);
                double c = (cStr == null || cStr.isEmpty() || cStr.equals("+") || cStr.equals("-")) ?
                        (cStr != null && cStr.startsWith("-") ? -1.0 : 1.0) :
                        Double.parseDouble(cStr);
                int idx = Integer.parseInt(mv.group(2)) - 1;
                if (idx >= coef.length) coef = Arrays.copyOf(coef, idx + 1);
                coef[idx] += c;
            } else {
                Matcher mc = pConst.matcher(t);
                if (mc.matches()) c0 += Double.parseDouble(mc.group(1));
                else throw new IllegalArgumentException("Término inválido: '" + t + "' en '" + raw + "'");
            }
        }

        ParsedExpr pe = new ParsedExpr();
        pe.coef = coef;
        pe.c0 = c0;
        return pe;
    }

  
    private static class RowBuild {
        double[] coeff;
        double b;
        Sense sense;

        int slackSign;        
        int slackIndex = 0;   
        boolean hasArtificial;
        int artificialIndex = 0;

        RowBuild(int n) { coeff = new double[n]; }

        void multiply(double k) {
            for (int i = 0; i < coeff.length; i++) coeff[i] *= k;
            b *= k;
        }
        void flipSense() {
            if (sense == Sense.LE) sense = Sense.GE;
            else if (sense == Sense.GE) sense = Sense.LE;
        }
        void addSlack(int idx, int sign) { slackSign = sign; slackIndex = idx; }
        void addArtificial(int idx) { hasArtificial = true; artificialIndex = idx; }
    }


    private RowBuild toLeft(Constraint c, int n) {
        RowBuild rb = new RowBuild(n);
      
        for (int i = 0; i < n; i++) {
            double li = i < c.left.length  ? c.left[i]  : 0.0;
            double ri = i < c.right.length ? c.right[i] : 0.0;
            rb.coeff[i] = li - ri;
        }
        rb.b =  (c.cRight - c.cLeft);
        rb.sense = c.sense;
        return rb;
    }
}
