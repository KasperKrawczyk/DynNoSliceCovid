https://user-images.githubusercontent.com/71812535/134390791-da3b4033-097a-40f0-8be4-ab2721abe654.mp4

# DynNoSlice - Covid

One of the applications of graph drawing is visualising disease spread in a popu-lation. Specifically, event-based graph animation can help visualise contact trac-ing model simulations. Building on the DynNoSlice algorithm, we proposea way of incorporating two types of relationships useful in epidemiology into adynamic graph animation: the vector-infected relationship as well as the ‘loca-tion of infection’ relationship. We focus on a COVID contact tracing model, which provides information on an infected person’s infection status, who theywere in contact with and the place or setting of infection. These characteristicsare  accounted  for  in  the  presented  algorithm,  and  encoded,  respectively,  withcolour  (including  saturation),  insertion  of  edges  (denoting  either  the  location,or the infection relationship), and grouping of nodes with using the ’cluster’ el-ements. Graphs are drawn synchronously on a 2D plane, and in a space-timecube,  with  planes  as  time  slices  arranged  perpendicularly  to  the  time  axisT.Variations of the algorithm use graph forces in either2D or 2D + T, componentfiltering, and node highlights to accentuate various aspects of the model.

## Description

<p>
The extension offers a range of options to highlight or filter out data that would be otherwise present in a graph embedding of an entire Covid data set. Each of these options allows for a dynamic graph animation as we all as a space-time cube view.
</p>

### Continuous with Location Highlight 
Lets the user pick a location / setting in which a person was infected, which will be then encoded with node radius increased by 2 for respective nodes.

### Continuous with Location Filter 
Lets the user pick the percentage of a given location (that is, the percentage of infections to have taken in a setting) that components of the graph should include. Components with less than the chosen percentage of infections in the picked location will be filtered out. Nodes to have been infected in that location have radius increased by 2, and the colour of nodes included in the embedding that were infected in locations different to the chosen one have lowered saturation.

### Continuous with Transmission Highlight 
Allows for two locations / settings to be picked. Given edge e, whenever node nsource was infected in the first location, and ntarget was infected in the second, e will have its width enlarged by a factor 1.2, and will be coloured orange.

### Continuous with Location Attraction 
Allows the user to choose a location / setting, which will be represented as an invisible node nlocation in the embedding. All nodes to have been infected in the location of choice will be attracted towards the invisible node’s position, and will have their size increased by a factor of 2. The force pulling nodes towards nlocation is twice of that which attracts any other two extremities of all other edges.

### Continuous with Multiple Locations Attraction 
It can be thought of as a version of Continuous with Location Attraction, where there are multiple nlocationi (where i stands for a cluster’s index) nodes, each embedded as a cluster pole node. Hence, all nodes nattractedi associated with a given location of infection will be encouraged to stay within ith cluster’s circumference. If there are enough vectors pulling nodes in nattractedi node’s component away from the ith cluster, there is a chance that nattractedi may be pulled out of the circumference.

## Getting Started

### Installing

* The program is self-contained, and can be run on download

### Executing program

* On download, the gui mode can be run from command line with:
```
java -jar DynNoSlice.jar gui
```


## Authors

* Kasper Krawczyk
* Daniel Archamabult
* Paolo Simonetto


## License

This project is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) License - see the LICENSE.md file for details

## Acknowledgments

Inspiration, code snippets, etc.
* Danie Archambault
* Paolo Simonetto
