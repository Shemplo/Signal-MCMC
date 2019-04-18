package ru.shemplo.metagennet.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.snowball.utils.StringManip;

public class AdjMatrixGraphReader implements GraphReader {
    
    @Override
    public Graph readGraph (String __) throws IOException {
        final Path path = Paths.get ("runtime/graph.csv");
        List <List <Double>> matrix = new ArrayList <> ();
        try (
            BufferedReader br = Files.newBufferedReader (path);
        ) {
            String line = null;
            
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                matrix.add (
                    Arrays.asList (line.split (";")).stream ()
                    . map     (Double::parseDouble)
                    . collect (Collectors.toList ())
                );
            }
        }
        
        Graph graph = new Graph (0.133, 0.195);
        for (int i = 0; i < matrix.size (); i++) {
            graph.addVertex (i, matrix.get (i).get (i));
        }
        
        for (int i = 0; i < matrix.size (); i++) {
            for (int j = i + 1; j < matrix.size (); j++) {
                double weight = matrix.get (i).get (j);
                if (weight > 0) {
                    graph.addEdge (i, j, weight);
                }
            }
        }
        
        return graph;
    }
    
}
