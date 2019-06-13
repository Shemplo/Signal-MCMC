package ru.shemplo.mcmc;

import static java.lang.Math.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import ru.shemplo.mcmc.graph.Edge;
import ru.shemplo.mcmc.graph.Graph;
import ru.shemplo.mcmc.graph.GraphDescriptor;
import ru.shemplo.mcmc.graph.Vertex;

public class RunGraphGenerator {
    
    public static final double BETA_A_V = 0.363, BETA_B_V = 1;
    public static final double BETA_A_E = 0.431, BETA_B_E = 1;
    public static final int VERTS = 500, VERTS_DEV = 5; // deviation
    public static final int EDGES = (VERTS - 1) * 2 + 100,
                            EDGES_DEV = 2;
    public static final int MODULE_SIZE = 20;
    
    private static final Random R = new Random ();
    
    public static void main (String ... args) throws IOException {
        Locale.setDefault (Locale.ENGLISH);
        Graph graph = new Graph (0, 0);
        
        //
        // Stage 0 - initialization of vertices
        //
        Map <Vertex, Set <Integer>> sds = new HashMap <> (); // system of disjoint sets
        int vertsN = VERTS + VERTS_DEV - R.nextInt (2 * VERTS_DEV + 1);
        for (int i = 0; i < vertsN; i++) {
            Vertex vertex = new Vertex (i, R.nextDouble ());
            graph.addVertex (vertex);
            sds.put (vertex, new HashSet <> ());
        }
        
        //
        // Stage 0.5 - initialization of tree
        //
        int sets = sds.size ();
        while (sets != 1) {
            Vertex a = graph.getVertices ().get (R.nextInt (vertsN)),
                   b = graph.getVertices ().get (R.nextInt (vertsN));
            if (!a.equals (b) && !sds.get (a).contains (b.getId ())) {
                //System.out.println (a + " -> " + b + " / " + sets);
                final Edge edge = new Edge (a, b, R.nextDouble ());
                Set <Integer> setA = sds.get (a);
                setA.addAll (sds.get (b));
                
                sds.get (b).forEach (vertex -> {
                    sds.put (graph.getVertices ().get (vertex), setA);
                });
                
                graph.addEdge (edge);
                sets -= 1;
            } else if (!a.equals (b)) {
                System.out.println (a + " ~ " + b);
            }
        }
        
        System.out.println (graph.getFullDescriptor (true));
        
        //
        // Stage 1 - additional edges
        //
        int edgesN = EDGES - 2 * R.nextInt (EDGES_DEV * 2 + 1);
        while (graph.getEdges ().size () < edgesN) {
            int a = 0, b = 0, bound = graph.getVertices ().size ();
            while (a == b) {
                a = R.nextInt (bound); b = R.nextInt (bound);
            }
            
            Vertex va = graph.getVertices ().get (a),
                   vb = graph.getVertices ().get (b);
            if (!va.getEdges ().containsKey (vb)) {
                Edge edge = new Edge (va, vb, R.nextDouble ());
                graph.addEdge (edge);
            }
        }
        
        System.out.println (graph.getFullDescriptor (true));
        System.out.println ("Grpah generated");
        
        //
        // Stage 2 - selection signal module
        // 
        GraphDescriptor module = graph.getFixedDescriptor (MODULE_SIZE, false);
        
        System.out.println (module);
        
        //
        // Stage 3 - beta distribution in module
        //
        module.getVertices ().forEach (vertex -> {
            vertex.setWeight (nextBetaDouble (BETA_A_V, BETA_B_V));
        });
        
        module.getEdges ().forEach (edge -> {
            edge.setWeight (nextBetaDouble (BETA_A_E, BETA_B_E));
        });
        
        //
        // Stage 4 - publishing graph
        //
        Path path = Paths.get ("runtime/generated_vertices.csv");
        try (
            PrintWriter pw = new PrintWriter (path.toFile ());
        ) {
            pw.println ("\"name\";\"pval\";\"answer\"");
            
            GraphDescriptor full = graph.getFullDescriptor (false);
            full.getVertices ().forEach (vertex -> {
                boolean inModule = module.getVertices ().contains (vertex);
                pw.println (String.format ("\"%s\";%e;%d", vertex.getName (), 
                            vertex.getWeight (), inModule ? 1 : 0));
            });
        }
        
        path = Paths.get ("runtime/generated_edges.csv");
        try (
            PrintWriter pw = new PrintWriter (path.toFile ());
        ) {
            pw.println ("\"from\";\"to\";\"weight\";\"answer\"");
            
            GraphDescriptor full = graph.getFullDescriptor (false);
            full.getEdges ().forEach (edge -> {
                boolean inModule = module.getEdges ().contains (edge);
                pw.println (String.format ("\"%s\";\"%s\";%e;%d", edge.getF ().getName (), 
                            edge.getS ().getName (), edge.getWeight (), 
                            inModule ? 1 : 0));
            });
        }
    }
    
    public static double nextBetaDouble (double a, double b) {
        double s1 = 1, s2 = 1;
        while (s1 + s2 > 1 || s1 + s2 == 0.0) {
            s1 = nthroot (a, R.nextDouble ());
            s2 = nthroot (b, R.nextDouble ());
        }
        
        return s1 / (s1 + s2);
    }
    
    public static double nthroot (double root, double value) {
        return signum (value) * pow (abs (value), 1.0 / root);
    }
    
}
