package ru.shemplo.metagennet.io;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.GraphSignals;
import ru.shemplo.snowball.stuctures.Pair;

public class GWASGraphReader extends MelanomaGraphReader {
    
    @Override
    public Graph readGraph (String filenamePrefix) throws IOException {
        Map <String, Double> genesDesc = readGenes ("runtime/gwas.csv", 0);
        List <Pair <String, String>> edgesDesc = readEdges ();
        Graph graph = new Graph (0.571, 1.0);
        
        edgesDesc = edgesDesc.stream ()
                  . filter  (pair -> genesDesc.containsKey (pair.F)
                                  && genesDesc.containsKey (pair.S))
                  . collect (Collectors.toList ());
        List <String> genesVerts = edgesDesc.stream ()
                                 . flatMap  (pair -> Stream.of (pair.F, pair.S))
                                 . distinct ().sorted ()
                                 . collect  (Collectors.toList ());
        Map <String, Integer> gene2index = new HashMap <> ();
        for (int i = 0; i < genesVerts.size (); i++) {
            String name = genesVerts.get (i);
            graph.addVertex (i, genesDesc.get (name))
                 .setName (name);
            gene2index.put (name, i);
        }
        
        Set <String> remove = new HashSet <> (Arrays.asList ("UBC"));
        
        for (Pair <String, String> edge : edgesDesc) {
            if (remove.contains (edge.F) || remove.contains (edge.S)) {
                continue;
            }
            
            graph.addEdge (gene2index.get (edge.F), gene2index.get (edge.S), 1.0);
        }
        
        graph.setSignals (GraphSignals.splitGraph (graph));
        
        graph.getOrientier ().addAll (Arrays.asList (
            "CDKN2A", "MTAP", "MX2", "PARP1", "ARNT", "SETDB1",
            "ATM", "CCND1", "PLA2G6", "RAD23B", "TMEM38B", "FT0",
            "AGR3", "RMDN2", "CASP8", "CDKAL1", "DIP2B", "EBF3",
            "PPP1R15A", "CTR9", "ZNF490", "B2M", "GRAMD3",
            "TOR1AIP1", "MTTP", "ATRIP", "TAF1", "ELAVL1"
        ));
        return graph;
    }
    
}
