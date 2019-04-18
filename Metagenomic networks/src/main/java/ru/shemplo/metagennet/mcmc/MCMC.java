package ru.shemplo.metagennet.mcmc;

import java.util.List;
import java.util.Set;

import ru.shemplo.metagennet.graph.GraphDescriptor;
import ru.shemplo.metagennet.graph.Vertex;

public interface MCMC {
    
    public void doAllIterations (boolean idling);
    
    public boolean finishedWork ();
    
    public GraphDescriptor getCurrentGraph ();
    
    public List <Set <Vertex>> getSnapshots ();
    
    public void makeIteration (boolean idling);
    
    public int getStarts ();
    
    public int getCommits ();
    
}
