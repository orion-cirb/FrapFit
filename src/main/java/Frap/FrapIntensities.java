package Frap;

import ij.IJ;
import ij.ImagePlus;
import ij.Menus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.gui.YesNoCancelDialog;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

public class FrapIntensities implements PlugIn {
   ImagePlus imp;
   int nslices;
   RoiManager rm;
   ResultsTable myrt;
   String dir;
   String resdir;
   String name;
   String pmlname;
   String res;
   boolean align;
   boolean filaments;
   boolean savePML;
   boolean saveIntPlot;
   boolean loadPML;
   double dt;

   public boolean getParameters() {
      GenericDialog var1 = new GenericDialog("Options", IJ.getInstance());
      Font var2 = new Font("SansSerif", 1, 12);
      var1.addCheckbox("align_stack", true);
      var1.addCheckbox("filamentous_pmls", false);
      var1.addMessage("------------------------------------------------------------------------- ");
      var1.addMessage("Saving options", var2);
      var1.addMessage("Save each analyzed PML zone to reanalyze later without reloading everything");
      var1.addCheckbox("save_PML_stack", true);
      var1.addMessage("------------------------------------------------------------------------ ");
      var1.addMessage("Load options", var2);
      var1.addMessage("Read a single PML stack previously saved");
      var1.addMessage("If unchecked, open all files from a folder (.nd)");
      var1.addCheckbox("load_PML_stack", false);
      var1.showDialog();
      if (var1.wasCanceled()) {
         return false;
      } else {
         this.align = var1.getNextBoolean();
         this.filaments = var1.getNextBoolean();
         this.savePML = var1.getNextBoolean();
         this.loadPML = var1.getNextBoolean();
         return true;
      }
   }

   public Roi[] getRois() {
      IJ.run(this.imp, "Select None", "");
      this.imp.show();
      IJ.setTool("Oval");
      (new WaitForUserDialog("Draw around FRAPPED PML \n check it contains the same in all times")).show();
      Roi var1 = this.imp.getRoi();
      IJ.setTool("Oval");
      IJ.run(this.imp, "Select None", "");
      (new WaitForUserDialog("Draw around nucleus \n check it contains nucleus in all times")).show();
      Roi var2 = this.imp.getRoi();
      IJ.setTool("Oval");
      IJ.run(this.imp, "Select None", "");
      (new WaitForUserDialog("Draw around NOT Frapped PML \n check it contains nucleus in all times")).show();
      Roi var3 = this.imp.getRoi();
      this.rm.reset();
      this.rm.addRoi(var1);
      this.rm.addRoi(var2);
      this.rm.setSelectedIndexes(new int[]{0, 1});
      this.rm.runCommand(this.imp, "AND");
      Roi var4 = var2;
      Roi var5;
      if (this.imp.getRoi() != null) {
         var5 = this.imp.getRoi();
         this.rm.addRoi(var5);
         this.rm.setSelectedIndexes(new int[]{1, 2});
         this.rm.runCommand(this.imp, "XOR");
         var4 = this.imp.getRoi();
         this.rm.reset();
      }

      IJ.setTool("Rectangle");
      IJ.run(this.imp, "Select None", "");
      (new WaitForUserDialog("Draw rectangle around background")).show();
      var5 = this.imp.getRoi();
      return new Roi[]{var1, var2, var4, var5, var3};
   }

