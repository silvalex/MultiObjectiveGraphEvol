#! /bin/env Rscript

library(ggplot2)
library(dplyr)

args<-commandArgs(TRUE)

#-------------------------------------

# Magic numbers

offset <- 1.0
numDatasets <- 8
numGenerations <- 51
maxNumExperiments <- 50
numExperimentsConsidered <- 30
conf.level=0.95
significance.warning=999
significance.error=888
print.width=200
set.numbers <- 1:numDatasets
file.numbers <- 1:maxNumExperiments
file.considered <- 1:numExperimentsConsidered

#-------------------------------------

# Create file names

run.prefix <- sprintf("run%s_2008/", args[1])
newops.prefix <- "graph-newops-dataset%s/"
graph.prefix <- "graph-dataset%s/"

prefix <- "out"
suffix <-".stat"

graph.filenames <- paste(run.prefix, graph.prefix, prefix, file.numbers, suffix, sep="")
newops.filenames <- paste(run.prefix, newops.prefix, prefix, file.numbers, suffix, sep="")

#-------------------------------------

# Read graph files

readGraphDataset <- function(dataset,graph.filenames) {
  graph.files <- lapply(graph.filenames, readGraphFile, dataset=dataset)
  graph.files <- graph.files[sapply(graph.files, function(x) !is.null(x))]
  graph.files <- graph.files[file.considered]
  graph.files <- bind_rows(graph.files)
  return(graph.files)
}

readGraphFile <- function(name,dataset){
  table <- tryCatch(read.table(sprintf(name,dataset), nrows=numGenerations),
  error = function(x) return(NULL),
  warning = function(x) return(NULL))
  
  if(!is.null(table)){
    table$dataset = dataset
    table$source = name
  }
  return(table)
}

graph.files <- lapply(set.numbers, readGraphDataset, graph.filenames=graph.filenames)
graph.data <- bind_rows(graph.files)
colnames(graph.data) <- c("Generation", "SetUpTime", "RunTime", "AverageFitness", "BestFitnessThisGen", "BestFitness", "X","Y", "dataset", "source")

#-------------------------------------

# Create total time column for graphs

graph.data$"TotalTime" <- graph.data$"SetUpTime" + graph.data$"RunTime"

#-------------------------------------

# Also create one data point per run for graphs

graph.time.data = aggregate(graph.data$"TotalTime", list(dataset=graph.data$"dataset", source=graph.data$"source"), sum)
colnames(graph.time.data) <- c("dataset", "source", "time")

graph.fitness.data = graph.data[graph.data$"Generation"==(numGenerations-1),]

#-------------------------------------

# Read newops files

newops.files <- lapply(set.numbers, readGraphDataset, graph.filenames=newops.filenames)
newops.data <- bind_rows(newops.files)
colnames(newops.data) <- c("Generation", "SetUpTime", "RunTime", "AverageFitness", "BestFitnessThisGen", "BestFitness", "X","Y", "dataset", "source")

#-------------------------------------

# Create total time column for newops

newops.data$"TotalTime" <- newops.data$"SetUpTime" + newops.data$"RunTime"

#-------------------------------------

# Also create one data point per run for newops

newops.time.data = aggregate(newops.data$"TotalTime", list(dataset=newops.data$"dataset", source=newops.data$"source"), sum)
colnames(newops.time.data) <- c("dataset", "source", "time")

newops.fitness.data = newops.data[newops.data$"Generation"==(numGenerations-1),]

#-------------------------------------

# Construct overall table

graphTime    = merge(aggregate(graph.time.data$"time", list(dataset=graph.time.data$"dataset"), mean),
	             aggregate(graph.time.data$"time", list(dataset=graph.time.data$"dataset"), sd),
	             by="dataset")
graphFitness = merge(aggregate(graph.fitness.data$"BestFitness", list(dataset=graph.fitness.data$"dataset"), mean),
	             aggregate(graph.fitness.data$"BestFitness", list(dataset=graph.fitness.data$"dataset"), sd),
	             by="dataset")
