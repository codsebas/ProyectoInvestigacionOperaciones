/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.sources.logica;

import java.util.*;

/**
 * Simplex de dos fases (Two-Phase). Soporta:
 *  - Restricciones <=, >=, = (char: '<','>','=')
 *  - Max y Min (min se convierte a max internamente)
 *  - Detección de infactibilidad y no acotado
 *
 * Convenciones:
 *  - A: m x n (solo variables de decisión originales)
 *  - b: m (RHS). Si b[i] < 0, se multiplica la fila por -1 y se invierte la desigualdad.
 *  - rel: m (cada entrada en {'<','>','='})
 *  - c: n (coeficientes de la FO en términos de las variables originales)
 *  - maximize: true (Max), false (Min) — Min se pasa a Max con -c.
 */
public class LogicaSimplex {

    public static final double EPS = 1e-9;
    public static final int MAX_ITERS = 1000;

    // ======== Tipos de datos públicos ========

    public static class Result {
        public boolean feasible;
        public boolean unbounded;
        public double z;        // valor óptimo (en signo del usuario: Max-> tal cual; Min-> valor mínimo)
        public double[] x;      // solución en variables originales (longitud n). Si no hay solución, null.
        public String message;

        // Para visualización opcional:
        public List<Iteration> steps = new ArrayList<>();
        public String[] varNamesPhase2; // nombres de columnas en la fase 2
        public int[] basis;             // índices de columnas básicas (fase 2 final)
    }

    public static class Iteration {
        public int phase;             // 1 o 2
        public int entering = -1;     // columna que entra
        public int leaving = -1;      // fila que sale
        public double[] reducedCosts; // Cj - Zj
        public int[] basis;           // índices de columnas básicas (en esta iter)
        public double[][] A;          // matriz (m x totalVarsActuales)
        public double[] b;            // RHS (m)
        public String note;           // comentario
        public String[] colNames;     // nombres de columnas

        public Iteration cloneShallow() {
            Iteration it = new Iteration();
            it.phase = this.phase;
            it.entering = this.entering;
            it.leaving = this.leaving;
            it.reducedCosts = this.reducedCosts != null ? this.reducedCosts.clone() : null;
            it.basis = this.basis != null ? this.basis.clone() : null;
            it.A = deepCopy2D(this.A);
            it.b = this.b != null ? this.b.clone() : null;
            it.note = this.note;
            it.colNames = this.colNames != null ? this.colNames.clone() : null;
            return it;
        }
    }

    // ======== API: resolver con matrices ========