   public void go() {
      IJ.log("Get Rois and plot intensities");
      this.imp.show();
      Roi[] var1 = this.getRois();
      this.imp.show();
      double[] var2 = new double[this.nslices];
      double[] var3 = new double[this.nslices];
      double[] var4 = new double[this.nslices];
      double[] var5 = new double[this.nslices];
      double[] var6 = new double[this.nslices];
      double[] var7 = new double[this.nslices];

      for(int var9 = 1; var9 <= this.nslices; ++var9) {
         var2[var9 - 1] = (double)(var9 - 1) * this.dt;
         this.imp.setSlice(var9);

         for(int var10 = 0; var10 < var1.length; ++var10) {
            var1[var10].setImage(this.imp);
            this.imp.setRoi(var1[var10]);
            ImageStatistics var8 = this.imp.getAllStatistics();
            switch(var10) {
            case 0:
               var5[var9 - 1] = var8.mean;
               break;
            case 1:
               var4[var9 - 1] = var8.mean;
               break;
            case 2:
               var6[var9 - 1] = var8.mean;
               break;
            case 3:
               var3[var9 - 1] = var8.mean;
               break;
            case 4:
               var7[var9 - 1] = var8.mean;
            }
         }
      }

      PlotGraph var11 = new PlotGraph((double)this.nslices * this.dt * 1.05D);
      var11.plotXY("Raw intensities", var2, var4, "all nucleus");
      var11.addPoints(var2, var3, "background");
      var11.addPoints(var2, var5, "frapped");
      var11.addPoints(var2, var6, "all sauf frap");
      var11.addPoints(var2, var7, "PML not frapped");
      var11.savePlot(this.resdir + this.pmlname + "_measuredIntensities.png");
   }

   public void alignImage(ImagePlus var1) {
      IJ.log("Doing stack alignement in time");
      var1.show();
      String var2 = "StackReg";

      try {
         if (Menus.getCommands().get(var2) == null) {
            var2 = "StackReg ";
         }

         IJ.run(var1, var2, "transformation=[Rigid Body]");
      } catch (Exception var4) {
         IJ.log("Error while trying to align (missing plugin ?) \n" + var4.toString());
      }
   }

   public void alignFilaments(ImagePlus var1) {
      IJ.log("Doing stack alignement in time for filamentous PMLs");
      var1.show();
      ImagePlus var2 = var1.duplicate();
      var2.show();
      IJ.run(var2, "Fast Median ...", "filter=5 stack");
      IJ.setAutoThreshold(var2, "MaxEntropy dark");
      Prefs.blackBackground = false;
      IJ.run(var2, "Convert to Mask", "method=MaxEntropy background=Dark calculate");
      IJ.run(var2, "Open", "stack");
      StackReg_Plus var3 = new StackReg_Plus();
      ArrayList var4 = var3.stackRegister(var2, 0);
      var2.changes = false;
      var2.close();

      for(int var5 = 0; var5 < var4.size(); ++var5) {
         Transformer var6 = (Transformer)var4.get(var5);
         var6.doTransformation(var1, false, 0, var5 + 2);
      }

      var1.show();
   }

   public ImagePlus prepareImage(ImagePlus var1) {
      IJ.run(var1, "16-bit", "");
      Calibration var3 = var1.getCalibration();
      this.dt = var3.frameInterval;
      ImagePlus var2;
      if (var1.getNSlices() > 1 && var1.getNFrames() > 1) {
         IJ.log("Doing Z projection");
         ImagePlus var4 = ZProjector.run(var1, "sum all");
         IJ.run(var4, "16-bit", "");
         var2 = var4.duplicate();
         var4.changes = false;
         var4.close();
      } else {
         var2 = var1.duplicate();
      }

      var1.changes = false;
      var1.close();
      if (this.align) {
         this.alignImage(var2);
      }

      int[] var5 = var2.getDimensions();
      if (var5[3] < 2 && var5[4] >= 2) {
         var2.show();
         IJ.run(var2, "Re-order Hyperstack ...", "channels=[Channels (c)] slices=[Frames (t)] frames=[Slices (z)]");
         var2 = IJ.getImage();
      }

      this.nslices = var2.getNSlices();
      return var2;
   }

