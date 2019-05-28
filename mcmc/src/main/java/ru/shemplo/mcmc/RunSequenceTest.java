package ru.shemplo.mcmc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

public class RunSequenceTest {
    
    public static void main (String ... args) throws IOException, InterruptedException {
        /*
        for (int i = 0; i < 30; i ++) {
            RunGraphGenerator.main ();
            RunMCMC.main ();
            
            Map <String, Integer> answers = new HashMap <> ();
            Path pathGen = Paths.get ("runtime/generated_vertices.csv");
            try (
                BufferedReader br = Files.newBufferedReader (pathGen);
            ) {
                br.readLine (); // it's headers
                
                String line = null;
                while ((line = br.readLine ()) != null) {
                    String [] tokens = line.split (";");
                    String name = tokens [0].replace ("\"", "");
                    System.out.println (name);
                }
            }
             
            Path pathRes = Paths.get ("runtime/mcmc_results.tsv");
            try (
                BufferedReader br = Files.newBufferedReader (pathRes);
            ) {
                String header = br.readLine ();
                
                
            }
        }
        */
        
        Set <String> verts = new HashSet <> ();
        Path pathGen = Paths.get ("runtime/mcmc_results_small.tsv");
        try (
            BufferedReader br = Files.newBufferedReader (pathGen);
        ) {
            br.readLine (); // it's headers
            
            String line = null;
            while ((line = br.readLine ()) != null) {
                StringTokenizer st = new StringTokenizer (line);
                verts.add (st.nextToken ());
            }
        }
        
        Path pathRes = Paths.get ("runtime/inwebIM_ppi.txt");
        Random r = new Random ();   
        try (
            BufferedReader br = Files.newBufferedReader (pathRes);
            PrintWriter pw = new PrintWriter ("runtime/web_small.tsv");
        ) {
            String line = null;
            while ((line = br.readLine ()) != null) {
                StringTokenizer st = new StringTokenizer (line);
                String a = st.nextToken (), b = st.nextToken ();
                
                if (verts.contains (a) && verts.contains (b)
                        && r.nextDouble () < 0.2) {
                    pw.println (a + "\t" + b);
                }
            }
        }
    }
    
}
