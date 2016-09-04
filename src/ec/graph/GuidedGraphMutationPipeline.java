package ec.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

public class GuidedGraphMutationPipeline extends BreedingPipeline {
	private static final long serialVersionUID = 1L;

	@Override
	public Parameter defaultBase() {
		return new Parameter("guidedgraphmutationpipeline");
	}

	@Override
	public int numSources() {
		return 1;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation,
			Individual[] inds, EvolutionState state, int thread) {

		GraphInitializer init = (GraphInitializer) state.initializer;

		int n = sources[0].produce(min, max, start, subpopulation, inds, state, thread);

        if (!(sources[0] instanceof BreedingPipeline)) {
            for(int q=start;q<n+start;q++)
                inds[q] = (Individual)(inds[q].clone());
        }

        if (!(inds[start] instanceof GraphIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("GuidedGraphMutationPipeline didn't get a GraphIndividual. The offending individual is in subpopulation "
            + subpopulation + " and it's:" + inds[start]);

        // Perform mutation
        for(int q=start;q<n+start;q++) {
            GraphIndividual graph = (GraphIndividual)inds[q];
            GraphSpecies species = (GraphSpecies) graph.species;

            // Determine objective with the lowest ranking
            GraphNSGA2MultiObjectiveFitness fit = (GraphNSGA2MultiObjectiveFitness) graph.fitness;
            int lowestObjective = 0;
            int lowestRanking = -1;
            for (int i = 0; i < fit.objRankings.length; i++) {
            	if (fit.objRankings[i] > lowestRanking) {
            		lowestRanking = fit.objRankings[i];
            		lowestObjective = i;
            	}
            }
            
            // Retrieve ranking for that front (i.e. rank) and objective
            GraphIndividual[] ranking = init.rankInformation.get(fit.rank)[lowestObjective];
            Set<String> sharedNodes = new HashSet<String>();

            // Find the nodes shared by a previously specified number of higher candidates
            Map<String, Integer> sharedCount = new HashMap<String, Integer>();
            if (lowestRanking > 0) {
            	int counter = 0;
            	for (int i = lowestRanking-1; i >= 0; i--) {
            		if (counter == GraphInitializer.higherCandidates) {
            			break;
            		}

            		GraphIndividual higherGraph = ranking[i];
            		// Count appearance of nodes within individual
            		for (String nodeName : higherGraph.nodeMap.keySet()) {
            			if (!sharedCount.containsKey(nodeName))
            				sharedCount.put(nodeName, 1);
            			else
            				sharedCount.put(nodeName, sharedCount.get(nodeName) + 1);
            		}
            		counter++;
            	}

            	// Only nodes that appear in all of them should be considered
            	for (Entry<String, Integer> e : sharedCount.entrySet()) {
            		if (e.getValue() == GraphInitializer.higherCandidates)
            			sharedNodes.add(e.getKey());
            	}
            	sharedNodes.remove("start");
            	sharedNodes.remove("end");
            }

            GraphIndividual newGraph = new GraphIndividual();
            List<Node> candidateList = new ArrayList<Node>();
            Set<Node> seenNodes = new HashSet<Node>();
            Set<Node> relevant = init.relevant;
            Set<String> currentEndInputs = new HashSet<String>();
            Node newEnd = init.endNode.clone();

            // Add start node to the graph
            Map<String,Edge> connections = new HashMap<String,Edge>();
    		species.connectCandidateToGraphByInputs(init.startNode.clone(), connections, newGraph, currentEndInputs, init);
            
            // Add shared nodes to the beginning of the candidate queue before starting building process
            for (String nodeName : sharedNodes) {
            	candidateList.add(init.serviceMap.get(nodeName));
            }

            // Then add nodes of the original graph to list of candidates
            for (String nodeName : graph.nodeMap.keySet()) {
            	if (!nodeName.equals("start") && !nodeName.equals("end")) {
            		candidateList.add(init.serviceMap.get(nodeName));
            	}
            }

            // Continue constructing graph
            species.finishConstructingGraph( currentEndInputs, newEnd, candidateList, connections, init,
                    newGraph, null, seenNodes, relevant, false );

            newGraph.evaluated=false;
            init.countGraphElements( newGraph );
        }
        return n;
	}
}