   public void openFolder() {
      try {
         DirectoryChooser var1 = new DirectoryChooser("Choose images folder");
         this.dir = var1.getDirectory();
         this.resdir = this.dir + Prefs.getFileSeparator() + "Results" + Prefs.getFileSeparator();
         File var2 = new File(this.resdir);
         if (!var2.exists()) {
            var2.mkdir();
         }

         File var3 = new File(this.dir);
         File[] var4 = var3.listFiles();

         for(int var5 = 0; var5 < var4.length; ++var5) {
            File var6 = var4[var5];
            if (var6.isFile()) {
               String var7 = var6.getName();
               int var8 = var7.lastIndexOf(46);
               if (var8 > 0) {
                  String var9 = var7.substring(var8);
                  if (var9.equals(".nd")) {
                     IJ.log("Opening hyperstack " + var7);
                     ImporterOptions var10 = new ImporterOptions();
                     var10.setVirtual(true);
                     var10.setId(this.dir + var7);
                     ImagePlus var11 = BF.openImagePlus(var10)[0];
                     this.name = var11.getTitle();
                     ImagePlus var12 = this.prepareImage(var11);
                     var12.show();
                     boolean var13 = true;
                     var13 = true;

                     for(int var14 = 0; var13; ++var14) {
                        var12.show();
                        IJ.run(var12, "Select None", "");
                        IJ.resetMinAndMax(var12);
                        IJ.setTool("Rectangle");
                        (new WaitForUserDialog("Draw rectangle around zone (nucleus) to analyse")).show();
                        this.imp = var12.crop("stack");
                        var12.hide();
                        this.imp.show();
                        this.pmlname = this.name + "_PML" + var14;
                        YesNoCancelDialog var15 = new YesNoCancelDialog(this.imp.getWindow(), "Align or not", "Align this stack again ?");
                        if (var15.yesPressed()) {
                           if (this.filaments) {
                              this.alignFilaments(this.imp);
                           } else {
                              this.alignImage(this.imp);
                           }
                        }

                        this.go();
                        IJ.run(this.imp, "Select None", "");
                        if (this.savePML) {
                           this.imp.setProperty("time_interval", this.dt);
                           IJ.saveAs(this.imp, "Tiff", this.resdir + this.pmlname + ".tif");
                        }

                        this.imp.changes = false;
                        this.imp.close();
                        var12.show();
                        YesNoCancelDialog var16 = new YesNoCancelDialog(var12.getWindow(), "Do more", "Analyze another nucleus ?");
                        var13 = var16.yesPressed();
                     }

                     var12.changes = false;
                     var12.close();
                  }
               }
            }
         }
      } catch (Exception var17) {
         IJ.log("Error while loading files: " + var17.toString());
      }

   }

   public void loadPML() {
      OpenDialog var1 = new OpenDialog("Open PML stack");
      String var2 = var1.getPath();
      this.imp = IJ.openImage(var2);
      IJ.run(this.imp, "Select None", "");
      this.name = this.imp.getTitle();
      this.dir = IJ.getDirectory("file");
      this.resdir = this.dir + Prefs.getFileSeparator();
      File var3 = new File(this.resdir);
      if (!var3.exists()) {
         var3.mkdir();
      }

      Calibration var4 = this.imp.getCalibration();
      this.dt = var4.frameInterval;
      this.pmlname = this.name;
      if (this.align) {
         this.alignImage(this.imp);
      }

      int[] var5 = this.imp.getDimensions();
      if (var5[3] < 2 && var5[4] >= 2) {
         this.imp.show();
         IJ.run(this.imp, "Re-order Hyperstack ...", "channels=[Channels (c)] slices=[Frames (t)] frames=[Slices (z)]");
         this.imp = IJ.getImage();
      }

      this.nslices = this.imp.getNSlices();
      this.go();
      this.imp.changes = false;
      this.imp.close();
   }

   public void run(String var1) {
      if (this.getParameters()) {
         this.rm = RoiManager.getInstance();
         if (this.rm == null) {
            this.rm = new RoiManager();
         }

         this.rm.reset();
         this.myrt = new ResultsTable();
         IJ.log("\\Clear");
         if (!this.loadPML) {
            this.openFolder();
         } else {
            this.loadPML();
         }

         this.myrt.save(this.resdir + this.name + "_fitResults.csv");
      }
   }
}
