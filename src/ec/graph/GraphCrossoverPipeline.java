package ec.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

/**
 *
 * @author Alex
 */
public class GraphCrossoverPipeline extends BreedingPipeline {
    public static int counter = 0;

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
        counter++;

		GraphInitializer init = (GraphInitializer) state.initializer;
		GraphSpecies species = null;

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

        		if (species == null)
        			species = (GraphSpecies) g1.species;

        		Set<Node> disconnectedInput1 = new HashSet<Node>();
        		Set<Node> disconnectedInput2 = new HashSet<Node>();

        		// Identify the half of each graph, and sever each graph into two
        		GraphIndividual g1Beginning = new GraphIndividual(), g2Beginning = new GraphIndividual();
        		// After severing the graph, g1 and g2 will contain the second halves of graphs only
        		Set<Node> endLayer1 = severGraph(g1, g1Beginning, disconnectedInput1);
        		Set<Node> endLayer2 = severGraph(g2, g2Beginning, disconnectedInput2);

        		GraphIndividual child1 = connectGraphHalves(state, init, species, g1Beginning, g2, endLayer2); // Create first child
        		GraphIndividual child2 = connectGraphHalves(state, init, species, g2Beginning, g1, endLayer1); // Create second child

        		// Incorporate children into population, after having removed any dangling nodes
        		init.removeDanglingNodes( child1 );
        		init.removeDanglingNodes( child2 );
        		
        		if(!species.structureValidator1( child1 ) || !species.structureValidator2( child1 ) || !species.structureValidator4( child1 ) || !species.structureValidator5( child1 ) || !species.structureValidator6( child1 ))
        		    System.out.println("Bah");
                if(!species.structureValidator1( child2 ) || !species.structureValidator2( child2 ) || !species.structureValidator4( child2 ) || !species.structureValidator5( child2 ) || !species.structureValidator6( child2 ))
                    System.out.println("Bah");
        		inds[q] = child1;
        		inds[q++].evaluated = false;
        		if (q < nMin + start) {
        		    inds[q] = child2;
        		    inds[q++].evaluated = false;
        		}
        }
        return nMin;
    }

    private Set<Node> severGraph(GraphIndividual graph, GraphIndividual graphBeginning, Set<Node> disconnectedInput) {
    	Set<Node> firstLayerEnd = new HashSet<Node>();

        // Find first half of the graph
    	int numNodes = graph.nodeMap.size() / 2;

        Queue<Node> queue = new LinkedList<Node>();
        queue.offer( graph.nodeMap.get("start") );

        for (int i = 0; i < numNodes; i++) {
             Node current = queue.poll();

             if(current == null || current.getName().equals( "end" )){
                 break;
             }
             else {
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

	            	// Check that node is entirely fulfilled by nodes already selected
	            	 boolean isInternal = true;
	            	 for (Edge incomingList : e.getToNode().getIncomingEdgeList()) {
	            		 if (!graphBeginning.nodeMap.containsKey(incomingList.getFromNode().getName())) {
	            			isInternal = false;
	            			break;
	            		 }
	            	 }
	            	 if (isInternal)
	            		 queue.offer( e.getToNode() );
	             }
             }
        }

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
        		firstLayerEnd.add(current.getToNode());
        	}
        }
        return firstLayerEnd;
    }

    private GraphIndividual connectGraphHalves(EvolutionState state, GraphInitializer init, GraphSpecies species, GraphIndividual firstHalf, GraphIndividual secondHalf, Set<Node> secondHalfLayer){

    	// Add both halves to the final graph
    	GraphIndividual finalGraph = firstHalf;

    	for(Node n: secondHalf.nodeMap.values()) {

			Node graphN = finalGraph.nodeMap.get(n.getName());

    		if (graphN == null) {
    			finalGraph.nodeMap.put( n.getName(), n );
    			finalGraph.considerableNodeMap.put( n.getName(), n );
    		}

            for (Edge e : n.getIncomingEdgeList()){
            	if (graphN != null && !graphN.getIncomingEdgeList().contains(e)) {
            		graphN.getIncomingEdgeList().add(e);
            	}
            	
            	finalGraph.edgeList.add(e);
            	finalGraph.considerableEdgeList.add(e);
            }

            for(Edge e : n.getOutgoingEdgeList()){
            	if (graphN != null && !graphN.getOutgoingEdgeList().contains(e)) {
            		graphN.getOutgoingEdgeList().add(e);
            	}
            	//finalGraph.edgeList.add(e);
            	//finalGraph.considerableEdgeList.add(e);
            }
    	}

    	// Attempt to satisfy each node from the second half with nodes from the first half
    	Map<String,Edge> connections = new HashMap<String,Edge>();
    	Map<Node,Set<String>> inputsNotSatisfied = new HashMap<Node, Set<String>>();

    	Set<Node> firstHalfNodes = new HashSet<Node>(firstHalf.nodeMap.values());
    	for (Node n : secondHalfLayer) {
    		connections.clear();
    		for (String input : n.getInputs()) {
    			boolean satisfied = species.checkNewGraphNode(init, finalGraph, n, input, connections, firstHalfNodes);
    			if (!satisfied) {
    				Set<String> inputs = inputsNotSatisfied.get(n);
    				if (inputs == null) {
    					inputs = new HashSet<String>();
    					inputsNotSatisfied.put(n, inputs);
    				}
    				inputs.add(input);
    			}
    		}
    		
    		// Make connections of inputs already satisfied
            for (Edge e : connections.values()) {
                finalGraph.edgeList.add(e);
                finalGraph.considerableEdgeList.add(e);
                e.getFromNode().getOutgoingEdgeList().add(e);
                e.getToNode().getIncomingEdgeList().add(e);
            }
    	}

    	// If not completely satisfied, create a subproblem that we solve in order to create the remaining connections
    	 if (!inputsNotSatisfied.isEmpty()) {
    		 addSubgraph(state, init, species, finalGraph, firstHalfNodes, inputsNotSatisfied);
    	}
    	return finalGraph;
    }

    private void addSubgraph(EvolutionState state, GraphInitializer init, GraphSpecies species, GraphIndividual graph, Set<Node> firstHalfNodes, Map<Node, Set<String>> inputsNotSatisfied) {
    	double[] mockQos = new double[4];
        mockQos[GraphInitializer.TIME] = 0;
        mockQos[GraphInitializer.COST] = 0;
        mockQos[GraphInitializer.AVAILABILITY] = 1;
        mockQos[GraphInitializer.RELIABILITY] = 1;

        // The task input is the output of all nodes in the first half
        Set<String> taskInput = new HashSet<String>();
        for (Node n : firstHalfNodes) {
       	 taskInput.addAll(n.getOutputs());
        }

        // The task output is made up of the inputs no satisfied yet
        Set<String> taskOutput = new HashSet<String>();
        for (Set<String> set: inputsNotSatisfied.values()) {
       	 taskOutput.addAll(set);
        }

       Node localStartNode = new Node("start", mockQos, new HashSet<String>(), taskInput);
       Node localEndNode = new Node("end", mockQos, taskOutput ,new HashSet<String>());

   		// Generate the new subgraph
       Set<Node> nodesToConsider = new HashSet<Node>(init.relevant);
       nodesToConsider.removeAll(graph.nodeMap.values());
       GraphIndividual subgraph = species.createNewGraph( null, state, localStartNode, localEndNode, nodesToConsider );

       // Fit subgraph into main graph
       species.fitMutatedSubgraph(init, graph, subgraph, inputsNotSatisfied, firstHalfNodes);
    }
}
