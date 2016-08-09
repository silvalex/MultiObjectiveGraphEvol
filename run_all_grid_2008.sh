#!/bin/sh

need sgegrid

NUM_RUNS=50

for i in {1..8}; do
  qsub -t 1-$NUM_RUNS:1 graph_ops.sh ~/workspace/wsc2008/Set0${i}MetaData 2008-graph-dataset${i} graph-evol.params;
  #qsub -t 1-$NUM_RUNS:1 graph_ops.sh ~/workspace/wsc2008/Set0${i}MetaData 2008-graph-newops-dataset${i} graph-evol-newops.params;
done
