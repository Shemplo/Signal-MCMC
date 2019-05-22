package ru.shemplo.metagennet;

import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.GraphDescriptor;
import ru.shemplo.metagennet.graph.GraphSignals;
import ru.shemplo.metagennet.graph.Vertex;
import ru.shemplo.metagennet.io.CommonWriter;
import ru.shemplo.metagennet.io.GraphReader;
import ru.shemplo.metagennet.io.MelanomaAdvGraphReader;
import ru.shemplo.metagennet.mcmc.MCMC;
import ru.shemplo.metagennet.mcmc.MCMCJoinOrLeave;
import ru.shemplo.snowball.stuctures.Pair;

public class RunMetaGenMCMC {
    
    public static final int TAU_E_N = 1, TAU_V_N = 11;
    
    public static final Random RANDOM = new Random ();
    
    private static final boolean SIGNALS = true, LONG_RUN = !true;
    private static final int TRIES = 100, ITERATIONS = 250000;
    private static final boolean REGENERATE = true;
    private static final boolean TRACE = !true;
    private static final int MODULE_SIZE = 1;
    
    
    private static final BiFunction <GraphDescriptor, Integer, MCMC> SUPPLIER = 
        (graph, iterations) -> new MCMCJoinOrLeave (graph, iterations);
    
    private static final ExecutorService pool = Executors.newFixedThreadPool (REGENERATE ? 1 : 2);
    private static final Map <Vertex, Double> occurrences = new HashMap <> ();
    private static final List <Double> likelihoods = new ArrayList <> ();
    private static final CommonWriter writer = new CommonWriter ();
    
    public static void main (String ... args) throws IOException, InterruptedException {
        Locale.setDefault (Locale.ENGLISH);
        
        final GraphReader reader      = new MelanomaAdvGraphReader ();
        Graph initial = reader.readGraph ("mapped_melanoma_gwas.txt");
        
        //final GraphReader reader = new CSVGraphReader ();
        //Graph initial = reader.readGraph ("paper_");
        
        //GraphReader reader = new GWASGraphReader ();
        //Graph initial = reader.readGraph ("");
        System.out.println ("Vertecies: " + initial.getVertices ().size ());
        System.out.println ("Signals: " + initial.getSignals ()
                            . getSignals ().values ().stream ()
                            . distinct ().count ());
        System.out.println ("Graph loaded");
        
        initial.getVertices ().stream ()
        . filter  (Objects::nonNull)
        . forEach (vertex -> occurrences.put (vertex, 0D));
        
        /*
        List <Double> aaa = initial.getEdgesList ().stream ()
                          . map     (Edge::getWeight)
                          . collect (Collectors.toList ());
        System.out.println (aaa);
        
        List <Double> bbb = initial.getVertices ().stream ()
                          . map     (Vertex::getWeight)
                          . collect (Collectors.toList ());
        System.out.println (bbb);
        */
       
        try (
            PrintWriter pw = new PrintWriter (new File ("runtime/temp.dot"));
        ) {
            @SuppressWarnings ("unused")
            GraphDescriptor full = initial.getFullDescriptor (false);
            System.out.println (initial.getFixedDescriptor (30, SIGNALS));
            //pw.println (full.toDot ());
        }
        
        for (int i = 0; i < TRIES; i++) {
            /*
            descriptor.getVertices ().forEach (vertex -> {
                vertex.getEdges ().keySet ().forEach (nei -> {
                    System.out.print (nei.getName () + ", ");
                    //System.out.print (" (" + nei.getId () + ") ");
                });
                System.out.println ();
            });
            //System.out.println (descriptor.getOuterEdges (true));
             */
            
            pool.execute (() -> {
                try {
                    if (REGENERATE) {
                        initial.regenerateVerticesWeight (vertex -> {
                            double weight = RANDOM.nextDouble () * RANDOM.nextDouble ();
                            return vertex.isStable () ? vertex.getWeight () : weight;
                        });
                        
                        initial.setSignals (GraphSignals.splitGraph (initial));
                    }
                    
                    GraphDescriptor descriptor = initial.getFixedDescriptor (MODULE_SIZE, SIGNALS);
                    System.out.println ("Initial descriptor:");
                    System.out.println (descriptor);
                    
                    long start = System.currentTimeMillis ();
                    MCMC singleRun = SUPPLIER.apply (descriptor, ITERATIONS);
                    singleRun.doAllIterations (false, TRACE);
                    
                    long end = System.currentTimeMillis ();
                    System.out.println (String.format ("Run finished in `%s` (time: %.3fs, starts: %d, commits: %d)", 
                                              Thread.currentThread ().getName (), (end - start) / 1000d, 
                                              singleRun.getStarts (), singleRun.getCommits ()));
                    registerRun (singleRun, initial);
                } catch (Exception e) {
                    System.out.println ("Oooops");
                    e.printStackTrace ();
                    registerRun (null, initial);
                }
            });
        }
        
        pool.awaitTermination (1, TimeUnit.DAYS);
    }
    
