package ru.shemplo.mcmc.io;

import java.io.IOException;

import ru.shemplo.mcmc.graph.Graph;

public interface GraphReader {
    
    public Graph readGraph (String filename) throws IOException;
    
}
