package ec.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

		GraphInitializer init = (GraphInitializer) state.initializer;

		Individual[] inds1 = new Individual[inds.length];
		Individual[] inds2 = new Individual[inds.length];

		int n1 = sources[0].produce(min, max, 0, subpopulation, inds1, state, thread);
		int n2 = sources[1].produce(min, max, 0, subpopulation, inds2, state, thread);

        if (!(sources[0] instanceof BreedingPipeline)) {
            for(int q=0;q<n1;q++)
                inds1[q] = (Individual)(inds1[q].clone());
        }

        if (!(sources[1] instanceof BreedingPipeline)) {
            for(int q=0;q<n2;q++)
                inds2[q] = (Individual)(inds2[q].clone());
        }

        if (!(inds1[0] instanceof GraphIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("GraphMergePipeline didn't get a GraphIndividual. The offending individual is in subpopulation "
            + subpopulation + " and it's:" + inds1[0]);

        if (!(inds2[0] instanceof GraphIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("GraphMergePipeline didn't get a GraphIndividual. The offending individual is in subpopulation "
            + subpopulation + " and it's:" + inds2[0]);

        int nMin = Math.min(n1, n2);

        // Perform crossover
        for(int q=start,x=0; q < nMin + start; q++,x++) {
        		GraphIndividual g1 = ((GraphIndividual)inds1[x]);
        		GraphIndividual g2 = ((GraphIndividual)inds2[x]);

        		// Identify the half of each graph, and sever each graph into two
        		GraphIndividual g1Beginning = null, g1End = null, g2Beginning = null, g2End = null;
        		severGraph(g1, g1Beginning, g1End);
        		severGraph(g2, g2Beginning, g2End);

        		GraphIndividual child1 = connectGraphHalves(g1Beginning, g2End); // Create first child
        		GraphIndividual child2 = connectGraphHalves(g2Beginning, g1End); // Create second child

        		// Incorporate children into population, after having removed any dangling nodes
        		init.removeDanglingNodes( child1 );
        		init.removeDanglingNodes( child2 );
        		inds[q] = child1;
        		inds[q+1] = child2;
	        	inds[q].evaluated = false;
	        	inds[q+1].evaluated = false;
        }
        return nMin;
    }

    private void severGraph(GraphIndividual graph, GraphIndividual graphBeginning, GraphIndividual graphEnd) {
    	graphBeginning = new GraphIndividual();

        // Find first half of the graph
    	int numNodes = graph.nodeMap.size() / 2;

        Queue<Node> queue = new LinkedList<Node>();
        queue.offer( graph.nodeMap.get("start") );

        for (int i = 1; i < numNodes; i++) {
             Node current = queue.poll();

             // Add current node and associated edges to graphBeginning
             graphBeginning.nodeMap.put(current.getName(),current);
             graphBeginning.considerableNodeMap.put(current.getName(), current);
             graphBeginning.edgeList.addAll(current.getOutgoingEdgeList());
             graphBeginning.considerableEdgeList.addAll(current.getOutgoingEdgeList());

             // Remove current node and associated edges from graph
             graph.nodeMap.remove(current.getName());
             graph.considerableNodeMap.remove(current.getName());
             graph.edgeList.removeAll(current.getOutgoingEdgeList());
             graph.considerableEdgeList.removeAll(current.getOutgoingEdgeList());

             // Add next nodes to the queue
             for(Edge e : current.getOutgoingEdgeList()){
                 queue.offer( e.getToNode() );
             }

        }

        // Original graph now only has the second half, so assign it to graphEnd
        graphEnd = graph;

        Iterator<Edge> it = graphBeginning.edgeList.iterator();

        // Sever edges connecting the first half to the second
        while (it.hasNext()) {
        	Edge current = it.next();
        	// If edge leads to a node not in graph beginning, delete it from graph
        	if (!graphBeginning.nodeMap.containsKey(current.getToNode().getName())){
        		it.remove();
        		graphBeginning.considerableEdgeList.remove(current);

        		// Remove it from the origin node
        		current.getFromNode().getOutgoingEdgeList().remove(current);

        		// Also remove this edge from the node in the second graph
        		current.getToNode().getIncomingEdgeList().remove(current);
        	}
        }
    }

    private GraphIndividual connectGraphHalves(GraphIndividual firstHalf, GraphIndividual secondHalf){

    	// Add both halves to the final graph
    	GraphIndividual finalGraph = firstHalf;

    	for(Node n: secondHalf.nodeMap.values()) {

    	}
    	// Check if we can satisfy the halves as they are
    	// If not, satisfy as many as we can and then create a subproblem that we solve in order to create the remaining connections
    	return null; // TODO
    }
}
