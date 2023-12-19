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

public class FrapFit implements PlugIn {
   int mode;
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
   boolean saveFitPlot;
   boolean loadPML;
   boolean drawProj;
   double dt;

   public boolean getParameters() {
      GenericDialog var1 = new GenericDialog("Options", IJ.getInstance());
      Font var2 = new Font("SansSerif", 1, 12);
      String[] var3 = new String[]{"Both", "SingleExponential (a-b.exp(-ct))", "DoubleExponential (a-b.exp(-ct)-d.exp(-et))"};
      var1.addChoice("Fit_function", var3, var3[0]);
      var1.addCheckbox("align_stack", true);
      var1.addCheckbox("filamentous_pmls", false);
      var1.addMessage("draw Roi on projected time image or on the time-stack ?");
      var1.addCheckbox("draw_Roi_on_projection", true);
      var1.addMessage("------------------------------------------------------------------------- ");
      var1.addMessage("Saving options", var2);
      var1.addMessage("Save each analyzed PML zone to reanalyze later without reloading everything");
      var1.addCheckbox("save_PML_stack", true);
      var1.addMessage("Save plot showing the intensity measured in each Roi in time");
      var1.addCheckbox("save_intensity_plot", false);
      var1.addMessage("Save plot showing the normalised FRAP intensity and the fit result");
      var1.addCheckbox("save_fit_plot", true);
      var1.addMessage("------------------------------------------------------------------------ ");
      var1.addMessage("Load options", var2);
      var1.addMessage("Read a single PML stack previously saved");
      var1.addMessage("If unchecked, open all files from a folder (.nd)");
      var1.addCheckbox("load_PML_stack", false);
      var1.showDialog();
      if (var1.wasCanceled()) {
         return false;
      } else {
         String var4 = var1.getNextChoice();
         this.mode = -1;

         for(int var5 = 0; var5 < var3.length; ++var5) {
            if (var4.equals(var3[var5])) {
               this.mode = var5;
            }
         }

         this.align = var1.getNextBoolean();
         this.filaments = var1.getNextBoolean();
         this.drawProj = var1.getNextBoolean();
         this.savePML = var1.getNextBoolean();
         this.saveIntPlot = var1.getNextBoolean();
         this.saveFitPlot = var1.getNextBoolean();
         this.loadPML = var1.getNextBoolean();
         return true;
      }
   }

   public void testFit() {
      ExponentialFunction var1 = new ExponentialFunction();
      var1.setMode(this.mode);
      FunctionFitting var2 = new FunctionFitting(var1);
      double[] var3 = new double[50];
      double[] var4 = new double[100];
      double[] var5 = new double[50];

      int var6;
      for(var6 = 0; var6 < 50; ++var6) {
         var3[var6] = (double)var6;
         var5[var6] = 0.7D - 0.3D * Math.exp(-0.4D * (double)var6) - 0.1D * Math.exp(-0.8D * (double)var6) + 0.1D * Math.random();
      }

      for(var6 = 0; var6 < var4.length; ++var6) {
         var4[var6] = (double)var6 * 0.5D;
      }

      double[] var9 = var2.dofit(var3, var5);
      PlotGraph var7 = new PlotGraph((double)this.nslices);
      var7.plotXY("Raw intensities", var3, var5, "pur");
      double[] var8 = var1.getFuncPoints(var4, var9);
      var7.addPoints(var4, var8, "fit");
   }

