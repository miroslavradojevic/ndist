package com.braincadet.ndist;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.io.*;

/**
 * IJ Plugin that measures the distance between two SWC reconstructions
 * arguments: paths for two SWC reconstruction files and the spatial distance argument
 * measure the distance measures between two SWC (SD,SSD,%SSD),
 * can use to evaluate the reconstruction (compare against gold standard)
 * appends the line with:
 * filename annottag sd ssd %ssd
 * onto eval.csv file that's located in the same directory as the first argument SWC file (swca),
 * the file is created if it does not exist
 * Created by miroslav on 12-5-15.
 */
public class SpatDist implements PlugIn {

    public void run(String s) {

        String swca, swcb;
        float dst;
        String maskpath;

        if (Macro.getOptions()==null) {

            GenericDialog gd = new GenericDialog("NDIST");
            gd.addStringField("A", 	    Prefs.get("com.braincadet.ndist.swca", System.getProperty("user.home")), 60);
            gd.addStringField("B",      Prefs.get("com.braincadet.ndist.swcb", System.getProperty("user.home")), 60);
            gd.addNumericField("S",     Prefs.get("com.braincadet.ndist.dst", 2f), 1, 10, "");
            gd.addStringField("MASK",   Prefs.get("com.braincadet.ndist.mask", ""), 60);
            gd.addMessage("(leave MASK empty if not used)");
            gd.showDialog();
            if (gd.wasCanceled()) return;
            swca	= gd.getNextString();       Prefs.set("com.braincadet.ndist.swca", swca);
            swcb	= gd.getNextString();       Prefs.set("com.braincadet.ndist.swcb", swcb);
            dst = (float) gd.getNextNumber();   Prefs.set("com.braincadet.ndist.dst", dst);
            maskpath = gd.getNextString();      Prefs.set("com.braincadet.ndist.mask", maskpath);
        }
        else {
            swca    = Macro.getValue(Macro.getOptions(), "swca",    "");
            swcb    = Macro.getValue(Macro.getOptions(), "swcb", 	"");
            dst     = Float.valueOf(Macro.getValue(Macro.getOptions(), "dst", 	Float.toString(2)));
            maskpath= Macro.getValue(Macro.getOptions(), "mask",    "");
        }

        ReadSWC swcA = new ReadSWC(swca, false);
        ReadSWC swcB = new ReadSWC(swcb, false);

        ImagePlus mask = (maskpath.equals(""))?null:new ImagePlus(new File(maskpath).getAbsolutePath());

//        IJ.log(""+ new File(maskpath).getAbsolutePath());

        if (mask!=null && mask.getType()!=ImagePlus.GRAY8) {IJ.log("mask needs to be GRAY8"); return;}

//        if (mask!=null) {
//            mask.show();
//
//            String          swclog = "/Users/miroslav/mask.swc";
//            int             swclog_count = 1;
//            PrintWriter     logWriter = null;
//
//            try {
//                logWriter = new PrintWriter(swclog);
//                logWriter.print("");
//                logWriter.close();
//            } catch (FileNotFoundException ex) {}
//
//            try {
//                logWriter = new PrintWriter(new BufferedWriter(new FileWriter(swclog, true)));
//                logWriter.println("");
//            } catch (IOException e) {}
//
//            for (int z = 0; z < mask.getStackSize(); z++) {
//                byte[] arr = (byte[])mask.getStack().getProcessor(z+1).getPixels();
//                for (int i = 0; i < mask.getStack().getWidth()*mask.getStack().getHeight(); i++) {
//                    if (arr[i] != (byte)0) {
//                        int x = i%mask.getStack().getWidth();
//                        int y = i/mask.getStack().getWidth();
//
//                        logWriter.println(
//
//                                String.format("%d %d %d %d %d %f %d", (swclog_count++), 3,
//                                        x,
//                                        y,
//                                        z,
//                                        .1,
//                                        -1
//                                )
//
//                        );
//                    }
//
//                }
//            }
//            logWriter.close();
//            IJ.log("exported: " + swclog);
//        }

        if (swcA.nnodes.size()==0) {
            IJ.log("Empty SWC:"+swca);
            return;
        }
        if (swcB.nnodes.size()==0) {
            IJ.log("Empty SWC:"+swcb);
            return;
        }

        IJ.showStatus("calculating spatial distance...");

        String atag = getFileName(swca);
        String btag = new File(swcb).getParent();
        btag = btag.substring(btag.lastIndexOf(File.separator)+1)+"_"+getFileName(swcb);

        long t1, t2;

        /*
        // version 1
        t1 = System.currentTimeMillis();
        float[] meas = swcA.spatdist(swcB, dst, mask);
        t2 = System.currentTimeMillis();

//        IJ.log("spatdist done. " + IJ.d2s((t2-t1)/1000f,2)+" sec.");
        String legend = String.format("%15s,%s,%10s,%10s,%10s,%10s,%10s,%10s", "NAME", "TAG", "SD", "SSD", "percSSD", "P", "R", "F");
        String eval = String.format("%15s,%s,%10.3f,%10.3f,%10.3f,%10.3f,%10.3f,%10.3f", atag, "NA", meas[0], meas[1], meas[2], meas[3], meas[4], meas[5]);
        IJ.log(legend);
        IJ.log(eval);
        */

        // version 2
        t1 = System.currentTimeMillis();
        float[][] meas1 = swcA.spatdist1(swcB, dst, mask);
        t2 = System.currentTimeMillis();
//        IJ.log("spatdist1 done. " + IJ.d2s((t2-t1)/1000f,2)+" sec.");

        String legend1 = String.format("%15s,%s,%10s,%10s,%10s,%10s,%10s,%10s,%10s,%10s,%10s,%10s", "NAME", "TAG", "SD", "S", "SSD", "percSSD", "P", "R", "F", "PC", "RC", "FC");
        String[] eval1 = new String[meas1.length];
        for (int i = 0; i < meas1.length; i++)
            eval1[i] = String.format("%15s,%s,%10.3f,%2.1f,%10.3f,%10.3f,%10.3f,%10.3f,%10.3f,%10.3f,%10.3f,%10.3f", atag, "NA", meas1[i][0], meas1[i][1], meas1[i][2], meas1[i][3], meas1[i][4], meas1[i][5], meas1[i][6], meas1[i][7], meas1[i][8], meas1[i][9]);

        IJ.log(legend1);
        for (int i = 0; i < meas1.length; i++) IJ.log(eval1[i]);

        // append to the evaluation file
        String  outf = new File(swca).getParent() + File.separator + "eval.csv"; // output (file append) will be stored in the same folder as the chosen swc file

        File f = new File(outf);
        if (!f.exists()) { // first line
            try {
                PrintWriter logWriter = new PrintWriter(outf);
                logWriter.println(legend1);
                logWriter.close();
            } catch (FileNotFoundException ex) {}
        }

        try { // if it exists already in the folder, append on the existing file
            PrintWriter logWriter = new PrintWriter(new BufferedWriter(new FileWriter(outf, true)));
//            logWriter.println(eval);
            for (int i = 0; i < eval1.length; i++) logWriter.println(eval1[i]);
            logWriter.close();
        } catch (IOException e) {}

    }

    private static String getFileName(String file_path) {
        String name = "";

        int i = file_path.lastIndexOf('.');
        int j = file_path.lastIndexOf(File.separator);

        if (i > 0) {
            if (j>=0) {
                name = file_path.substring(j+1, i);
            }
            else {
                name = file_path.substring(0, i);
            }
        }

        return name;
    }

}
