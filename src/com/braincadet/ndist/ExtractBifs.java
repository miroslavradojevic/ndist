package com.braincadet.ndist;

import ij.IJ;
import ij.Macro;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.io.File;
import java.util.ArrayList;

/**
 *
 */
public class ExtractBifs implements PlugIn {

    @Override
    public void run(String s) {

        String swcIn;

        if (Macro.getOptions()==null) {
            GenericDialog gd = new GenericDialog("BIFURCATION");
            gd.addStringField("swcin", 	    Prefs.get("com.braincadet.ndist.swcin", System.getProperty("user.home")), 80);
            gd.showDialog();
            if (gd.wasCanceled()) return;
            swcIn	= gd.getNextString();       Prefs.set("com.braincadet.ndist.swcin", swcIn);
        }
        else {
            swcIn     = Macro.getValue(Macro.getOptions(), "swcin",    "");
        }

        String swcpath1 = new File(swcIn).getAbsolutePath(); // path to swc file

        File swcfile = new File(swcpath1);

        if (!(swcfile.exists())) {
            IJ.log(swcfile+" does not exist.");
            return;
        }

        if (swcfile.isFile() && getFileExtension(swcpath1).equals("swc")) {

            extractBifurcations(swcpath1);

        }
        else if (swcfile.isDirectory()) {

            ArrayList<File> fileList = new ArrayList<File>();

            listFileExtensionInDirectory(swcIn, fileList, ".swc");

            IJ.log("found " + fileList.size() + " files:");

            for (File f : fileList) {

                extractBifurcations(f.getAbsolutePath());

            }

        }
        else {
            IJ.log("No option available for this entry");
        }

        IJ.log("done.");

    }

    public void extractBifurcations(String swcFilePath){

        IJ.log("");
        IJ.log(swcFilePath);

        ReadSWC swcA = new ReadSWC(swcFilePath, false);

        IJ.log(swcA.nnodes.size() + " nodes");

        String swcOut = removeFileExtension(swcFilePath) + "_bif." + getFileExtension(swcFilePath);

        swcA.getBifurcations(swcOut);

        IJ.log(swcOut);

    }

    public void listFilesInDirectory(String directoryName, ArrayList<File> files) {

        File directory = new File(directoryName);

        // Get all the files from a directory.
        File[] fList = directory.listFiles();

        if(fList != null) {

            for (File file : fList) {

                if (file.isFile()) {

                    files.add(file);

                } else if (file.isDirectory()) {

                    listFilesInDirectory(file.getAbsolutePath(), files);

                }
            }
        }
    }

    public void listFileExtensionInDirectory(String directoryName, ArrayList<File> files, String extension){

        File directory = new File(directoryName);

        // Get all the files from a directory with extension.
        File[] fList = directory.listFiles();

        if(fList != null) {

            for (File file : fList) {

                if (file.isFile() && getFileExtension(file).toLowerCase().equals(extension)) {

                    files.add(file);

                } else if (file.isDirectory()) {

                    listFileExtensionInDirectory(file.getAbsolutePath(), files, extension);

                }
            }
        }

    }

    private static String getFileExtension(File file) {

        String extension = "";

        try {
            if (file != null && file.exists()) {
                String name = file.getName();
                extension = name.substring(name.lastIndexOf("."));
            }
        } catch (Exception e) {
            extension = "";
        }

        return extension;

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

    private static String removeFileExtension(String file_path)
    {

        String baseName = "";

        int i = file_path.lastIndexOf('.');
        if (i >= 0) {
            baseName = file_path.substring(0, i); //substring(i+1);
        }

        return baseName;

    }

}
