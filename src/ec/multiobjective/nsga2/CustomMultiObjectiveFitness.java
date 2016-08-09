package ec.multiobjective.nsga2;

import ec.graph.GraphInitializer;

public class CustomMultiObjectiveFitness extends NSGA2MultiObjectiveFitness {
	public CustomMultiObjectiveFitness() {
		super();
		super.objectives = new double[4];

		super.maximize = new boolean[4];
		super.maximize[GraphInitializer.AVAILABILITY] = true;
		super.maximize[GraphInitializer.RELIABILITY] = true;
		super.maximize[GraphInitializer.TIME] = false;
		super.maximize[GraphInitializer.COST] = false;

		super.maxObjective = new double[]{1.0, 1.0, 1.0, 1.0};
		super.minObjective = new double[]{0.0, 0.0, 0.0, 0.0};

	}

}
