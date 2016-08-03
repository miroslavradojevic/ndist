package com.braincadet.ndist;

import java.util.ArrayList;

/**
 * Node class that stores the basic component of the reconstruction
 * Created by miroslav on 13-3-15.
 * modified by miroslav on 24-8-15.
 */
public class Node {

    public float    x;
    public float    y;
    public float    z;
    public float    r;
    public int      type;

    // types as in neuromorpho.org description
    public static int NOTHING = 0;
    public static int SOMA = 1;
    public static int AXON = 2;
    public static int BASAL_DENDRITE = 3;
    public static int APICAL_DENDRITE = 4;
    public static int CUSTOM1 = 5;
    public static int CUSTOM2 = 6;
    public static int UNDEFINED = 7;

    public ArrayList<Integer> nbr; // indexes of the neighbouring nodes from the node list ArrayList<Node>

//    public Node() {
//        x = y = z = r = Float.NaN;
//        type    = NOTHING;
//        nbr     = null;
//    }

    public Node(float xn, float yn, float zn, float rn, int typ) {
        x       = xn;
        y       = yn;
        z       = zn;
        r       = rn;
        type    = typ;
        nbr = new ArrayList<Integer>();
    }

    public Node(Node n) {
        x = n.x;
        y = n.y;
        z = n.z;
        r = n.r;
        type = n.type;
        nbr = new ArrayList<Integer>();
    }

    public Node(Node n, int parentid) {
        x = n.x;
        y = n.y;
        z = n.z;
        r = n.r;
        type = n.type;
        nbr = new ArrayList<Integer>();
        nbr.add(parentid);
    }

    public static boolean areNeighbours(int i1, Node n1, int i2, Node n2) {
        return n1.nbr.contains(i1) && n2.nbr.contains(i1);
    }

    public float overlap(Node n) { // [0,1]
        // use volumetric overlap of two node spheres, normalized w.r.t the volume of the smaller sphere node
        // http://mathworld.wolfram.com/Sphere-SphereIntersection.html

        double d = Math.sqrt(Math.pow(x-n.x,2)+Math.pow(y-n.y,2)+Math.pow(z-n.z,2));
        float R1 = r;
        float R2 = n.r;

        if (d>R1+R2) return 0f;

        // intersection volume using d, R1, R2
        float V = (float) ((Math.PI/(12*d)) * Math.pow(R1 + R2 - d, 2) * (d*d+6*R1*R2    +2*d*R2-3*R2*R2     +2*d*R1-3*R1*R1));
        float Vnorm = (float) ((4/3f)*Math.PI*Math.pow(Math.min(R1,R2),3)); // (4/3)*pi*r^3 sphere volume
        return (Vnorm>Float.MIN_VALUE)? (V/Vnorm) : 0f ; // return normalized volumetric overlap

    }

}
