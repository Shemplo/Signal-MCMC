package ru.shemplo.metagennet.io;

import java.io.IOException;

import ru.shemplo.metagennet.graph.Graph;

public interface GraphReader {
    
    public Graph readGraph (String filename) throws IOException;
    
}