   public Roi[] getRois() {
      IJ.run(this.imp, "Select None", "");
      ImagePlus var1;
      if (this.drawProj) {
         ImagePlus var2 = ZProjector.run(this.imp, "max all");
         var1 = var2;
      } else {
         var1 = this.imp.duplicate();
      }

      var1.show();
      IJ.run(var1, "Select None", "");
      IJ.setTool("Oval");
      (new WaitForUserDialog("Draw around FRAPPED PML \n must contain constant number of PMLs in all times")).show();
      Roi var5 = var1.getRoi();
      if (var5 == null) {
         var5 = this.imp.getRoi();
      }

      this.imp.hide();
      IJ.setTool("Oval");
      IJ.run(var1, "Select None", "");
      (new WaitForUserDialog("Draw around unfrapped zone (large; if contains frap zone it will be removed) \n must contain constant number of MLs in all times")).show();
      Roi var3 = var1.getRoi();
      this.rm.reset();
      this.rm.addRoi(var5);
      this.rm.addRoi(var3);
      this.rm.setSelectedIndexes(new int[]{0, 1});
      this.rm.runCommand(var1, "AND");
      Roi var4;
      if (var1.getRoi() != null) {
         var4 = var1.getRoi();
         this.rm.addRoi(var4);
         this.rm.setSelectedIndexes(new int[]{1, 2});
         this.rm.runCommand(var1, "XOR");
         var3 = var1.getRoi();
         this.rm.reset();
      }

      IJ.setTool("Rectangle");
      IJ.run(var1, "Select None", "");
      (new WaitForUserDialog("Draw rectangle around background")).show();
      var4 = var1.getRoi();
      var1.changes = false;
      var1.close();
      return new Roi[]{var5, var3, var4};
   }

   public void go() {
      IJ.log("Get Rois and calculate fit(s)");
      this.imp.show();
      Roi[] var1 = this.getRois();
      this.imp.show();
      double[] var2 = new double[this.nslices];
      double[] var3 = new double[this.nslices];
      double[] var4 = new double[this.nslices];
      double[] var5 = new double[this.nslices];

      for(int var7 = 1; var7 <= this.nslices; ++var7) {
         var2[var7 - 1] = (double)(var7 - 1) * this.dt;
         this.imp.setSlice(var7);
         var1[2].setImage(this.imp);
         this.imp.setRoi(var1[2]);
         ImageStatistics var6 = this.imp.getAllStatistics();
         var3[var7 - 1] = var6.mean;
         var1[1].setImage(this.imp);
         this.imp.setRoi(var1[1]);
         var6 = this.imp.getAllStatistics();
         var4[var7 - 1] = var6.mean;
         var1[0].setImage(this.imp);
         this.imp.setRoi(var1[0]);
         var6 = this.imp.getAllStatistics();
         var5[var7 - 1] = var6.mean;
      }

      PlotGraph var18 = new PlotGraph((double)this.nslices * this.dt * 1.05D);
      var18.plotXY("Raw intensities", var2, var4, "cytoplasm");
      var18.addPoints(var2, var3, "background");
      var18.addPoints(var2, var5, "frapped");
      int var8 = 0;
      double var9 = 1.0E7D;

      for(int var11 = 0; var11 < this.nslices; ++var11) {
         var4[var11] -= var3[var11];
         var5[var11] -= var3[var11];
         if (var5[var11] <= var9) {
            var9 = var5[var11];
            var8 = var11;
         }
      }

      double var19 = 0.0D;
      double var13 = 0.0D;

      int var15;
      for(var15 = 0; var15 < var8; ++var15) {
         var19 += var4[var15];
         var13 += var5[var15];
      }

      var19 /= (double)var8;
      var13 /= (double)var8;
      var15 = 0;

      for(int var16 = 0; var16 < this.nslices; ++var16) {
         var5[var16] *= var19 / var13 * 1.0D / var4[var16];
         if (var16 > var8 && var16 > 0 && var5[var16] > 1.0D && var5[var16 - 1] > 1.0D) {
            ++var15;
         }
      }

      if (var15 >= 5) {
         var18.showPlot();
         YesNoCancelDialog var20 = new YesNoCancelDialog(this.imp.getWindow(), "Warning", "Intensitiy after bleaching (recovery) is higher than before. Rois could be wrong. \n Do you want to redraw them ?");
         boolean var17 = var20.yesPressed();
         if (var17) {
            var18.closePlot();
            IJ.log("Redraw Rois");
            this.go();
            return;
         }
      }

      if (this.saveIntPlot) {
         var18.savePlot(this.resdir + this.pmlname + "_measuredIntensities.png");
      } else {
         var18.closePlot();
      }

      this.myrt.incrementCounter();
      this.myrt.addValue("Name", this.pmlname);
      if (this.mode == 0) {
         this.fitFrap(var8, var2, var5, 1, 0);
         this.fitFrap(var8, var2, var5, 2, 0);
      } else {
         this.fitFrap(var8, var2, var5, this.mode, 0);
      }

      this.myrt.addResults();
   }

