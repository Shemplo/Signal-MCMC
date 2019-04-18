package ru.shemplo.metagennet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.Vertex;
import ru.shemplo.metagennet.io.GraphReader;
import ru.shemplo.metagennet.io.MelanomaGraphReader;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class RunSignalsFix {
    
    public static void main (String ... args) throws IOException {
        Locale.setDefault (Locale.ENGLISH);
        
        GraphReader reader = new MelanomaGraphReader ();
        Graph initial = reader.readGraph ("");
        Map <String, Double> probs = new LinkedHashMap <> ();
        
        Path filepath = Paths.get ("runtime/mcmc_results.txt");
        try (
            BufferedReader br = Files.newBufferedReader (filepath);
        ) {
            br.readLine (); // headers
            
            String line = null;
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                String [] tokens = line.trim ().split ("\\s+");
                probs.put (tokens [0], Double.parseDouble (tokens [3]));
            }
        }
        
        try (
            PrintWriter pw = new PrintWriter (filepath.toFile ());
        ) {            
            pw.println ("gene\tpval\tsignal\tprobability");
            probs.entrySet ().stream ()
            . map     (Pair::fromMapEntry)
            . sorted  ((a, b) -> -Double.compare (a.S, b.S))
            . forEach (pair -> {
                Vertex vertex = initial.getVertexByName (pair.F);
                System.out.println (vertex + " " + initial.getSignals ().getSignal (vertex));
                int signal = initial.getSignals ().getSignal (vertex).getId ();
                pw.println (String.format ("%9s\t%.12f\t%6d\t%.6f", vertex.getName (), 
                                                vertex.getWeight (), signal, pair.S));
            });
        }
    }
    
}
