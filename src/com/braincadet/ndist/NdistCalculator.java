package com.braincadet.ndist;

import ij.IJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.Arrays;

public class NdistCalculator extends Thread {


    private int begA, endA, begB, endB;
    public static ArrayList<Node> nlistA = new ArrayList<Node>();
    public static ArrayList<Node> nlistB = new ArrayList<Node>();

    public static float[] dAB;
    public static float[] dBA;

    public NdistCalculator (int a0, int a1, int b0, int b1) {
        this.begA = a0;
        this.endA = a1;
        this.begB = b0;
        this.endB = b1;
    }

    // loader using the exclusion mask (soma nodes are usually masked out)
    public static void load(ArrayList<Node> nlist1, ArrayList<Node> nlist2, ImagePlus mask) {
        // add those nodes where byte8 mask was zero
        byte[][] stackarr = new byte[mask.getStackSize()][];
        for (int z = 0; z < mask.getStackSize(); z++)
            stackarr[z] = (byte[])mask.getStack().getProcessor(z+1).getPixels();

        nlistA.clear();
        for (int i = 0; i <nlist1.size(); i++) { // check node coordinates, add if it is within the image boundaries and soma mask==0
            if (nlist1.get(i)!=null) {
                int x = Math.round(nlist1.get(i).x);
                int y = Math.round(nlist1.get(i).y);
                int z = Math.round(nlist1.get(i).z);
                if (x>=0 && x<mask.getWidth() && y>=0 && y<mask.getHeight() && z>=0 && z<mask.getStackSize() && stackarr[z][y*mask.getWidth()+x]==(byte)0) {
                    nlistA.add(nlist1.get(i));
                }
            }
        }

        nlistB.clear();
        for (int i = 0; i < nlist2.size(); i++) { // check node coordinates, add if it is within the image boundaries and soma mask==0
            if (nlist2.get(i)!=null) {
                int x = Math.round(nlist2.get(i).x);
                int y = Math.round(nlist2.get(i).y);
                int z = Math.round(nlist2.get(i).z);
                if (x>=0 && x<mask.getWidth() && y>=0 && y<mask.getHeight() && z>=0 && z< mask.getStackSize() && stackarr[z][y*mask.getWidth()+x]==(byte)0) {
                    nlistB.add(nlist2.get(i));
                }
            }
        }

        if (nlistA.size()==0 || nlistB.size()==0) {
            IJ.log("error: empty swc file!");
            return;
        }

        // initialize the matrix with distances and outputs
        dAB = new float[nlistA.size()];
        Arrays.fill(dAB, Float.POSITIVE_INFINITY);
        dBA = new float[nlistB.size()];
        Arrays.fill(dBA, Float.POSITIVE_INFINITY);

    }

    public static void load(ArrayList<Node> nlist1, ArrayList<Node> nlist2) {

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

        // initialize the matrix with distances and outputs
        dAB = new float[nlistA.size()];
        Arrays.fill(dAB, Float.POSITIVE_INFINITY);
        dBA = new float[nlistB.size()]; // will be calculated in a separate method
        Arrays.fill(dBA, Float.POSITIVE_INFINITY);

    }

    public void run() {

        float dx, dy, dz, d2, curr_min;

        for (int locA=begA; locA < endA; locA++) {
            curr_min = Float.POSITIVE_INFINITY;
            for (int locB = 0; locB < nlistB.size(); locB++) {
                dx = nlistA.get(locA).x - nlistB.get(locB).x;
                dy = nlistA.get(locA).y - nlistB.get(locB).y;
                dz = nlistA.get(locA).z - nlistB.get(locB).z;
                d2 = dx*dx + dy*dy + dz*dz;
                if (d2<curr_min) curr_min = d2;
            }
            dAB[locA] = curr_min;
        }

        for (int locB = begB; locB < endB; locB++) {
            curr_min = Float.POSITIVE_INFINITY;
            for (int locA = 0; locA < nlistA.size(); locA++) {
                dx = nlistB.get(locB).x - nlistA.get(locA).x;
                dy = nlistB.get(locB).y - nlistA.get(locA).y;
                dz = nlistB.get(locB).z - nlistA.get(locA).z;
                d2 = dx*dx + dy*dy + dz*dz;
                if (d2<curr_min) curr_min = d2;
            }
            dBA[locB] = curr_min;
        }
    }

    public static float[][] compute(float s) {

        ArrayList<Float> sval = new ArrayList<Float>();
        for (float i = 0.5f; i <= s+Float.MIN_VALUE; i+=0.5f) {
            sval.add(i);}

        float[][] out = new float[sval.size()][10];

        for (int si = 0; si < sval.size(); si++) {

            float   d;
            float   sdA = 0, ssdA = 0, sdB = 0, ssdB = 0;
            int     cntA = 0, cntB = 0, tp_rec = 0, tp_gs = 0;

            for (int i = 0; i < dAB.length; i++) {

                d = (float) Math.sqrt(dAB[i]);

                sdA += d;

                if (d>=sval.get(si)) {
                    ssdA += d;
                    cntA++;
                }
                else {
                    tp_rec++;
                }

            }

            for (int i = 0; i < dBA.length; i++) {

                d = (float) Math.sqrt(dBA[i]);

                sdB += d;

                if (d>=sval.get(si)) {
                    ssdB += d;
                    cntB++;
                }
                else {
                    tp_gs++;
                }

            }

            float sd        = .5f*(sdA/dAB.length)+.5f*(sdB/dBA.length);
            float ssd       = ((cntA>0)? (.5f*(ssdA/cntA)) : 0) + ((cntB>0)? (.5f*(ssdB/cntB)) : 0);
            float percssd   = .5f*((float)cntA/dAB.length)+.5f*((float)cntB/dBA.length);
            float precision = (float)tp_rec/dAB.length;//((swtch)?dBA.length:dAB.length);
            float recall    = (float)tp_gs/dBA.length;//((swtch)?dAB.length:dBA.length);
            float fscore    = (precision+recall>Float.MIN_VALUE)? ((2*precision*recall)/(precision+recall)) : 0f;

            out[si][0] = sd;
            out[si][1] = sval.get(si);
            out[si][2] = ssd;
            out[si][3] = percssd;
            out[si][4] = precision;
            out[si][5] = recall;
            out[si][6] = fscore;

            float precision_cumm = 0;
            float recall_cumm    = 0;
            float fscore_cumm    = 0;
            for (int k = si; k >= 0; k--) {
                float delta_s = (k>0)?(sval.get(k)-sval.get(k-1)) : sval.get(k);
                precision_cumm += delta_s * out[k][4];
                recall_cumm    += delta_s * out[k][5];
                fscore_cumm    += delta_s * out[k][6];
            }
            precision_cumm /= sval.get(si); // normalize
            recall_cumm /= sval.get(si);
            fscore_cumm /= sval.get(si);

            out[si][7] = precision_cumm;
            out[si][8] = recall_cumm;
            out[si][9] = fscore_cumm;

        }

        return out;

    }

}
