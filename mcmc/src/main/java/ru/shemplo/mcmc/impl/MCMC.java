package ru.shemplo.mcmc.impl;

import java.util.List;
import java.util.Set;

import ru.shemplo.mcmc.graph.GraphDescriptor;
import ru.shemplo.mcmc.graph.Vertex;

public interface MCMC {
    
    public void doAllIterations (boolean idling, boolean trace);
    
    public boolean finishedWork ();
    
    public GraphDescriptor getCurrentGraph ();
    
    public List <Set <Vertex>> getSnapshots ();
    
    public List <Double> getLikelihoods ();
    
    public void makeIteration (boolean idling);
    
    public int getStarts ();
    
    public int getCommits ();
    
}