   public void fitFrap(int var1, double[] var2, double[] var3, int var4, int var5) {
      ExponentialFunction var6 = new ExponentialFunction();
      var6.setMode(var4);
      FunctionFitting var7 = new FunctionFitting(var6);
      double[] var8;
      double[] var9;
      if (var5 > 0) {
         switch(var4) {
         case 1:
            var8 = new double[]{0.85D - (double)var5 * 0.1D, 0.51D - (double)var5 * 0.1D, Math.random()};
            var6.setInitialGuess(var8);
            break;
         case 2:
            var9 = new double[]{0.85D - (double)var5 * 0.1D, 0.51D - (double)var5 * 0.1D, Math.random(), 0.31D - (double)var5 * 0.05D, Math.random()};
            var6.setInitialGuess(var9);
         }
      }

      var8 = new double[this.nslices - var1];
      var9 = new double[this.nslices - var1];
      double var10 = 0.0D;

      for(int var12 = 0; var12 < this.nslices - var1; ++var12) {
         var9[var12] = var2[var12 + var1] - var2[var1];
         var8[var12] = var3[var12 + var1];
         var10 += var8[var12];
      }

      var10 /= (double)(this.nslices - var1);
      double[] var26 = var7.dofit(var9, var8);
      double[] var13 = var6.getFuncPoints(var9, var26);
      double var14 = var6.getMobileFraction(var26);
      if (var5 <= 4 && var14 > 1.0D) {
         this.fitFrap(var1, var2, var3, var4, var5 + 1);
      } else {
         PlotGraph var16 = null;
         if (this.saveFitPlot) {
            var16 = new PlotGraph(var2[var2.length - 1] * 1.05D);
            var16.plotXY("Normalised intensities", var2, var3, "normalised");
         }

         String var17 = "exponential";
         String var18 = "";
         if (var4 == 1) {
            var17 = "single_" + var17;
            var18 = "F1_";
         }

         if (var4 == 2) {
            var17 = "double_" + var17;
            var18 = "F2_";
         }

         this.myrt.addValue(var18 + "Fit", var17);
         this.myrt.addValue(var18 + "MobileFraction", var14);
         double var19 = var6.getThalf(var26, var9, var13);
         this.myrt.addValue(var18 + "HalfTime", var19);
         double var21 = 0.0D;
         double var23 = 0.0D;

         for(int var25 = 0; var25 < this.nslices - var1; ++var25) {
            var9[var25] += var2[var1];
            var21 += Math.pow(var8[var25] - var10, 2.0D);
            var23 += Math.pow(var8[var25] - var13[var25], 2.0D);
         }

         if (this.saveFitPlot) {
            var16.addPoints(var9, var13, var17);
            var16.savePlot(this.resdir + this.pmlname + "_normalisedIntensities_Fit" + var17 + ".png");
         }

         this.myrt.addValue(var18 + "SSE", var23);
         this.myrt.addValue(var18 + "SST", var21);
         this.myrt.addValue(var18 + "R2", 1.0D - var23 / var21);
      }
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
                        (new WaitForUserDialog("Draw rectangle around zone to analyse")).show();
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
                        YesNoCancelDialog var16 = new YesNoCancelDialog(var12.getWindow(), "Do more", "Analyze another nucleus?");
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
