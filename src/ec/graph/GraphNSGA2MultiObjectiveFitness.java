package ec.graph;

import ec.EvolutionState;
import ec.multiobjective.MultiObjectiveFitness;
import ec.multiobjective.nsga2.NSGA2MultiObjectiveFitness;
import ec.util.Parameter;

public class GraphNSGA2MultiObjectiveFitness extends NSGA2MultiObjectiveFitness {
	private static final long serialVersionUID = 1L;
	public int[] objRankings;

	@Override
    public void setup(EvolutionState state, Parameter base){
		super.setup(state,base);
		Parameter def = defaultBase();
		int numFitnesses = state.parameters.getInt(base.push(P_NUMOBJECTIVES), def.push(P_NUMOBJECTIVES), 0);
		objRankings = new int[numFitnesses];
	}

	@Override
	public Object clone() {
		GraphNSGA2MultiObjectiveFitness fit = (GraphNSGA2MultiObjectiveFitness) super.clone();
		fit.objRankings = (int[]) objRankings.clone();
		return fit;
	}
}
