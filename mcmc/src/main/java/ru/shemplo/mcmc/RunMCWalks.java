package ru.shemplo.mcmc;

import java.util.Random;

public class RunMCWalks {
    
    /*
    private static final double [][] matrix = {
        {1,   0,   0,   0,   0,   0,   0,   0,   0,   0},
        {0.3, 0.6, 0.1, 0,   0,   0,   0,   0,   0,   0},
        {0,   0.3, 0.6, 0.1, 0,   0,   0,   0,   0,   0},
        {0,   0,   0.3, 0.6, 0.1, 0,   0,   0,   0,   0},
        {0,   0,   0,   0.3, 0.6, 0.1, 0,   0,   0,   0},
        {0,   0,   0,   0,   0.3, 0.6, 0.1, 0,   0,   0},
        {0,   0,   0,   0,   0,   0.3, 0.6, 0.1, 0,   0},
        {0,   0,   0,   0,   0,   0,   0.3, 0.6, 0.1, 0},
        {0,   0,   0,   0,   0,   0,   0,   0.3, 0.6, 0.1},
        {0,   0,   0,   0,   0,   0,   0,   0,   0.3, 1 - 0.3},
    };
    */
    
    private static double a = 0.4, b = 0.5, c = 1 - a - b;
    private static int size = 9;
    
    private static double [][] matrix = null;
    
    private static int stateS = size - 1, // start
                       stateD = 0;        // destination
    private static final int runs = 100000;
    
    public static void main (String ... args) {
        final Random r = new Random ();
        
        matrix = new double [size][size];
        matrix [size - 1][size - 1] = 1 - a;
        matrix [size - 1][size - 2] = a;
        matrix [0][0] = 1;
        
        for (int i = 1; i < size - 1; i++) {
            matrix [i][i - 1] = a;
            matrix [i][i]     = b;
            matrix [i][i + 1] = c;
        }
        
        double totalIterations = 0;
        for (int run = 0; run < runs; run++) {
            int it = 0, state = stateS;
            while (state != stateD) {
                double p = r.nextDouble ();
                for (int i = 0; i < matrix [state].length; i++) {
                    if (matrix [state][i] >= p && p != 0) {
                        state = i;
                        break;
                    } else {
                        p -= matrix [state][i];
                    }
                }
                
                it++;
            }
            
            totalIterations += it * 1.0;
        }
        
        System.out.println (totalIterations / runs);
    }
    
}
