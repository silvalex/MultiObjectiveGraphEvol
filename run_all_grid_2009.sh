#!/bin/sh

need sgegrid

NUM_RUNS=50

for i in {1..5}; do
  qsub -t 1-$NUM_RUNS:1 graph_ops.sh ~/workspace/wsc2009/Testset0${i} 2009-graph-dataset${i} graph-evol.params;
  #qsub -t 1-$NUM_RUNS:1 graph_ops.sh ~/workspace/wsc2009/Testset0${i} 2009-graph-newops-dataset${i} graph-evol-newops.params;
done
