package ec.graph;

import java.util.ArrayList;

import ec.EvolutionState;
import ec.Individual;
import ec.multiobjective.nsga2.NSGA2Breeder;
import ec.multiobjective.nsga2.NSGA2Evaluator;
import ec.multiobjective.nsga2.NSGA2MultiObjectiveFitness;
import ec.util.SortComparator;

public class GraphNSGA2Evaluator extends NSGA2Evaluator {

	private static final long serialVersionUID = 1L;

	@Override
	public Individual[] buildArchive(EvolutionState state, int subpop) {
		Individual[] dummy = new Individual[0];
		ArrayList ranks = assignFrontRanks(state.population.subpops[subpop]);

		ArrayList newSubpopulation = new ArrayList();
		int size = ranks.size();
		for (int i = 0; i < size; i++) {
			Individual[] rank = (Individual[]) ((ArrayList) (ranks.get(i))).toArray(dummy);
			assignSparsity(rank, i);
			if (rank.length + newSubpopulation.size() >= originalPopSize[subpop]) {
				// first sort the rank by sparsity
				ec.util.QuickSort.qsort(rank, new SortComparator() {
					public boolean lt(Object a, Object b) {
						Individual i1 = (Individual) a;
						Individual i2 = (Individual) b;
						return (((NSGA2MultiObjectiveFitness) i1.fitness).sparsity > ((NSGA2MultiObjectiveFitness) i2.fitness).sparsity);
					}

					public boolean gt(Object a, Object b) {
						Individual i1 = (Individual) a;
						Individual i2 = (Individual) b;
						return (((NSGA2MultiObjectiveFitness) i1.fitness).sparsity < ((NSGA2MultiObjectiveFitness) i2.fitness).sparsity);
					}
				});

				// then put the m sparsest individuals in the new population
				int m = originalPopSize[subpop] - newSubpopulation.size();
				for (int j = 0; j < m; j++)
					newSubpopulation.add(rank[j]);

				// and bail
				break;
			} else {
				// dump in everyone
				for (int j = 0; j < rank.length; j++)
					newSubpopulation.add(rank[j]);
			}
		}

		Individual[] archive = (Individual[]) (newSubpopulation.toArray(dummy));

		// maybe force reevaluation
		NSGA2Breeder breeder = (NSGA2Breeder) (state.breeder);
		if (breeder.reevaluateElites[subpop])
			for (int i = 0; i < archive.length; i++)
				archive[i].evaluated = false;

		return archive;
	}

	public void assignSparsity(Individual[] front, int rankNo) {
		int numObjectives = ((NSGA2MultiObjectiveFitness) front[0].fitness).getObjectives().length;

		for (int i = 0; i < front.length; i++)
			((NSGA2MultiObjectiveFitness) front[i].fitness).sparsity = 0;

		// Create structure for storing the ordering for this rank, and add it to the initializer // XXX
		GraphIndividual[][] ordering = new GraphIndividual[numObjectives][front.length];
		GraphInitializer.rankInformation.add(rankNo, ordering);

		for (int i = 0; i < numObjectives; i++) {
			final int o = i;
			// 1. Sort front by each objective.
			// 2. Sum the manhattan distance of an individual's neighbours over
			// each objective.
			// NOTE: No matter which objectives objective you sort by, the
			// first and last individuals will always be the same (they maybe
			// interchanged though). This is because a Pareto front's
			// objective values are strictly increasing/decreasing.

			ec.util.QuickSort.qsort(front, new SortComparator() {
				public boolean lt(Object a, Object b) {
					Individual i1 = (Individual) a;
					Individual i2 = (Individual) b;
					return (((NSGA2MultiObjectiveFitness) i1.fitness)
							.getObjective(o) < ((NSGA2MultiObjectiveFitness) i2.fitness).getObjective(o));
				}

				public boolean gt(Object a, Object b) {
					Individual i1 = (Individual) a;
					Individual i2 = (Individual) b;
					return (((NSGA2MultiObjectiveFitness) i1.fitness)
							.getObjective(o) > ((NSGA2MultiObjectiveFitness) i2.fitness).getObjective(o));
				}
			});

			// Clone the individuals in this particular order, storing it in the initializer for later use // XXX
			for (int j = 0; j < front.length; j++) {
				GraphIndividual ind = (GraphIndividual)front[j];
				((GraphNSGA2MultiObjectiveFitness)ind.fitness).objRankings[i] = j;
				ordering[i][j] = ind.clone();
			}

			// Compute and assign sparsity.
			// the first and last individuals are the sparsest.
			((NSGA2MultiObjectiveFitness) front[0].fitness).sparsity = Double.POSITIVE_INFINITY;
			((NSGA2MultiObjectiveFitness) front[front.length - 1].fitness).sparsity = Double.POSITIVE_INFINITY;
			for (int j = 1; j < front.length - 1; j++) {
				NSGA2MultiObjectiveFitness f_j = (NSGA2MultiObjectiveFitness) (front[j].fitness);
				NSGA2MultiObjectiveFitness f_jplus1 = (NSGA2MultiObjectiveFitness) (front[j + 1].fitness);
				NSGA2MultiObjectiveFitness f_jminus1 = (NSGA2MultiObjectiveFitness) (front[j - 1].fitness);

				// store the NSGA2Sparsity in sparsity
				f_j.sparsity += (f_jplus1.getObjective(o) - f_jminus1.getObjective(o))
						/ (f_j.maxObjective[o] - f_j.minObjective[o]);
			}
		}
	}
}