newopsTime    = merge(aggregate(newops.time.data$"time", list(dataset=newops.time.data$"dataset"), mean),
	             aggregate(newops.time.data$"time", list(dataset=newops.time.data$"dataset"), sd),
	             by="dataset")
newopsFitness = merge(aggregate(newops.fitness.data$"BestFitness", list(dataset=newops.fitness.data$"dataset"), mean),
	             aggregate(newops.fitness.data$"BestFitness", list(dataset=newops.fitness.data$"dataset"), sd),
	             by="dataset")
	             
graphValues  = merge(graphTime, graphFitness, by="dataset")
newopsValues   = merge(newopsTime, newopsFitness, by="dataset")
finalTable   = merge(graphValues, newopsValues, by="dataset")

# Run significance tests

significanceForDataset <- function(dataset, graph.time.data, newops.time.data) {
  p.time <- significance(graph.time.data[graph.time.data$"dataset"==dataset,]$"time", newops.time.data[newops.time.data$"dataset"==dataset,]$"time")
  p.fitness <- significance(graph.fitness.data[graph.fitness.data$"dataset"==dataset,]$"BestFitness", newops.fitness.data[newops.fitness.data$"dataset"==dataset,]$"BestFitness")
  return(data.frame(dataset=dataset,p.time,p.fitness))
}

significance <- function(first.group, second.group) {  
  p.value <- tryCatch(wilcox.test(first.group, second.group, paired = TRUE, conf.int=T, conf.level=conf.level)$p.value,
  # If population has many ties, assign our own value 
  warning = function(w) return(significance.warning),
  # If population has many ties, assign our own value
  error = function(e) return(significance.error))
   
  return(p.value)
}

statTable <- lapply(set.numbers, significanceForDataset, graph.time.data=graph.time.data, newops.time.data=newops.time.data)
statTable <- bind_rows(statTable)

finalTable <-merge(finalTable, statTable, by="dataset")
colnames(finalTable) <- c("dataset", "Gtime(avg)", "Gtime(std)", "Gfit(avg)", "Gfit(std)", "Ntime(avg)", "Ntime(std)", "Nfit(avg)", "Nfit(std)", "P-time", "P-fitness");

options(width=print.width)                     
print(finalTable)

#-------------------------------------

# Adjust time data to be in seconds for plotting

graph.time.data$"time" <- graph.time.data$"time" / 1000
newops.time.data$"time" <- newops.time.data$"time"/ 1000

#-------------------------------------

# Combine and adjust dataframes for plotting

graph.total.data <- merge(graph.time.data, graph.fitness.data, by=c("dataset", "source"))
graph.total.data <- subset(graph.total.data, select=c("dataset", "source", "time", "BestFitness"))
graph.total.data$"Method" <- "Graph-based"

newops.total.data <- merge(newops.time.data, newops.fitness.data, by=c("dataset", "source"))
newops.total.data <- subset(newops.total.data, select=c("dataset", "source", "time", "BestFitness"))
newops.total.data$"Method" <- "Newops-based"

total.data <- rbind(graph.total.data, newops.total.data)

#-------------------------------------

# Plot time

pdf(sprintf("timePlot%s_2008.pdf",args[1]), width=10)
ggplot(data=total.data, aes(x=factor(dataset), y=time , colour=Method)) + geom_point(position="jitter") + labs(x = "Dataset", y = 
"Time (s)") + theme(text = element_text(size=20), axis.text.x=element_text(colour="black"), axis.text.y=element_text(colour="black"))

# Plot fitness

pdf(sprintf("fitnessPlot%s_2008.pdf",args[1]), width=10)
ggplot(data=total.data, aes(x=factor(dataset), y=BestFitness , colour=Method)) + geom_point(position="jitter") + labs(x = "Dataset", 
y = "Fitness") + theme(text = element_text(size=20), axis.text.x=element_text(colour="black"), axis.text.y=element_text(colour="black"))
