package ec.graph;
import java.util.HashMap;
import java.util.Map;

import ec.EvolutionState;
import ec.Individual;
import ec.Problem;
import ec.Subpopulation;
import ec.multiobjective.MultiObjectiveFitness;
import ec.simple.SimpleProblemForm;

public class GraphEvol extends Problem implements SimpleProblemForm {
	private static final long serialVersionUID = 1L;

	@Override
	public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation, final int threadnum) {
	    GraphInitializer init = (GraphInitializer) state.initializer;
	    calculateQoS(init, state, ind, subpopulation, threadnum);
	    if (!GraphInitializer.dynamicNormalisation)
	    	calculateFitness((GraphIndividual)ind, init, state);
	}

    public void calculateQoS(final GraphInitializer init, final EvolutionState state, final Individual ind, final int subpopulation, final int threadnum) {
		if (ind.evaluated) return;   //don't evaluate the individual if it's already evaluated
        if (!(ind instanceof GraphIndividual))
            state.output.fatal("Whoa!  It's not a GraphIndividual!!!",null);
        GraphIndividual ind2 = (GraphIndividual)ind;

        double a = 1.0;
        double r = 1.0;
        double t = 0.0;
        double c = 0.0;

        for (Node n : ind2.nodeMap.values()) {
        	double[] qos = n.getQos();
        	a *= qos[GraphInitializer.AVAILABILITY];
        	r *= qos[GraphInitializer.RELIABILITY];
        	c += qos[GraphInitializer.COST];
        }

        // Calculate longest time
        t = findLongestPath(ind2);

        ind2.availability = a;
        ind2.reliability = r;
        ind2.time = t;
        ind2.cost = c;
	}

    public void calculateFitness(GraphIndividual ind, GraphInitializer init, EvolutionState state) {
    	double a = normaliseAvailability(ind.availability, init);
        double r = normaliseReliability(ind.reliability, init);
        double t = normaliseTime(ind.time, init);
        double c = normaliseCost(ind.cost, init);

        double[] objectives = new double[4];
        objectives[GraphInitializer.AVAILABILITY] = a;
        objectives[GraphInitializer.RELIABILITY] = r;
        objectives[GraphInitializer.TIME] = t;
        objectives[GraphInitializer.COST] = c;

        ((MultiObjectiveFitness)ind.fitness).setObjectives(state, objectives);
        ind.evaluated = true;
    }

	private double normaliseAvailability(double availability, GraphInitializer init) {
		if (init.maxAvailability - init.minAvailability == 0.0)
			return 1.0;
		else
			return (availability - init.minAvailability)/(init.maxAvailability - init.minAvailability);
	}

	private double normaliseReliability(double reliability, GraphInitializer init) {
		if (init.maxReliability - init.minReliability == 0.0)
			return 1.0;
		else
			return (reliability - init.minReliability)/(init.maxReliability - init.minReliability);
	}

	private double normaliseTime(double time, GraphInitializer init) {
	if (init.maxTime - init.minTime == 0.0)
			return 1.0;
		else
			return (init.maxTime - time)/(init.maxTime - init.minTime);
	}

	private double normaliseCost(double cost, GraphInitializer init) {
		if (init.maxCost - init.minCost == 0.0)
			return 1.0;
		else
			return (init.maxCost - cost)/(init.maxCost - init.minCost);
	}

	/**
	 * Uses the Bellman-Ford algorithm with negative weights to find the longest
	 * path in an acyclic directed graph.
	 *
	 * @param g
	 * @return list of edges composing longest path
	 */
	private double findLongestPath(GraphIndividual g) {
		Map<String, Double> distance = new HashMap<String, Double>();
		Map<String, Node> predecessor = new HashMap<String, Node>();

		// Step 1: initialize graph
		for (Node node : g.nodeMap.values()) {
			if (node.getName().equals("start"))
				distance.put(node.getName(), 0.0);
			else
				distance.put(node.getName(), Double.POSITIVE_INFINITY);
		}

		// Step 2: relax edges repeatedly
		for (int i = 1; i < g.nodeMap.size(); i++) {
			for (Edge e : g.edgeList) {
				if ((distance.get(e.getFromNode().getName()) -
				        e.getToNode().getQos()[GraphInitializer.TIME])
				        < distance.get(e.getToNode().getName())) {
					distance.put(e.getToNode().getName(), (distance.get(e.getFromNode().getName()) - e.getToNode().getQos()[GraphInitializer.TIME]));
					predecessor.put(e.getToNode().getName(), e.getFromNode());
				}
			}
		}

		// Now retrieve total cost
		Node pre = predecessor.get("end");
		double totalTime = 0.0;

		while (pre != null) {
			totalTime += pre.getQos()[GraphInitializer.TIME];
			pre = predecessor.get(pre.getName());
		}

		return totalTime;
	}

	/**
	 * Uses the Bellman-Ford algorithm with negative weights to find the longest
	 * path in an acyclic directed graph.
	 *
	 * @param g
	 * @return list of edges composing longest path
	 */
	private int findLongestPath2(GraphIndividual g) {
		Map<String, Integer> distance = new HashMap<String, Integer>();
		Map<String, Node> predecessor = new HashMap<String, Node>();

		// Step 1: initialize graph
		for (Node node : g.nodeMap.values()) {
			if (node.getName().equals("start"))
				distance.put(node.getName(), 0);
			else
				distance.put(node.getName(), Integer.MAX_VALUE);
		}

		// Step 2: relax edges repeatedly
		for (int i = 1; i < g.nodeMap.size(); i++) {
			for (Edge e : g.edgeList) {
				if ((distance.get(e.getFromNode().getName()) - 1)
				        < distance.get(e.getToNode().getName())) {
					distance.put(e.getToNode().getName(), (distance.get(e.getFromNode().getName()) - 1));
					predecessor.put(e.getToNode().getName(), e.getFromNode());
				}
			}
		}

		// Now retrieve total cost
		Node pre = predecessor.get("end");
		int totalTime = 0;

		while (pre != null) {
			totalTime += 1;
			pre = predecessor.get(pre.getName());
		}

		return totalTime;
	}

	@Override
	public void finishEvaluating(EvolutionState state, int threadnum) {
		GraphInitializer init = (GraphInitializer) state.initializer;

		// Get population
		Subpopulation pop = state.population.subpops[0];

		double minAvailability = 2.0;
		double maxAvailability = -1.0;
		double minReliability = 2.0;
		double maxReliability = -1.0;
		double minTime = Double.MAX_VALUE;
		double maxTime = -1.0;
		double minCost = Double.MAX_VALUE;
		double maxCost = -1.0;

		// Keep track of means
		double meanAvailability = 0.0;
		double meanReliability = 0.0;
		double meanTime = 0.0;
		double meanCost = 0.0;

		// Find the normalisation bounds
		for (Individual ind : pop.individuals) {
			GraphIndividual wscInd = (GraphIndividual) ind;
			double a = wscInd.availability;
			double r = wscInd.reliability;
			double t = wscInd.time;
			double c = wscInd.cost;

			meanAvailability += a;
			meanReliability += r;
			meanTime += t;
			meanCost += c;

			if (GraphInitializer.dynamicNormalisation) {
				if (a < minAvailability)
					minAvailability = a;
				if (a > maxAvailability)
					maxAvailability = a;
				if (r < minReliability)
					minReliability = r;
				if (r > maxReliability)
					maxReliability = r;
				if (t < minTime)
					minTime = t;
				if (t > maxTime)
					maxTime = t;
				if (c < minCost)
					minCost = c;
				if (c > maxCost)
					maxCost = c;
			}
		}

		GraphInitializer.meanAvailPerGen[GraphInitializer.availIdx++] = meanAvailability / pop.individuals.length;
		GraphInitializer.meanReliaPerGen[GraphInitializer.reliaIdx++] = meanReliability / pop.individuals.length;
		GraphInitializer.meanTimePerGen[GraphInitializer.timeIdx++] = meanTime / pop.individuals.length;
		GraphInitializer.meanCostPerGen[GraphInitializer.costIdx++] = meanCost / pop.individuals.length;

		if (GraphInitializer.dynamicNormalisation) {
			// Update the normalisation bounds with the newly found values
			init.minAvailability = minAvailability;
			init.maxAvailability = maxAvailability;
			init.minReliability = minReliability;
			init.maxReliability = maxReliability;
			init.minCost = minCost;
			init.maxCost = maxCost;
			init.minTime = minTime;
			init.maxTime = maxTime;

			// Finish calculating the fitness of each candidate
			for (Individual ind : pop.individuals) {
				calculateFitness((GraphIndividual) ind, init, state);
			}
		}

	}
}
