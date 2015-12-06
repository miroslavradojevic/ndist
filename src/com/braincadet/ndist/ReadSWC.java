package com.braincadet.ndist;

import ij.IJ;

import java.io.*;
import java.util.*;

/**
 * reader class for swc neuron reconstruction file
 * Created by miroslav on 23-10-13.
 * modified by miroslav on 5-12-15.
 */
public class ReadSWC {

    // node ids need to be unique in swc format, id's should not repeat
    // (nevertheless, ids can repeat or be missing)
    // input is read as the list of linked nodes (com.braincadet.ndist.Node class)
    // com.braincadet.ndist.Node contains the sphere (x,y,z,r) + the link towards the neighbouring node (index from the node list)

    // new list with nodes
    public ArrayList<Node> nnodes = new ArrayList<Node>();
    public ArrayList<ArrayList<Node>> trees = new ArrayList<ArrayList<Node>>(); // extracted from nnodes if doTree=1

    public int maxID = Integer.MIN_VALUE;

    public float minR = Float.POSITIVE_INFINITY, maxR = Float.NEGATIVE_INFINITY;
    public float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
    public float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
    public float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

    // indexes
    public static int ID 		= 0;
    public static int TYPE 		= 1;
    public static int XCOORD 	= 2;
    public static int YCOORD 	= 3;
    public static int ZCOORD 	= 4;
    public static int RADIUS 	= 5;
    public static int PARENT    = 6;

    public static int SWC_LINE_LENGTH = 7;

