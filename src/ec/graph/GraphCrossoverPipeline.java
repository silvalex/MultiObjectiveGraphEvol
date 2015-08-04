package ec.graph;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

/**
 * 
 * @author Alex
 */
public class GraphCrossoverPipeline extends BreedingPipeline {

    @Override
    public Parameter defaultBase() {
        return new Parameter("graphcrossoverpipeline");
    }

    @Override
    public int numSources() {
        return 2;
    }

    @Override
    public int produce(int min, int max, int start, int subpopulation,
            Individual[] inds, EvolutionState state, int thread) {
        // TODO Auto-generated method stub
        return 0;
    }

}
