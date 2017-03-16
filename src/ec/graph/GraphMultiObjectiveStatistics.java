package ec.graph;

import java.util.ArrayList;

import ec.EvolutionState;
import ec.Individual;
import ec.multiobjective.MultiObjectiveFitness;
import ec.multiobjective.MultiObjectiveStatistics;
import ec.multiobjective.nsga2.NSGA2MultiObjectiveFitness;
import ec.util.QuickSort;
import ec.util.SortComparator;

public class GraphMultiObjectiveStatistics extends MultiObjectiveStatistics {
	public long lastTime;

	public long initTime;
	public long breedingTime;
	public long evaluationTime;

	private static final long serialVersionUID = 1L;

	@Override
    public void preInitializationStatistics(final EvolutionState state){
	    super.preInitializationStatistics(state);
	    lastTime = System.currentTimeMillis();
    }

    @Override
    public void postInitializationStatistics(final EvolutionState state) {
	    super.postInitializationStatistics(state);
	    initTime = System.currentTimeMillis()-lastTime;
    }

	@Override
    public void preBreedingStatistics(final EvolutionState state) {
    	super.preBreedingStatistics(state);
        lastTime = System.currentTimeMillis();
    }

	@Override
    public void postBreedingStatistics(final EvolutionState state) {
	    super.postBreedingStatistics(state);
        breedingTime = System.currentTimeMillis()-lastTime;
    }

	@Override
    public void preEvaluationStatistics(final EvolutionState state) {
		super.preEvaluationStatistics(state);
        lastTime = System.currentTimeMillis();
    }

    @Override
	public void postEvaluationStatistics(final EvolutionState state) {
    	evaluationTime = System.currentTimeMillis()-lastTime;

		for (int x = 0; x < state.population.subpops.length; x++) {
			for (int y = 0; y < state.population.subpops[x].individuals.length; y++) {
				GraphIndividual ind = (GraphIndividual) state.population.subpops[x].individuals[y];
				StringBuilder builder = new StringBuilder();

				builder.append(state.generation);
				builder.append(" ");
				builder.append(y); // Individual
				builder.append(" ");
				// If it is first generation, also count initialization time
				if (state.generation == 0) {
					builder.append(initTime + breedingTime);
					builder.append(" ");
				}
				else {
					builder.append(breedingTime);
					builder.append(" ");
				}
				builder.append(evaluationTime);
				builder.append(" ");

				individualStringRepresentation(ind, builder, false);
				state.output.println(builder.toString(), statisticslog);
			}
		}
	}

	/** Logs the best individual of the run. */
	@Override
	public void finalStatistics(final EvolutionState state, final int result) {
		if (!silentFront) {
			bypassFinalStatistics(state, result);

			for (int s = 0; s < state.population.subpops.length; s++) {
				MultiObjectiveFitness typicalFitness = (MultiObjectiveFitness) (state.population.subpops[s].individuals[0].fitness);

				// build front
				ArrayList front = typicalFitness.partitionIntoParetoFront(state.population.subpops[s].individuals, null,
						null);

				// sort by objective[0]
				Object[] sortedFront = front.toArray();
				QuickSort.qsort(sortedFront, new SortComparator() {
					public boolean lt(Object a, Object b) {
						return (((MultiObjectiveFitness) (((Individual) a).fitness)).getObjective(
								0) < (((MultiObjectiveFitness) ((Individual) b).fitness)).getObjective(0));
					}

					public boolean gt(Object a, Object b) {
						return (((MultiObjectiveFitness) (((Individual) a).fitness)).getObjective(
								0) > ((MultiObjectiveFitness) (((Individual) b).fitness)).getObjective(0));
					}
				});

				// write front out to disk
				for (int i = 0; i < sortedFront.length; i++) {
					GraphIndividual ind = (GraphIndividual) sortedFront[i];
					StringBuilder builder = new StringBuilder();
					individualStringRepresentation(ind, builder, true);
					state.output.println(builder.toString(), frontLog);
				}
			}
		}
	}

	/**
	 * Pass in a string builder and an individual, and this method will create a String representation. This is
	 * saved in the String builder, and it can be retrieved by calling the toString() method on it.
	 *
	 * @param ind
	 * @param builder
	 * @param finalFront
	 */
	public void individualStringRepresentation(GraphIndividual ind, StringBuilder builder, boolean finalFront) {

		NSGA2MultiObjectiveFitness f = (NSGA2MultiObjectiveFitness) ind.fitness;
		builder.append(f.rank);
		builder.append(" ");
		builder.append(f.sparsity);
		builder.append(" ");

		double[] objectives = f.getObjectives();

		builder.append(objectives[0]);
		builder.append(" ");
		builder.append(objectives[1]);
		builder.append(" ");

		builder.append(ind.availability);
		builder.append(" ");
		builder.append(ind.reliability);
		builder.append(" ");
		builder.append(ind.time);
		builder.append(" ");
		builder.append(ind.cost);

		if (finalFront) {
			builder.append(" ");
			builder.append("\"");
			builder.append(ind.toString());
			builder.append("\"");
		}
	}
}
