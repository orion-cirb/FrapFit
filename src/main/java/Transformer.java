import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import java.io.File;
import java.lang.reflect.Method;

public class Transformer {
   protected double[][] sourcePts = new double[3][2];
   private int width;
   private int height;

   public Transformer() {
      for(int var1 = 0; var1 < 3; ++var1) {
         for(int var2 = 0; var2 < 2; ++var2) {
            this.sourcePts[var1][var2] = 0.0D;
         }
      }

   }

   public void setSize(int var1, int var2) {
      this.width = var1;
      this.height = var2;
   }

   public void setSource(double[][] var1) {
      for(int var2 = 0; var2 < 3; ++var2) {
         for(int var3 = 0; var3 < 2; ++var3) {
            this.sourcePts[var2][var3] = var1[var2][var3];
         }
      }

   }

   public void doTransformation(ImagePlus var1, boolean var2, int var3, int var4) {
      try {
         Object var5 = null;
         if (var1.getBitDepth() != 16) {
            IJ.run(var1, "16-bit", "");
         }

         var1.setSlice(var1.getNSlices() / 2);
         ImageStatistics var6 = var1.getStatistics();
         var1.setSlice(var4);
         ImagePlus var7 = new ImagePlus("StackRegSource", new ShortProcessor(this.width, this.height, (short[])var1.getProcessor().getPixels(), var1.getProcessor().getColorModel()));
         FileSaver var8 = new FileSaver(var7);
         String var9 = IJ.getDirectory("temp") + var7.getTitle() + "_" + var3 + ".tif";
         var8.saveAsTiff(var9);
         var5 = IJ.runPlugIn("TurboReg_", "-transform -file " + var9 + " " + this.width + " " + this.height + " -rigidBody " + this.sourcePts[0][0] + " " + this.sourcePts[0][1] + " " + this.width / 2 + " " + this.height / 2 + " " + this.sourcePts[1][0] + " " + this.sourcePts[1][1] + " " + this.width / 2 + " " + this.height / 4 + " " + this.sourcePts[2][0] + " " + this.sourcePts[2][1] + " " + this.width / 2 + " " + 3 * this.height / 4 + " -hideOutput");
         if (var5 == null) {
            throw new ClassNotFoundException();
         } else {
            Method var10 = var5.getClass().getMethod("getTransformedImage", (Class[])null);
            ImagePlus var11 = (ImagePlus)var10.invoke(var5);
            var11.getStack().deleteLastSlice();
            var11.getProcessor().setMinAndMax(0.0D, 65535.0D);
            ImageConverter var12 = new ImageConverter(var11);
            var12.convertToGray16();
            var1.setProcessor((String)null, var11.getProcessor());
            File var13 = new File(var9);
            if (var13 != null && var13.exists()) {
               var13.delete();
            }

            String var14 = IJ.getDirectory("temp") + "StackRegTarget_" + var3 + ".tif";
            File var15 = new File(var14);
            if (var15 != null && var15.exists()) {
               var15.delete();
            }

            var7.changes = false;
            var7.close();
            if (var2) {
               double var16 = var6.mean - 1.0D * var6.stdDev;
               IJ.run(var1, "Macro...", "code=v=v+(v==0)*" + var16 + " stack");
            }

         }
      } catch (Exception var18) {
         IJ.log("Error in alignement with TurboReg " + var18.toString());
         IJ.log("Continue");
      }
   }
}
