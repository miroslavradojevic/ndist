package com.braincadet.ndist;

import ij.IJ;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by miroslav on 12/5/15.
 */
public class SpatDistCalculator extends Thread {

    private int begN, endN;

    public static ArrayList<Node> nlistA = new ArrayList<Node>();
    public static ArrayList<Node> nlistB = new ArrayList<Node>();

    public static float[] dAB;          // this one will be filled in threaded run
    public static float[] dBA;          // this one is appended after the run

    public SpatDistCalculator (int n0, int n1) {
        this.begN = n0;
        this.endN = n1;
    }

    public static void load(ArrayList<Node> nlist1, ArrayList<Node> nlist2, float dst) {

        nlistA.clear();

        for (int i = 0; i <nlist1.size(); i++) {
            if (nlist1.get(i)!=null) {
                nlistA.add(nlist1.get(i));
            }
        }

        nlistB.clear();

        for (int i = 0; i < nlist2.size(); i++) {
            if (nlist2.get(i)!=null) {
                nlistB.add(nlist2.get(i));
            }
        }

        if (nlistA.size()==0 || nlistB.size()==0) {
            IJ.log("error: empty swc file!");
            return;
        }

        if (nlistB.size()>nlistA.size()) {
            ArrayList<Node> temp = nlistA;
            nlistA = nlistB;
            nlistB = temp;
        }

//        IJ.log(nlistA.size() + " --- " + nlistB.size());

        // initialize the matrix with distances and outputs
        dAB = new float[nlistA.size()];
        Arrays.fill(dAB, Float.POSITIVE_INFINITY);
        dBA = new float[nlistB.size()]; // will be calculated in a separate method
        Arrays.fill(dBA, Float.POSITIVE_INFINITY);

    }

    public void run() {

//        IJ.log(begN + " --- " + endN);

        // what will be calculated in parallel are the distances and the closest from A->B
        // distances are kept so that B->A can be calculated as well
        for (int locA=begN; locA<endN; locA++) { // threading works per nodes in neuron A

            // calculate differences for point in A towards all the points in B

            float curr_min = Float.POSITIVE_INFINITY; // reset upon every row

            for (int locB = 0; locB < nlistB.size(); locB++) {

                float dx = nlistA.get(locA).x - nlistB.get(locB).x;
                float dy = nlistA.get(locA).y - nlistB.get(locB).y;
                float dz = nlistA.get(locA).z - nlistB.get(locB).z;

                float d2 = dx*dx + dy*dy + dz*dz; // (float) Math.sqrt( Math.pow(dx,2) + Math.pow(dy,2) + Math.pow(dz,2) );

                if (d2<curr_min) curr_min = d2;

//                IJ.log( " A["+locA+"]="+nlistA.get(locA).x+","+nlistA.get(locA).y+","+nlistA.get(locA).z+
//                        " B["+locB+"]="+nlistB.get(locB).x+","+nlistB.get(locB).y+","+nlistA.get(locB).z+
//                        " D="+Math.sqrt(d2)+
//                                "dx="+dx
//                );

                dBA_store(d2,locB);

            }

            dAB[locA] = curr_min;

        }

    }

    private static synchronized void dBA_store(float val, int idx) {
        if (val<dBA[idx])
            dBA[idx] = val;
    }

    public static float[] spatdist(float dst){ // directed divergence

        float d;

//        IJ.log("AB: "+Arrays.toString(dAB));
//        IJ.log("BA: "+Arrays.toString(dBA));

        // return average of dAB array with mins. (closest dist of node in A towards B)
        float   sd1 = 0, ssd1 = 0;
        int     cnt1 = 0;
        float   maxd = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < dAB.length; i++) {

            d = (float) Math.sqrt(dAB[i]);

            if (d>maxd) maxd = d;

            sd1 += d;

            if (d>=dst) { ssd1 += d; cnt1++;}

        }

        float sd2 = 0, ssd2 = 0;
        int cnt2 = 0;

        for (int i = 0; i < dBA.length; i++) {

            d = (float) Math.sqrt(dBA[i]);

            if (d>maxd) maxd = d;

            sd2 += d;

            if (d>=dst) { ssd2 += d; cnt2++;}

        }

        float sd        = .5f*(sd1/dAB.length)+.5f*(sd2/dBA.length);
        float ssd       = .5f*(ssd1/cnt1)+.5f*(ssd2/cnt2);
        float percssd   = .5f*((float)cnt1/dAB.length)+.5f*((float)cnt2/dBA.length);

        // alternative average computation
//        float sd        = (sd1+sd2)/(dAB.length+dBA.length);
//        float ssd       = (ssd1+ssd2)/(cnt1+cnt2);
//        float percssd   = (float)(cnt1+cnt2)/(dAB.length+dBA.length);

        return new float[]{sd, ssd, percssd};

    }

}