    public static Result solve(double[][] A, double[] b, char[] rel, double[] c, boolean maximize) {
        NormalizedSystem ns = normalizeSystem(A, b, rel);

        // Min -> Max
        double[] cMax = c.clone();
        if (!maximize) for (int j=0;j<cMax.length;j++) cMax[j] = -cMax[j];

        // Construir forma estándar (slack/surplus/artificial)
        BuildForm bf = buildStandardForm(ns);

        // ====== Fase I: maximizar -sum(artificiales) ======
        Result res = new Result();
        res.steps = new ArrayList<>();

        PhaseState phase1 = new PhaseState();
        phase1.phase = 1;
        phase1.A = deepCopy2D(bf.Aext);
        phase1.b = ns.b.clone();
        phase1.totalVars = bf.totalVars;
        phase1.nOrig = A[0].length;
        phase1.isArtificial = bf.isArtificial.clone();
        phase1.C = new double[bf.totalVars];
        for (int j=0;j<bf.totalVars;j++) phase1.C[j] = bf.isArtificial[j] ? -1.0 : 0.0;

        phase1.basis = bf.initialBasis.clone();
        phase1.CB = new double[phase1.basis.length];
        for (int i=0;i<phase1.basis.length;i++) phase1.CB[i] = phase1.C[phase1.basis[i]];

        boolean unboundedPhase1 = runSimplex(phase1, res.steps);
        if (unboundedPhase1) {
            res.feasible = false; res.unbounded = false; res.z = Double.NaN; res.x = null;
            res.message = "Fase I fallida."; return res;
        }

        // Chequear artificiales en 0
        for (int j=0;j<phase1.totalVars;j++) {
            if (bf.isArtificial[j]) {
                double val = 0;
                for (int i=0;i<phase1.basis.length;i++) if (phase1.basis[i] == j) { val = phase1.b[i]; break; }
                if (Math.abs(val) > 1e-7) {
                    res.feasible = false; res.unbounded = false; res.z = Double.NaN; res.x = null;
                    res.message = "Infactible (quedaron artificiales básicas con valor > 0)."; return res;
                }
            }
        }

        // ====== Fase II: quitar artificiales y resolver el original ======
        PhaseState phase2 = removeArtificialColumns(phase1, bf);
        phase2.phase = 2;
        phase2.C = new double[phase2.totalVars];
        System.arraycopy(cMax, 0, phase2.C, 0, phase2.nOrig);

        phase2.CB = new double[phase2.basis.length];
        for (int i=0;i<phase2.basis.length;i++) {
            int col = phase2.basis[i];
            phase2.CB[i] = (col >= 0 && col < phase2.C.length) ? phase2.C[col] : 0.0;
        }

        boolean unbounded = runSimplex(phase2, res.steps);
        if (unbounded) {
            res.feasible = true; res.unbounded = true; res.z = maximize ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            res.x = null; res.message = "No acotado."; return res;
        }

        // Solución final (solo variables originales)
        double[] x = new double[phase2.nOrig];
        for (int i=0;i<phase2.basis.length;i++) {
            int col = phase2.basis[i];
            if (col >= 0 && col < phase2.nOrig) x[col] = phase2.b[i];
        }

        double zMax = 0.0;
        for (int i=0;i<phase2.basis.length;i++) zMax += phase2.CB[i] * phase2.b[i];
        double zUser = maximize ? zMax : -zMax;

        res.feasible = true; res.unbounded = false; res.z = zUser; res.x = x;
        res.message = "Óptimo encontrado.";
        res.basis = phase2.basis.clone();
        res.varNamesPhase2 = buildVarNames(phase2.nOrig, phase2.totalVars, phase2.isArtificial);
        return res;
    }

    /** Atajo: resolver desde restricciones 2D (usa LPSolver2D.Constraint) */
    public static Result solveFrom2D(java.util.List<com.umg.sources.logica.LPSolver2D.Constraint> cons, double c1, double c2, boolean maximize) {
        int m = cons.size(), n = 2;
        double[][] A = new double[m][n];
        double[] b = new double[m];
        char[] rel = new char[m];
        for (int i=0;i<m;i++){
            var ci = cons.get(i);
            A[i][0] = ci.a; A[i][1] = ci.b;
            b[i] = ci.c;
            rel[i] = switch (ci.type) {
                case LE -> '<'; case GE -> '>'; case EQ -> '=';
            };
        }
        double[] c = new double[]{c1, c2};
        return solve(A, b, rel, c, maximize);
    }

    // ======== Internals ========

    private static class NormalizedSystem {
        double[][] A; double[] b; char[] rel;
    }

    private static class BuildForm {
        double[][] Aext; boolean[] isArtificial; int totalVars; int[] initialBasis;
        int nSlacks, nSurplus, nArtificial, nOrig;
    }

    private static class PhaseState {
        int phase;
        double[][] A; double[] b; double[] C; double[] CB;
        int[] basis; int totalVars; int nOrig;
        boolean[] isArtificial; String[] colNames;
    }

    private static NormalizedSystem normalizeSystem(double[][] A, double[] b, char[] rel) {
        int m = A.length, n = A[0].length;
        double[][] A2 = new double[m][n];
        double[] b2 = new double[m];
        char[] r2 = new char[m];
        for (int i=0;i<m;i++){
            double bi = b[i]; char ri = rel[i];
            System.arraycopy(A[i], 0, A2[i], 0, n);
            if (bi < -EPS) {
                for (int j=0;j<n;j++) A2[i][j] = -A2[i][j];
                b2[i] = -bi;
                r2[i] = (ri=='<') ? '>' : (ri=='>') ? '<' : '=';
            } else { b2[i] = bi; r2[i] = ri; }
        }
        NormalizedSystem ns = new NormalizedSystem();
        ns.A = A2; ns.b = b2; ns.rel = r2; return ns;
    }

