package com.braincadet.ndist;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.OverlayCommands;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Arrays;

public class ExportN0 implements PlugIn {

    // used to export the PNR demo video

    public void run(String s) {

        String  nlist_swc = "/Users/miroslav/pnr_viz/201.v3dpbd_n0_.swc";
        int w=512, h=512, l=60;

        // input: swc with , w, h, l
        GenericDialog gd = new GenericDialog("Export_nlist_0");
        gd.addStringField("swc", nlist_swc, 40);
        gd.addNumericField("w", w, 0, 10, "");
        gd.addNumericField("h", h, 0, 10, "");
        gd.addNumericField("l", l, 0, 5, "");

        gd.showDialog();
        if (gd.wasCanceled()) return;

        nlist_swc = gd.getNextString();
        w = (int) gd.getNextNumber(); if (w<=0) return;
        h = (int) gd.getNextNumber(); if (h<=0) return;
        l = (int) gd.getNextNumber(); if (l<=0) return;

        ReadSWC reader = new ReadSWC(nlist_swc, false);

        ImagePlus ip_cumm = new ImagePlus("cumm", new FloatProcessor(w,h));
        float[] cumm = (float[]) ip_cumm.getProcessor().getPixels();

        ImagePlus ip_curr = new ImagePlus("curr", new FloatProcessor(w,h));
        float[] curr = (float[]) ip_curr.getProcessor().getPixels();

        ImageStack is_out_join = new ImageStack(w,h);

        ImagePlus frame = new ImagePlus();

        int fps_min = 50;
        int fps_step = 150;

        int limit = reader.nnodes.size();//reader.nnodes.size();//reader.nnodes.size() / 20;

        int INTERVAL = limit/10; // export video each INTERVAL

        // go through the frames (i.e. overcomplete reconstruction nodes)
        for (int i = 1; i < limit; i++) {

            if (i%INTERVAL==0) IJ.log((10 * (i/INTERVAL)) + "%");

            float x = reader.nnodes.get(i).x;
            float y = reader.nnodes.get(i).y;
            float r = 2f; // reader.nnodes.get(i).r

            // draw the node to the cumm frames
            for (int x1 = (int)Math.floor(x-3f*r); x1 <= (int)Math.ceil(x+3f*r); x1++) {
                for (int y1 = (int) Math.floor(y-3f*r); y1 <= Math.ceil(y+3f*r); y1++) {
                    if (x1>=0 && x1<w && y1>=0 && y1<h) {
                        float val = (float) (1f * Math.exp(-(Math.pow(x1-x,2)+Math.pow(y1-y,2))/(2*(r/2f)*(r/2f))));
                        cumm[y1 * w + x1] = (val>cumm[y1 * w + x1])? val : cumm[y1 * w + x1] ;
                    }
                }
            }

            Arrays.fill(curr, 0f); // reset

            // draw the node to the curr frames
            for (int x1 = (int)Math.floor(x-3f*r); x1 <= (int)Math.ceil(x+3f*r); x1++) {
                for (int y1 = (int) Math.floor(y-3f*r); y1 <= Math.ceil(y+3f*r); y1++) {
                    if (x1>=0 && x1<w && y1>=0 && y1<h) {
                        float val = (float) (1f * Math.exp(-(Math.pow(x1-x,2)+Math.pow(y1-y,2))/(2*(r/.5f)*(r/.5f))));
                        curr[y1 * w + x1] = val;
                    }
                }
            }

            IJ.run(ip_curr, "Yellow", "");

//            is_out_curr.addSlice("i="+IJ.d2s(i,0), ip_curr.duplicate().getProcessor().convertToByteProcessor());

            frame.setImage(ip_cumm.duplicate());

//            is_out_cumm.addSlice("i="+IJ.d2s(i,0), frame.getProcessor().convertToByteProcessor());

            Roi roi = new ImageRoi(0, 0, ip_curr.getProcessor());
            ((ImageRoi)roi).setOpacity(50/100.0);
            ((ImageRoi)roi).setZeroTransparent(true);
            Overlay ov = new Overlay();
            ov.add(roi);

            frame.setOverlay(ov);
            frame = frame.flatten();
            is_out_join.addSlice(""+IJ.d2s(i,0),frame.getProcessor());

            if (i%INTERVAL==0 || i==limit-1) {
                if (is_out_join.getSize()>0) {

                    IJ.log("saving video...");
                    ImagePlus ip_out = new ImagePlus("join", is_out_join);

                    Calibration cal = ip_out.getCalibration();
                    cal.fps = fps_min + fps_step * (i/INTERVAL-1);
                    ip_out.setCalibration(cal);

                    String out = "/Users/miroslav/pnr_viz/is_out_join_"+IJ.d2s(i,0)+"_"+IJ.d2s(cal.fps,0)+"fps.avi";
                    IJ.log(out);
                    IJ.saveAs(ip_out, "AVI", out);
                    IJ.log("done. " + out);
                    while (is_out_join.getSize()>0) is_out_join.deleteLastSlice();

                }
            }

//            ip_frame_cumm.setProcessor(zp.getProjection().getProcessor().convertToByte(true));
//            ip_frame_cumm.setTitle("cummulative");
//            ip_frame_cumm.show();

//            out_cumm.addSlice(zp.getProjection().getProcessor().convertToByte(true));

            // add frame current
//            zp.setImage(ip_curr);
//            zp.setMethod(ZProjector.MAX_METHOD);
//            zp.doProjection();

//            new ImagePlus("b", ip_curr).show();

//            ip_frame_curr.setProcessor(zp.getProjection().getProcessor().convertToByte(true));
//            ip_frame_curr.setTitle("current");
//            ip_frame_curr.show();

//            IJ.run(ip_frame_curr, "Yellow", "");

//            IJ.selectWindow("cummulative");
//            IJ.run("Add Image...", "image=current x=0 y=0 opacity=100");
//            ip_frame_cumm = ip_frame_cumm.flatten();

             // zp.getProjection().getProcessor().convertToByte(true)

//            IJ.run("Close All", "");

            // reset curr stack so that it's filled up from blank next iteration
//            for (int lay = 0; lay < ip_curr.getStack().getSize(); lay++) {
//                int lay_w = ip_curr.getStack().getProcessor(lay+1).getWidth();
//                int lay_h = ip_curr.getStack().getProcessor(lay+1).getHeight();
//                float[] tt = (float[])ip_curr.getStack().getProcessor(lay+1).getPixels();
//                for (int ti = 0; ti < lay_w*lay_h; ti++) {
//                    tt[ti] = 0f;
//                }
//            }

        } // go throught the nodes


        IJ.log("DONE.");

//        if (is_out_cumm.getSize()>0) {
//            ImagePlus ip_out = new ImagePlus("cumm", is_out_cumm);
//            String out = "/Users/miroslav/pnr_viz/is_out_cumm.avi";
//            IJ.saveAs(ip_out, "AVI", out);
//
//        }

//        if (is_out_curr.getSize()>0) {
//            ImagePlus ip_out = new ImagePlus("curr", is_out_curr);
//            String out = "/Users/miroslav/pnr_viz/is_out_curr.avi";
//            IJ.saveAs(ip_out, "AVI", out);
//        }

    }

//    private void setPosition(ImagePlus imp, Roi roi) {
//        boolean setPos = defaultRoi.getPosition()!=0;
//        int stackSize = imp.getStackSize();
//        if (setPos && stackSize>1) {
//            if (imp.isHyperStack()||imp.isComposite()) {
//                boolean compositeMode = imp.isComposite() && ((CompositeImage)imp).getMode()==IJ.COMPOSITE;
//                int channel = !compositeMode||imp.getNChannels()==stackSize?imp.getChannel():0;
//                if (imp.getNSlices()>1)
//                    roi.setPosition(channel, imp.getSlice(), 0);
//                else if (imp.getNFrames()>1)
//                    roi.setPosition(channel, 0, imp.getFrame());
//            } else
//                roi.setPosition(imp.getCurrentSlice());
//        }
        //IJ.log(roi.getCPosition()+" "+roi.getZPosition()+" "+roi.getTPosition());
//    }

}
