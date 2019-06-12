package ru.shemplo.mcmc;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import ru.shemplo.mcmc.graph.*;
import ru.shemplo.mcmc.impl.MCMC;
import ru.shemplo.mcmc.impl.MCMCJoinOrLeave;
import ru.shemplo.mcmc.io.CSVGraphReader;
import ru.shemplo.mcmc.io.GraphReader;

public class RunMCMCStatistics {
    
    public static final Random RANDOM = new Random ();
    
    private static final int TRIES = 1, ITERATIONS = 1000000;
    private static final boolean REGENERATE = true;
    private static final boolean SIGNALS = !true;
    private static final boolean TRACE = !true;
    private static final int MODULE_SIZE = 1;
    
    private static final BiFunction <GraphDescriptor, Integer, MCMC> SUPPLIER = 
        (graph, iterations) -> new MCMCJoinOrLeave (graph, iterations);
    
    private static final ExecutorService pool = Executors.newFixedThreadPool (REGENERATE ? 1 : 5);
    
    public static void main (String ... args) throws IOException, InterruptedException {
        Locale.setDefault (Locale.ENGLISH);
        
        final GraphReader reader = new CSVGraphReader ();
        Graph initial = reader.readGraph ("generated_");
        
        System.out.println ("Vertecies: " + initial.getVertices ().size ());
        System.out.println ("Signals: " + initial.getSignals ()
                            . getSignals ().values ().stream ()
                            . distinct ().count ());
        System.out.println ("Graph loaded");
        
        for (int i = 0; i < TRIES; i++) {
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
        Set <Edge> moduleEdges = new HashSet <> (Arrays.asList (
            new Edge (new Vertex (72, 0D), new Vertex (90, 0D), 0),
            new Edge (new Vertex (1, 0D), new Vertex (4, 0D), 0),
            new Edge (new Vertex (1, 0D), new Vertex (62, 0D), 0),
            new Edge (new Vertex (4, 0D), new Vertex (44, 0D), 0),
            new Edge (new Vertex (61, 0D), new Vertex (62, 0D), 0),
            new Edge (new Vertex (44, 0D), new Vertex (90, 0D), 0),
            new Edge (new Vertex (62, 0D), new Vertex (72, 0D), 0),
            new Edge (new Vertex (62, 0D), new Vertex (75, 0D), 0),
            new Edge (new Vertex (46, 0D), new Vertex (62, 0D), 0),
            new Edge (new Vertex (1, 0D), new Vertex (25, 0D), 0),
            new Edge (new Vertex (21, 0D), new Vertex (46, 0D), 0),
            new Edge (new Vertex (44, 0D), new Vertex (92, 0D), 0),
            new Edge (new Vertex (21, 0D), new Vertex (36, 0D), 0),
            new Edge (new Vertex (18, 0D), new Vertex (75, 0D), 0),
            new Edge (new Vertex (29, 0D), new Vertex (72, 0D), 0),
            new Edge (new Vertex (11, 0D), new Vertex (36, 0D), 0),
            new Edge (new Vertex (4, 0D), new Vertex (12, 0D), 0),
            new Edge (new Vertex (35, 0D), new Vertex (36, 0D), 0),
            new Edge (new Vertex (12, 0D), new Vertex (88, 0D), 0),
            new Edge (new Vertex (41, 0D), new Vertex (61, 0D), 0)
        ));
        
        int size = -1;
        if (singleRun != null) {
            GraphDescriptor graphd = singleRun.getCurrentGraph ();
            
            int prevDistance = 100000;
            int better = 0, same = 0, worse = 0;
            Map <Integer, double []> freqs = new HashMap <> ();
            for (int i = 0; i < graphd.getSnapshots ().size (); i++) {
                Set <Edge> snapshot = graphd.getSnapshots ().get (i);
                int edges = snapshot.size ();
                snapshot.retainAll (moduleEdges);
                
                int distance = moduleEdges.size () + edges - 2 * snapshot.size ();
                freqs.putIfAbsent (distance, new double [4]);
                if (distance < prevDistance) {
                    freqs.get (distance) [0] ++;
                    freqs.get (distance) [3] ++;
                    better ++;
                } else if (distance > prevDistance) {
                    freqs.get (distance) [2] ++;
                    freqs.get (distance) [3] ++;
                    worse ++;
                } else {
                    freqs.get (distance) [1] ++;
                    freqs.get (distance) [3] ++;
                    same ++;
                }
                
                prevDistance = distance;
            }
            
            size = singleRun.getCurrentGraph ().getVertices ().size ();
            double sum = better + same + worse;
            System.out.println (String.format ("Total: better %d (%.3f), same %d (%.3f), worse %d (%.3f)", 
                    better, better / sum, same, same / sum, worse, worse / sum));
            freqs.keySet ().stream ()
            . sorted  (Comparator.comparing (__ -> __))
            . forEach (distance -> {
                double [] array = freqs.get (distance);
                System.out.println (String.format ("%d:   better %4d (%.3f),   same %6d (%.3f),   worse %4d (%.3f)", 
                                    distance, (int) array [0], array [0] / array [3], (int) array [1], 
                                    array [1] / array [3], (int) array [2], array [2] / array [3]));
            });
        }
        
        System.out.println (String.format ("    %04d registered at ___ %s (verticies: %d)", 
                                           registered + 1, new Date ().toString (), size));
        
        if (++registered == TRIES) {
            pool.shutdown ();
        }
    }
    
}
