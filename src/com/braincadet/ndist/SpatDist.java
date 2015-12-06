package com.braincadet.ndist;

import ij.IJ;
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

        if (Macro.getOptions()==null) {

            GenericDialog gd = new GenericDialog("SPATIAL DISTANCE");
            gd.addStringField("swca", 	Prefs.get("com.braincadet.ndist.swca", System.getProperty("user.home")), 80);
            gd.addStringField("swcb",   Prefs.get("com.braincadet.ndist.swcb", System.getProperty("user.home")), 80);
            gd.addNumericField("dst",   Prefs.get("com.braincadet.ndist.dst", 2f), 0, 10, "pix");
            gd.showDialog();
            if (gd.wasCanceled()) return;
            swca	= gd.getNextString();       Prefs.set("com.braincadet.ndist.swca", swca);
            swcb	= gd.getNextString();       Prefs.set("com.braincadet.ndist.swcb", swcb);
            dst = (float) gd.getNextNumber();   Prefs.set("com.braincadet.ndist.dst", dst);

        }
        else {
            swca    = Macro.getValue(Macro.getOptions(), "swca",    "");
            swcb    = Macro.getValue(Macro.getOptions(), "swcb", 	"");
            dst     = Float.valueOf(Macro.getValue(Macro.getOptions(), "dst", 	Float.toString(2)));
        }

        ReadSWC swc1 = new ReadSWC(swca, false);
        ReadSWC swc2 = new ReadSWC(swcb, false);

        IJ.showStatus("calculating...");

        long t1 = System.currentTimeMillis();
        float[] meas = swc1.spatdist(swc2, dst);
        long t2 = System.currentTimeMillis();

        IJ.showStatus("done. " + IJ.d2s((t2-t1)/1000f,2)+" sec.");

        String legend = String.format("%15s,%20s,%10s,%10s,%10s", "NAME", "TAG", "SD", "SSD", "percSSD");
        String atag = getFileName(swca);
        String btag = new File(swcb).getParent();
        btag = btag.substring(btag.lastIndexOf(File.separator)+1)+"_"+getFileName(swcb);
        String eval = String.format("%15s,%20s,%10.3f,%10.3f,%10.3f", atag, btag, meas[0], meas[1], meas[2]);

        String  outf = new File(swca).getParent() + File.separator + "eval.csv"; // output (file append) will be stored in the same folder as the chosen swc file
        File f = new File(outf);
        if (!f.exists()) {
            try {
                PrintWriter logWriter = new PrintWriter(outf);
                logWriter.println(legend);
                logWriter.close();
            } catch (FileNotFoundException ex) {}
        }

        try { // if it exists already in the folder, append on the existing file
            PrintWriter logWriter = new PrintWriter(new BufferedWriter(new FileWriter(outf, true)));
            logWriter.println(eval);
            logWriter.close();
        } catch (IOException e) {}

        IJ.log("");
        IJ.log(legend);
        IJ.log(eval);
        IJ.log(outf);

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