    public ReadSWC(String swcpath, boolean doTree) {

        String swcpath1 = new File(swcpath).getAbsolutePath();// path to swc file
        File swcfile = new File(swcpath1);
        if (!(swcfile.exists()) || !getFileExtension(swcpath1).equals("swc")) return;

        ArrayList<float[]> nodes_load = new ArrayList<float[]>(); // 1x7 rows (swc format)

        // read the node list of line elements
        // it's not guaranteed that the node IDs will be compatible with indexing
        // in sense that the nodes are arranged ascending and all exist and none repeat
        // read it first all to het the full range if ids would be to know how much to allocate
        try { // scan the file

            FileInputStream fstream 	= new FileInputStream(swcpath1);
            BufferedReader br 			= new BufferedReader(new InputStreamReader(new DataInputStream(fstream)));
            String read_line;

            while ( (read_line = br.readLine()) != null ) { // it will break on the empty line !!!

                if (read_line.isEmpty()) continue; // skip empty lines

                if(!read_line.trim().startsWith("#")) { // # are comments

                    String[] 	readLn = 	read_line.trim().replaceAll("," , ".").split("\\s+");

                    if (readLn.length!=SWC_LINE_LENGTH) continue; // skip the line that did not have enough values

                    float[] 	valsLn = 	new float[SWC_LINE_LENGTH]; // x, y, z, mother_index

                    valsLn[0] = Integer.valueOf(readLn[ID].trim()).intValue();      // id
                    valsLn[1] = Integer.valueOf(readLn[TYPE].trim()).intValue();    // type

                    valsLn[2] = Float.valueOf(readLn[XCOORD].trim()).floatValue();  // x, y, z
                    valsLn[3] = Float.valueOf(readLn[YCOORD].trim()).floatValue();
                    valsLn[4] = Float.valueOf(readLn[ZCOORD].trim()).floatValue();

                    valsLn[5] = Float.valueOf(readLn[RADIUS].trim()).floatValue();  // radius

                    valsLn[6] = Integer.valueOf(readLn[PARENT].trim()).intValue();  // mother idx

                    nodes_load.add(valsLn);

                    maxID = (valsLn[0]>maxID)? (int) valsLn[0] : maxID;

                    minR = (valsLn[RADIUS]<minR)? valsLn[RADIUS] : minR;
                    maxR = (valsLn[RADIUS]>maxR)? valsLn[RADIUS] : maxR;

                    minX = (valsLn[XCOORD]<minX)? valsLn[XCOORD] : minX;
                    maxX = (valsLn[XCOORD]>maxX)? valsLn[XCOORD] : maxX;

                    minY = (valsLn[YCOORD]<minY)? valsLn[YCOORD] : minY;
                    maxY = (valsLn[YCOORD]>maxY)? valsLn[YCOORD] : maxY;

                    minZ = (valsLn[ZCOORD]<minZ)? valsLn[ZCOORD] : minZ;
                    maxZ = (valsLn[ZCOORD]>maxZ)? valsLn[ZCOORD] : maxZ;

                }

            }

            br.close();
            fstream.close();

        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

//        IJ.log(nodes_load.size() + " lines"); // analyze
//        IJ.log("maxID= " + maxID);

        // initialize all with null, ID will correspond to the index in nnodes list
        nnodes = new ArrayList<Node>(maxID + 1); // zeroth is dummy one (so that indexing can work with IDs) hence there is one more allocated
        for (int i = 0; i <= maxID; i++) nnodes.add(i, null);

        for (int i = 0; i < nodes_load.size(); i++) { // fill the nnodes list elements only

            int     currId      = Math.round(nodes_load.get(i)[ID]);
            int     currType    = Math.round(nodes_load.get(i)[TYPE]);

            float   currX       = nodes_load.get(i)[XCOORD];
            float   currY       = nodes_load.get(i)[YCOORD];
            float   currZ       = nodes_load.get(i)[ZCOORD];

            float   currR       = nodes_load.get(i)[RADIUS];

            int     prevId      = Math.round(nodes_load.get(i)[PARENT]);

            if (nnodes.get(currId)==null) { // add the node
                nnodes.set(currId, new Node(currX, currY, currZ, currR, currType));
            }
            else {
//                IJ.log("double node at " + currId + " with neighbours " + nnodes.get(currId).nbr.size()+ ":"); // doubling the same node (illegal, can happen)
                // assume that we will not read the new coordinates again but the neighbours only
//                nnodes.get(currId).nbr.add(prevId);
            }

        }

        // once the nodes are added add bi-directional connections
        for (int i = 0; i < nodes_load.size(); i++) {

            int     currId      = Math.round(nodes_load.get(i)[ID]);
            int     prevId      = Math.round(nodes_load.get(i)[PARENT]);

//            System.out.println(currId + " -- " + prevId + " --- " + nnodes.size());
//            if (nnodes.get(currId)==null) System.out.println("it was null");

            if (prevId!=-1 && prevId!=currId) {
                nnodes.get(currId).nbr.add(prevId);
                nnodes.get(prevId).nbr.add(currId);
            }

        }

        // remove duplicate neighbouring links
        removeduplicate(nnodes);

        if (doTree) bfs(nnodes, trees);

    }

    private void bfs(ArrayList<Node> nlist, ArrayList<ArrayList<Node>> trees){

        /**
         *  breadth-first search (BFS) to traverse the tree from seed node jotting the number of nodes to reach seed node
         *  http://en.wikipedia.org/wiki/Breadth-first_search
         *
         1  procedure BFS(G,v) is
         2      let Q be a queue
         3      Q.enqueue(v)
         4      label v as discovered
         5      while Q is not empty
         6         v ← Q.dequeue()
         7         for all edges from v to w in G.adjacentEdges(v) do
         8             if w is not labeled as discovered
         9                 Q.enqueue(w)
         10                label w as discovered
         *
         */

        ArrayList[] discovered = disclist(nlist); // initialize discovered list

        int seed; // index of the undiscovered from nlist
        int[] n2t = new int[nlist.size()]; // node list index -> tree list index

        while ((seed=undiscidx(discovered))!=-1) {

            BfsQueue bfsQueue = new BfsQueue();
            Arrays.fill(n2t, Integer.MIN_VALUE);

            ArrayList<Node> tree = new ArrayList<Node>();

            //***
            tree.add(new Node( nlist.get(seed)) );
            n2t[seed] = tree.size()-1;
            //***

            // add the neighbors to the queue and label them as discovered
            for (int j = 0; j <nlist.get(seed).nbr.size(); j++) {
                int next = nlist.get(seed).nbr.get(j);
                // enqueue(), add to FIFO structure, http://en.wikipedia.org/wiki/Queue_%28abstract_data_type%29
                bfsQueue.enqueue(new int[]{seed, next});
                discovered[seed].set(j, true);                                 // set label to discovered in both neighbouting index lists
                discovered[next].set(nlist.get(next).nbr.indexOf(seed), true); // index where the background link was found
            }

//            IJ.log("seed=" + seed + "|q|" + bfsQueue.size());

            while (bfsQueue.hasItems()) {

                // dequeue(), take from FIFO structure, http://en.wikipedia.org/wiki/Queue_%28abstract_data_type%29
                int [] getLnk = (int[]) bfsQueue.dequeue();

                // next neighbour at the time it was added to the queue becomes current
                int prev = getLnk[0]; //Q.get(Q.size()-1)[0];
                int curr = getLnk[1]; //Q.get(Q.size()-1)[1];

                //***
                tree.add(new Node(nlist.get(curr), n2t[prev]));
                n2t[curr] = tree.size()-1;
                //***

                while(Collections.frequency(discovered[curr], false)==1) { // step further if only one undiscovered

                    prev = curr;
                    curr = nlist.get(curr).nbr.get(discovered[curr].indexOf(false));

//                System.out.println(prev + " -- " + curr);
//                    connectivity[curr] = connectivity[prev] + 1;
                    //***
                    tree.add(new Node(nlist.get(curr), n2t[prev]));
                    n2t[curr] = tree.size()-1;
                    //***

                    // mark as discovered the connections curr--prev and prev--curr
                    discovered[curr].set(nlist.get(curr).nbr.indexOf(prev), true);
                    discovered[prev].set(nlist.get(prev).nbr.indexOf(curr), true);

                }

                // there is !=1 neighbour
                for (int i = 0; i < discovered[curr].size(); i++) {
                    boolean isDiscovered =  (Boolean) discovered[curr].get(i);
                    if (!isDiscovered) { // if it was not discovered (0,2,3...)
                        int next = nlist.get(curr).nbr.get(i);

                        bfsQueue.enqueue(new int[]{curr, next});   // enqueue()

                        discovered[curr].set(i, true); // label as discovered
                        discovered[next].set(nlist.get(next).nbr.indexOf(curr), true);

                    }
                }

            } // while there are elements in the queue

            trees.add(tree);

        }

    }

    private void removeduplicate(ArrayList<Node> nlist) {

        // remove duplicate neighbourhood links
        for (int i = 0; i < nlist.size(); i++) {
            if (nlist.get(i)!=null) {
                Set<Integer> set = new HashSet<Integer>();
                set.addAll(nlist.get(i).nbr);
                nlist.get(i).nbr.clear();
                nlist.get(i).nbr.addAll(set);
            }
        }
        // check if the neighborhoods are conistent
        for (int i = 0; i < nlist.size(); i++) {
            if (nlist.get(i)!=null) {
                for (int j = 0; j < nlist.get(i).nbr.size(); j++) {
                    int nbr_idx = nlist.get(i).nbr.get(j);
                    if (nbr_idx>0) {
                        if (Collections.frequency(nlist.get(nbr_idx).nbr, i)!=1)
                            IJ.log("error: missing bidirectional link " + i + " -- " + nbr_idx);
                    }
                }
            }
        }
    }

    private ArrayList[] disclist(ArrayList<Node> nlist) {
        // will be used to book-keep discovered traces (graph vertices)
        ArrayList[] d = new ArrayList[nlist.size()];
        for (int i = 0; i < nlist.size(); i++) {

            d[i] = new ArrayList<Boolean>();

            if (nlist.get(i)!=null) {
                for (int j = 0; j < nlist.get(i).nbr.size(); j++) {
                    d[i].add(false);
                }
            }


        }
        return d;
    }

    private int undiscidx(ArrayList[] d) {
        for (int i = 0; i < d.length; i++) {
            if (d[i].size()>0) {
                boolean dd = (Boolean) d[i].get(0);
                if (!dd) return i;
            }
        }
        return -1;
    }

    private static String getFileExtension(String file_path)
    {
        String extension = "";

        int i = file_path.lastIndexOf('.');
        if (i >= 0) {
            extension = file_path.substring(i+1);
        }

        return extension.toLowerCase();
    }

    public float[] spatdist(ReadSWC compswc, float dst) {

        if (nnodes.size()==0 || compswc.nnodes.size()==0) {
            IJ.log("empty SWC");
            return new float[]{Float.NaN, Float.NaN, Float.NaN};
        }

        // threaded implementation of neuron distance calculation
        SpatDistCalculator.load(nnodes, compswc.nnodes, dst);
        int total = SpatDistCalculator.nlistA.size();
        int CPU_NR = Runtime.getRuntime().availableProcessors() + 0; //IJ.log(CPU_NR+"");

        SpatDistCalculator jobs[] = new SpatDistCalculator[CPU_NR];

        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new SpatDistCalculator(i * total / CPU_NR, (i + 1) * total / CPU_NR);
            jobs[i].start();
        }

        for (int i = 0; i < jobs.length; i++) {
            try {
                jobs[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return SpatDistCalculator.spatdist(dst);

    }

}

class BfsQueue<E> {
    private LinkedList<E> list = new LinkedList<E>();
    public void enqueue(E item) {
        list.addLast(item);
    }
    public E dequeue() {
        return list.poll();
    }
    public boolean hasItems() {
        return !list.isEmpty();
    }
    public int size() {
        return list.size();
    }
//    public void addItems(com.braincadet.ndist.BfsQueue<? extends E> q) {
//        while (q.hasItems())
//            list.addLast(q.dequeue());
//    }
}