    private static BuildForm buildStandardForm(NormalizedSystem ns) {
        int m = ns.A.length, n = ns.A[0].length;
        int slacks=0, surplus=0, artificial=0;
        for (int i=0;i<m;i++){
            if (ns.rel[i]=='<') slacks++;
            else if (ns.rel[i]=='>') { surplus++; artificial++; }
            else if (ns.rel[i]=='=') { artificial++; }
        }
        int total = n + slacks + surplus + artificial;
        double[][] Aext = new double[m][total];
        boolean[] isArtificial = new boolean[total];

        int idxSlackStart = n;
        int idxSurpStart  = idxSlackStart + slacks;
        int idxArtStart   = idxSurpStart + surplus;

        int sCount=0, pCount=0, aCount=0;
        int[] basis = new int[m];

        for (int i=0;i<m;i++){
            System.arraycopy(ns.A[i], 0, Aext[i], 0, n);
            if (ns.rel[i]=='<'){
                int s = idxSlackStart + (sCount++);
                Aext[i][s] = 1.0;
                basis[i] = s;
            } else if (ns.rel[i]=='>'){
                int p = idxSurpStart + (pCount++);
                Aext[i][p] = -1.0; // surplus
                int a = idxArtStart + (aCount++);
                Aext[i][a] = 1.0;  // artificial
                isArtificial[a] = true;
                basis[i] = a;
            } else { // '='
                int a = idxArtStart + (aCount++);
                Aext[i][a] = 1.0;
                isArtificial[a] = true;
                basis[i] = a;
            }
        }

        BuildForm bf = new BuildForm();
        bf.Aext = Aext; bf.isArtificial = isArtificial; bf.totalVars = total; bf.initialBasis = basis;
        bf.nSlacks = slacks; bf.nSurplus = surplus; bf.nArtificial = artificial; bf.nOrig = n;
        return bf;
    }

    private static boolean runSimplex(PhaseState st, List<Iteration> stepsOut) {
        int m = st.A.length, nTot = st.totalVars;
        st.colNames = buildVarNames(st.nOrig, nTot, st.isArtificial);

        int iters = 0;
        while (iters++ < MAX_ITERS) {
            double[] r = reducedCosts(st);

            Iteration snap = new Iteration();
            snap.phase = st.phase; snap.reducedCosts = r.clone();
            snap.A = deepCopy2D(st.A); snap.b = st.b.clone();
            snap.basis = st.basis.clone(); snap.colNames = st.colNames.clone();
            snap.note = "Antes de pivoteo";
            stepsOut.add(snap);

            // Entrante (Bland)
            int enter = -1; for (int j=0;j<nTot;j++){ if (r[j] > EPS) { enter = j; break; } }
            if (enter == -1) return false; // óptimo

            // Saliente (razón mínima)
            int leave = -1; double minRatio = Double.POSITIVE_INFINITY;
            for (int i=0;i<m;i++){
                double aij = st.A[i][enter];
                if (aij > EPS) {
                    double ratio = st.b[i] / aij;
                    if (ratio < minRatio - 1e-12) { minRatio = ratio; leave = i; }
                }
            }
            if (leave == -1) {
                Iteration last = new Iteration();
                last.phase = st.phase; last.note = "No acotado (sin fila saliente).";
                stepsOut.add(last); return true; // unbounded
            }

            pivot(st, leave, enter);

            Iteration snap2 = new Iteration();
            snap2.phase = st.phase; snap2.entering = enter; snap2.leaving = leave;
            snap2.reducedCosts = reducedCosts(st);
            snap2.A = deepCopy2D(st.A); snap2.b = st.b.clone();
            snap2.basis = st.basis.clone(); snap2.colNames = st.colNames.clone();
            snap2.note = "Después de pivoteo";
            stepsOut.add(snap2);
        }

        Iteration last = new Iteration();
        last.phase = st.phase; last.note = "Máx. iteraciones alcanzado ("+MAX_ITERS+").";
        stepsOut.add(last); return false;
    }

