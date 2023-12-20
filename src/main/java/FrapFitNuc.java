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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import org.apache.commons.lang.ArrayUtils;

public class FrapFitNuc implements PlugIn {
   int mode;
   ImagePlus imp;
   int nslices;
   int bleachStartSlice;
   int bleachEndSlice;
   RoiManager rm;
   ResultsTable myrt;
   String dir;
   String resdir;
   String name;
   String pmlname;
   boolean align;
   boolean filaments;
   boolean savePML;
   boolean saveIntPlot;
   boolean saveFitPlot;
   boolean loadPML;
   double dt;

   public boolean getParameters() {
      GenericDialog var1 = new GenericDialog("Options", IJ.getInstance());
      Font var2 = new Font("SansSerif", 1, 12);
      String[] var3 = new String[]{"Both", "SingleExponential (a-b.exp(-ct))", "DoubleExponential (a-b.exp(-ct)-d.exp(-et))"};
      var1.addChoice("Fit_function", var3, var3[1]);
      var1.addNumericField("Bleach first frame", 26, 0);
      var1.addNumericField("Bleach last frame", 33, 0);
      var1.addCheckbox("align_stack", true);
      var1.addCheckbox("filamentous_pmls", false);
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
      var1.addHelp("https://github.com/orion-cirb/FrapFit/tree/version2");
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

         this.bleachStartSlice = (int) var1.getNextNumber();
         this.bleachEndSlice = (int) var1.getNextNumber();
         this.align = var1.getNextBoolean();
         this.filaments = var1.getNextBoolean();
         this.savePML = var1.getNextBoolean();
         this.saveIntPlot = var1.getNextBoolean();
         this.saveFitPlot = var1.getNextBoolean();
         this.loadPML = var1.getNextBoolean();
         return true;
      }
   }

   public Roi[] getRois() {
      IJ.run(this.imp, "Select None", "");
      this.imp.show();
      IJ.run(this.imp, "Select None", "");
      IJ.setTool("Oval");
      (new WaitForUserDialog("Draw around FRAPPED PML \n check that it contains the same in all times")).show();
      Roi var1 = this.imp.getRoi();
      IJ.setTool("Oval");
      IJ.run(this.imp, "Select None", "");
      (new WaitForUserDialog("Draw around whole nucleus \n check that it contains nucleus in all times")).show();
      Roi var2 = this.imp.getRoi();
      this.rm.reset();
      this.rm.addRoi(var1);
      this.rm.addRoi(var2);
      IJ.setTool("Rectangle");
      IJ.run(this.imp, "Select None", "");
      (new WaitForUserDialog("Draw rectangle around background")).show();
      Roi var3 = this.imp.getRoi();
      return new Roi[]{var1, var2, var3};
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
      
      // Delete time points and corresponding intensity measurements between bleachStartSlice and bleachEndSlice
      var2 = ArrayUtils.addAll(Arrays.copyOfRange(var2, 0, this.bleachStartSlice-1), Arrays.copyOfRange(var2, this.bleachEndSlice, this.nslices));
      var3 = ArrayUtils.addAll(Arrays.copyOfRange(var3, 0, this.bleachStartSlice-1), Arrays.copyOfRange(var3, this.bleachEndSlice, this.nslices));
      var4 = ArrayUtils.addAll(Arrays.copyOfRange(var4, 0, this.bleachStartSlice-1), Arrays.copyOfRange(var4, this.bleachEndSlice, this.nslices));
      var5 = ArrayUtils.addAll(Arrays.copyOfRange(var5, 0, this.bleachStartSlice-1), Arrays.copyOfRange(var5, this.bleachEndSlice, this.nslices));
      this.nslices -= this.bleachEndSlice - this.bleachStartSlice + 1;
      
      PlotGraph var20 = new PlotGraph((double)this.nslices * this.dt * 1.05D);
      var20.plotXY("Raw intensities", var2, var4, "nucleus");
      var20.addPoints(var2, var3, "background");
      var20.addPoints(var2, var5, "frapped");
      int var8 = 0;
      double var9 = 1.0E7D;
      double var11 = 0.0D;

      for(int var13 = 0; var13 < this.nslices; ++var13) {
         var4[var13] -= var3[var13];
         var5[var13] -= var3[var13];
         double var14 = 0.0D;
         if (var13 >= 1) {
            var14 = var5[var13 - 1] - var5[var13];
         }

         if (var13 >= 2) {
            var14 = var5[var13 - 2] - var5[var13];
         }

         if (var5[var13] <= var9 && var14 > var11 * 1.05D) {
            var9 = var5[var13];
            var11 = var14;
            var8 = var13;
         }
      }

      double var21 = 0.0D;
      double var15 = 0.0D;

      int var17;
      for(var17 = 0; var17 < var8; ++var17) {
         var21 += var4[var17];
         var15 += var5[var17];
      }

      var21 /= (double)var8;
      var15 /= (double)var8;
      var17 = 0;

      for(int var18 = 0; var18 < this.nslices; ++var18) {
         var5[var18] *= var21 / var15 * 1.0D / var4[var18];
         if (var18 > var8 && var18 > 0 && var5[var18] > 1.0D && var5[var18 - 1] > 1.0D) {
            ++var17;
         }
      }

      if (var17 >= 5) {
         var20.showPlot();
         YesNoCancelDialog var22 = new YesNoCancelDialog(this.imp.getWindow(), "Warning", "Intensitiy after bleaching (recovery) is higher than before. Rois could be wrong. \n Do you want to redraw them ?");
         boolean var19 = var22.yesPressed();
         if (var19) {
            var20.closePlot();
            IJ.log("Redraw Rois");
            this.go();
            return;
         }
      }

      if (this.saveIntPlot) {
         var20.savePlot(this.resdir + this.pmlname + "_measuredIntensities.png");
      } else {
         var20.closePlot();
      }

      this.saveIntensities(var2, var5, this.resdir + this.pmlname + "_normalisedIntensities.csv");
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

   public void saveIntensities(double[] var1, double[] var2, String var3) {
      try {
         FileWriter var4 = new FileWriter(var3, false);
         BufferedWriter var5 = new BufferedWriter(var4);
         int var6 = var3.lastIndexOf("PML");
         int var7 = -1;
         if (var3.endsWith(".tif")) {
            var7 = var3.indexOf(".tif");
         }

         if (var3.endsWith(".TIF")) {
            var7 = var3.lastIndexOf(".TIF");
         }

         if (var7 == -1) {
            var7 = var3.length();
         }

         String var8 = var3.substring(var6 + 3, var7);
         var5.write("ImageName\tNumber\tTime\tNormalisedIntensity\n");

         for(int var9 = 0; var9 < var1.length; ++var9) {
            var5.write(this.name + "\t" + var8 + "\t" + var1[var9] + "\t" + var2[var9] + "\n");
         }

         var5.flush();
         var5.close();
      } catch (Exception var10) {
         System.out.println("Error " + var10);
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