    private static int registered = 0;
    
    private static synchronized void registerRun (MCMC singleRun, Graph graph) {
        int size = -1;
        if (singleRun != null) {
            GraphDescriptor graphd = singleRun.getCurrentGraph ();
            graphd.getVertices ().forEach (vertex -> 
                occurrences.compute (vertex, (___, v) -> v == null ? 1 : v + 1)
            );
            //System.out.println (i + " " + graph);
            
            if (LONG_RUN) {
                singleRun.getSnapshots ().forEach (gph -> {
                    gph.forEach (vertex -> 
                        occurrences.compute (vertex, (___, v) -> v == null ? 1 : v + 1)
                    );
                });
            }
            
            if (singleRun.getLikelihoods ().size () > 0) {
                likelihoods.clear (); // truncate previous result
                likelihoods.addAll (singleRun.getLikelihoods ());
            }
            
            size = singleRun.getCurrentGraph ().getVertices ().size ();
            
            try (
                OutputStream os = new FileOutputStream ("runtime/tmp.log", true);
                PrintWriter pw = new PrintWriter (os);
            ) {
                pw.print   (String.format ("#%04d ", registered + 1));
                pw.println (graphd.toVerticesString ());
            } catch (IOException ioe) {}
        }
        
        System.out.println (String.format ("    %04d registered at ___ %s (verticies: %d)", 
                                           registered + 1, new Date ().toString (), size));
        
        if (++registered == TRIES) {
            printResults (graph); pool.shutdown ();
        }
    }
    
    private static void printResults (Graph graph) {
        writer.saveMap ("runtime/mcmc_frequences.tsv", "frequency", occurrences, graph);
        
        double inRun = LONG_RUN ? Math.max (1, ITERATIONS * 0.9 / 100) + 1 : 1;
        occurrences.keySet ().forEach (key -> {
            occurrences.compute (key, (__, v) -> v / (TRIES * inRun));
        });
        
        if (SIGNALS) {            
            writer.saveNontrivialSignalsMap ("runtime/mcmc_nontrivial.tsv", 
                                        "probability", occurrences, graph);
        }
        writer.saveMap ("runtime/mcmc_results.tsv", "probability", occurrences, graph);
        
        Set <String> orientier = graph.getOrientier ();
        if (orientier.size () > 0) {
            List <Pair <Vertex, Double>> matched = occurrences.entrySet ().stream ()
                                                 . map     (Pair::fromMapEntry)
                                                 . sorted  ((a, b) -> -Double.compare (a.S, b.S))
                                                 . limit   ((int) (orientier.size () * 1.5))
                                                 . filter  (pair -> orientier.contains (pair.F.getName ()))
                                                 //. forEach (pair -> System.out.println (pair.F.getName ()));
                                                 . collect (Collectors.toList ());
            StringJoiner sj = new StringJoiner (", ");
            matched.forEach (pair -> sj.add (String.format ("%s {p: %.4f}", pair.F.getName (), pair.S)));
            System.out.println ("(" + matched.size () + " / " + orientier.size () + ") " + sj.toString ());
            
            GraphDescriptor finalD = graph.getFinalDescriptor (orientier.size (), occurrences);
            writer.saveGradientDOTFile ("runtime/final.dot", finalD, occurrences, Color.YELLOW, Color.GRAY);
        }
        
        if (likelihoods.size () > 0) {
            writer.saveLikelihoods ("runtime/likelihoods.csv", likelihoods);
        }
    }
    
}
