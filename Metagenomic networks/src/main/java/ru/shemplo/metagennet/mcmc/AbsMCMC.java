package ru.shemplo.metagennet.mcmc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.metagennet.graph.Edge;
import ru.shemplo.metagennet.graph.GraphDescriptor;
import ru.shemplo.metagennet.graph.Vertex;

@RequiredArgsConstructor
public abstract class AbsMCMC implements MCMC {

    @Getter protected GraphDescriptor currentGraph;
    protected final GraphDescriptor initialGraph;
    protected List <Edge> initialGraphEdges;
    
    @Getter protected int starts = 0, commits = 0;
    protected final int iterations;
    protected int iteration = 0;
    
    @Getter
    protected final List <Set <Vertex>> snapshots = new ArrayList <> ();
    
    @Override
    public boolean finishedWork () {
        return iteration >= iterations;
    }
    
    @Override
    public void doAllIterations (boolean idling) {
        if (!idling) {
            //System.out.println (initialGraph.toVerticesString ());
            //System.out.println (initialGraph.getEdges ());
        }
        
        //System.out.println ("Start");
        double parts = 0.0;
        while (!finishedWork ()) {
            starts += 1; // it's a start - not iteration
            makeIteration (idling);
            
            if (!idling && iteration >= iterations * (parts + 0.1)) {
                String thread = Thread.currentThread ().getName (); parts += 0.1;
                int verts = currentGraph.getVertices ().size ();
                System.out.println (String.format ("Heart bit of `%s` - %.1f (verticies: %d)", 
                                                   thread, parts, verts));
            }
            /*
            if (!idling && currentGraph.getBedges ().size () > 5000) {
                String thread = Thread.currentThread ().getName ();
                System.out.println ("!!! To many beges in " + thread);
            }
            */
            /*
            if (!idling && starts - iteration > iterations * 0.5) {
                String thread = Thread.currentThread ().getName ();
                System.out.println ("!!! To many fails in " + thread + " " + (starts - iteration) + " " + iteration);
            }
            */
            
            int limit = (int) (iterations * 0.9);
            if (iteration >= limit && iteration % 100 == 0) {
                snapshots.add (new HashSet <> (currentGraph.getVertices ()));
                //System.out.println (currentGraph.toVerticesString ());
                //System.out.println (currentGraph);
            }
            //System.out.println (currentGraph.getEdges ());
        }
        
        if (!idling) {
            //System.out.println (currentGraph.toVerticesString ());
            //System.out.println (currentGraph.getEdges ());
        }
    }
    
}
