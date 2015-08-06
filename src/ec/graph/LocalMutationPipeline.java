package ec.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
public class LocalMutationPipeline extends BreedingPipeline {

    @Override
    public Parameter defaultBase() {
        return new Parameter("localmutationpipeline");
    }

    @Override
    public int numSources() {
        return 1;
    }

    @Override
    public int produce( int min, int max, int start, int subpopulation,
            Individual[] inds, EvolutionState state, int thread) {

        GraphInitializer init = (GraphInitializer) state.initializer;

        int n = sources[0].produce(min, max, start, subpopulation, inds, state, thread);

        if (!(sources[0] instanceof BreedingPipeline)) {
            for(int q=start;q<n+start;q++)
                inds[q] = (Individual)(inds[q].clone());
        }

        if (!(inds[start] instanceof GraphIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("GraphAppendPipeline didn't get a GraphIndividual. The offending individual is in subpopulation "
            + subpopulation + " and it's:" + inds[start]);

        // Perform mutation
        for(int q=start;q<n+start;q++) {
            GraphIndividual graph = (GraphIndividual)inds[q];
            GraphSpecies species = (GraphSpecies) graph.species;
            Object[] nodes = graph.nodeMap.values().toArray();

            // Randomly select node from which to perform mutation (excluding start and end nodes)
            Node selected = null;
            while (selected == null) {
                Node temp = (Node) nodes[init.random.nextInt( nodes.length )];
                if (!temp.getName().equals( "start" ) && !temp.getName().equals( "end" )) {
                    selected = temp;
                }
            }

            // Select the additional nodes that will be involved in the mutation
            double[] mockQos = new double[4];
            mockQos[GraphInitializer.TIME] = 0;
            mockQos[GraphInitializer.COST] = 0;
            mockQos[GraphInitializer.AVAILABILITY] = 1;
            mockQos[GraphInitializer.RELIABILITY] = 1;

            Set<String> taskInput = new HashSet<String>();
            Set<String> taskOutput = new HashSet<String>();

            Node localStartNode = new Node("start", mockQos, new HashSet<String>(), taskInput);
            Node localEndNode = new Node("end", mockQos, taskOutput ,new HashSet<String>());
            Map<Node, Set<String>> disconnectedInput = new HashMap<Node, Set<String>>();
            Set<Node> disconnectedOutput = new HashSet<Node>();


            removeMutationNodes(init.numNodesMutation, selected, graph, taskInput, taskOutput, disconnectedInput, disconnectedOutput);

            // Generate the new subgraph
            GraphIndividual subgraph = species.createNewGraph( null, state, localStartNode, localEndNode );

            // Add the new subgraph into the existing candidate
            fitMutatedSubgraph(init, graph, subgraph, disconnectedInput, disconnectedOutput);

            // Remove any dangling nodes
            init.removeDanglingNodes( graph );
        }

        return n;
    }


    /**
     * Removes nodes to be replaced during mutation and its associated edges. Based on the
     * nodes removed, it determines the inputs and outputs required by that subpart of the
     * graph.
     *
     * @param numNodes - Number of nodes to be removed (greater or equal to 1)
     * @param selected - The root of the mutation removal process
     * @param graph - The original graph to be mutated
     * @param taskInput - Set to collect the inputs required by the removed subpart
     * @param taskOutput - Set to collect the outputs required by the removed subpart
     */
    private void removeMutationNodes(int numNodes, Node selected, GraphIndividual graph, Set<String> taskInput, Set<String> taskOutput, Map<Node, Set<String>> disconnectedInput, Set<Node> disconnectedOutput) {
        if (numNodes < 1)
            throw new RuntimeException(String.format("The number of nodes requested to be removed during mutation was %d; it should always greater than 0.", numNodes));

        Set<Node> mutationNodes = new HashSet<Node>();

        // Find mutation nodes to remove
        Queue<Node> queue = new LinkedList<Node>();
        queue.offer( selected );

        for (int i = 0; i < numNodes; i++) {
             Node current = queue.poll();

             if(current.getName().equals( "end" )){
                 break;
             }
             else {
                 mutationNodes.add(current);
                 for(Edge e : current.getOutgoingEdgeList()){
                     queue.offer( e.getToNode() );
                 }
             }
        }

        // Now remove all selected mutation nodes and associated edges
        Set<Edge> mutationEdges = new HashSet<Edge>();

        // Remove nodes
        for (Node node : mutationNodes) {
            graph.nodeMap.remove( node.getName() );
            graph.considerableNodeMap.remove( node.getName() );

            for (Edge e : node.getIncomingEdgeList()) {
                mutationEdges.add( e );
                e.getFromNode().getOutgoingEdgeList().remove( e );
            }
            for (Edge e : node.getOutgoingEdgeList()) {
                mutationEdges.add( e );
                e.getToNode().getIncomingEdgeList().remove( e );
            }
        }

        // Remove edges, and figure out what the required inputs and outputs are
        for (Edge edge : mutationEdges) {
            graph.edgeList.remove( edge );
            graph.considerableEdgeList.remove( edge );

            // If the edge is coming from a service that has not been deleted, add its values as available inputs
            if(graph.nodeMap.containsKey( edge.getFromNode().getName())){
                taskInput.addAll(edge.getIntersect());
                disconnectedOutput.add(graph.nodeMap.get(edge.getFromNode().getName()));
            }
            // Else if edge is going to a service that has not been deleted, add its values as required outputs
            else if(graph.nodeMap.containsKey( edge.getToNode().getName())){
                taskOutput.addAll( edge.getIntersect());
                disconnectedInput.put(graph.nodeMap.get(edge.getToNode().getName()), edge.getIntersect());
            }
        }
    }

    private void fitMutatedSubgraph(GraphInitializer init, GraphIndividual graph, GraphIndividual subgraph, Map<Node, Set<String>> disconnectedInput, Set<Node> disconnectedOutput){

        // Remove any repeated nodes in the graph (but keep a list of their outgoing edges)
        List<Edge> outgoingEdges = new ArrayList<Edge>();
        for (String key : subgraph.nodeMap.keySet()){
            if (!key.equals( "start" ) && !key.equals( "end" )) {
                Node node = graph.nodeMap.get( key );
                if (node != null) {
                    // Add outgoing edges to list
                    outgoingEdges.addAll(node.getOutgoingEdgeList());

                    // Remove incoming edges from the graph
                    for (Edge e : node.getIncomingEdgeList()){
                        graph.edgeList.remove( e );
                        graph.considerableEdgeList.remove( e );
                    }

                    // Remove outgoing edges from the graph
                    for (Edge e : node.getOutgoingEdgeList()){
                        graph.edgeList.remove( e );
                        graph.considerableEdgeList.remove( e );
                    }
                }

                // Remove node from graph
                graph.nodeMap.remove( key );
                graph.considerableNodeMap.remove( key );
            }
        }

        // Add subgraph to main graph
        Set<Node> firstSubgraphLayer = new HashSet<Node>();
        Set<Node> lastSubgraphLayer = new HashSet<Node>();

        for (Node n : subgraph.nodeMap.values()) {
            if (!n.getName().equals( "start" ) && !n.getName().equals( "end" )){
                Node newN = n.clone();
                graph.nodeMap.put( newN.getName(), newN );
                graph.considerableNodeMap.put( newN.getName(), newN );
            }

            for (Edge e : n.getIncomingEdgeList()){
                if (e.getFromNode().getName().equals( "start" )) {
                    firstSubgraphLayer.add(n);
                }
                else {
                    addNewGraphEdge(e, graph);
                }
            }

            for(Edge e : n.getOutgoingEdgeList()){
                if (e.getToNode().getName().equals( "end" )) {
                    lastSubgraphLayer.add(n);
                }
                else {
                    addNewGraphEdge(e, graph);
                }
            }
        }

        // If edge destination not in subgraph, re-add edge to the graph XXX Could this introduce cycles?
        for (Edge e : outgoingEdges) {
            if (!subgraph.nodeMap.containsKey(e.getToNode().getName())) {
                addNewGraphEdge(e, graph);
            }
        }

        // Match first subgraph layer with nodes from main graph whose output has been disconnected
        Map<String,Edge> connections = new HashMap<String,Edge>();
        for (Node s : firstSubgraphLayer) {
            Node n = graph.nodeMap.get( s.getName() );
            // Find all input connections
            for (String input : n.getInputs()) {
            	connectNewGraphNode(init, graph, n, input, connections, disconnectedOutput);
            }
        }

        // Match last subgraph layer with nodes from main graph whose input has been disconnected
        connections.clear();
        for (Entry<Node, Set<String>> entry : disconnectedInput.entrySet()) {
        	for (String input : entry.getValue()) {
        		connectNewGraphNode(init, graph, entry.getKey(), input, connections, lastSubgraphLayer);
        	}
        }
    }

    private void connectNewGraphNode(GraphInitializer init, GraphIndividual graph, Node n, String input, Map<String,Edge> connections, Set<Node> fromNodes) {
    	for (Node candidate : init.taxonomyMap.get(input).servicesWithOutput){
    		if (fromNodes.contains(candidate)) {

    			Node graphC = graph.nodeMap.get(candidate.getName());
    			Set<String> intersect = new HashSet<String>();
                intersect.add(input);

                Edge mapEdge = connections.get(graphC.getName());
                if (mapEdge == null) {
                    Edge e = new Edge(intersect);
                    e.setFromNode(graph.nodeMap.get(graphC.getName()));
                    e.setToNode(graphC);
                    connections.put(e.getFromNode().getName(), e);

                    // Connect it to graph
                    graph.edgeList.add(e);
                    graph.considerableEdgeList.add(e);
                    graphC.getOutgoingEdgeList().add(e);
                    n.getIncomingEdgeList().add(e);
                }
                else
                    mapEdge.getIntersect().addAll(intersect);
    		}
        }
    }

    private void addNewGraphEdge(Edge e, GraphIndividual destGraph){
        Edge newE = new Edge(e.getIntersect());
        newE.setFromNode( destGraph.nodeMap.get( e.getFromNode().getName() ) );
        newE.setToNode( destGraph.nodeMap.get( e.getToNode().getName() ) );

        destGraph.edgeList.add(newE);
        destGraph.considerableEdgeList.add(newE);
    }
}