    private static double[] reducedCosts(PhaseState st) {
        int m = st.A.length, nTot = st.totalVars;
        double[] Z = new double[nTot];
        for (int j=0;j<nTot;j++){
            double zj = 0.0; for (int i=0;i<m;i++) zj += st.CB[i] * st.A[i][j]; Z[j] = zj;
        }
        double[] r = new double[nTot];
        for (int j=0;j<nTot;j++) r[j] = st.C[j] - Z[j];
        return r;
    }

    private static void pivot(PhaseState st, int row, int col) {
        int m = st.A.length, nTot = st.totalVars;
        double piv = st.A[row][col];
        if (Math.abs(piv) < EPS) throw new IllegalStateException("Pivote ~ 0");

        for (int j=0;j<nTot;j++) st.A[row][j] /= piv;
        st.b[row] /= piv;

        for (int i=0;i<m;i++){
            if (i==row) continue;
            double factor = st.A[i][col];
            if (Math.abs(factor) > EPS) {
                for (int j=0;j<nTot;j++) st.A[i][j] -= factor * st.A[row][j];
                st.b[i] -= factor * st.b[row];
            }
        }

        st.basis[row] = col;
        st.CB[row] = st.C[col];
    }

    private static PhaseState removeArtificialColumns(PhaseState p1, BuildForm bf) {
        int m = p1.A.length, nTot = p1.totalVars;

        List<Integer> keep = new ArrayList<>();
        for (int j=0;j<nTot;j++) if (!bf.isArtificial[j]) keep.add(j);

        int newTot = keep.size();
        double[][] A2 = new double[m][newTot];
        boolean[] isArt2 = new boolean[newTot];
        for (int i=0;i<m;i++){
            int col=0;
            for (int j: keep){ A2[i][col] = p1.A[i][j]; isArt2[col]=false; col++; }
        }

        int[] basis2 = p1.basis.clone();
        for (int i=0;i<m;i++){
            int bcol = p1.basis[i];
            if (bcol >= 0 && bf.isArtificial[bcol]) {
                int newEnter = -1; int idx=0;
                for (int j: keep){ if (Math.abs(p1.A[i][j]) > EPS){ newEnter = idx; break; } idx++; }
                if (newEnter != -1){
                    double piv = A2[i][newEnter];
                    for (int j=0;j<newTot;j++) A2[i][j] /= piv;
                    p1.b[i] /= piv;
                    for (int r=0;r<m;r++){
                        if (r==i) continue;
                        double factor = A2[r][newEnter];
                        if (Math.abs(factor)>EPS){
                            for (int j=0;j<newTot;j++) A2[r][j] -= factor*A2[i][j];
                            p1.b[r] -= factor*p1.b[i];
                        }
                    }
                    basis2[i] = newEnter;
                } else {
                    basis2[i] = -1; // redundante
                }
            } else {
                basis2[i] = keep.indexOf(bcol);
            }
        }

        PhaseState p2 = new PhaseState();
        p2.A = A2; p2.b = p1.b.clone();
        p2.totalVars = newTot; p2.basis = basis2;
        p2.nOrig = p1.nOrig; p2.isArtificial = isArt2;
        return p2;
    }

    private static String[] buildVarNames(int nOrig, int totalVars, boolean[] isArtificial) {
        String[] names = new String[totalVars];
        int idx = 0; for (; idx<nOrig; idx++) names[idx] = "x" + (idx+1);
        int s=1, a=1;
        for (; idx<totalVars; idx++) {
            if (isArtificial!=null && idx<isArtificial.length && isArtificial[idx]) names[idx] = "a" + (a++);
            else names[idx] = "s" + (s++);
        }
        return names;
    }

    private static double[][] deepCopy2D(double[][] M) {
        if (M == null) return null;
        double[][] C = new double[M.length][];
        for (int i=0;i<M.length;i++) C[i] = M[i].clone();
        return C;
    }
}

