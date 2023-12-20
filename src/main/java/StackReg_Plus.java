import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ShortProcessor;
import java.awt.image.IndexColorModel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class StackReg_Plus implements PlugIn {
   private final double TINY = (double)Float.intBitsToFloat(872415231);
   private int transformType = 2;
   protected ArrayList<Transformer> transformers;

   public void run(String var1) {
      Runtime.getRuntime().gc();
      ImagePlus var2 = WindowManager.getCurrentImage();
      if (var2 == null) {
         IJ.error("No image available");
      } else if (!var2.getStack().isRGB() && !var2.getStack().isHSB()) {
         ImagePlus var3 = ZProjector.run(var2, "avg all");
         var3.show();
         this.runRegister(var3, 0);
         var3.changes = false;
         var3.close();

         for(int var4 = 0; var4 < this.transformers.size(); ++var4) {
            Transformer var5 = (Transformer)this.transformers.get(var4);
            IJ.run(var2, "Make Substack...", "slices=1-" + var2.getNSlices() + " frames=" + (var4 + 1));
            ImagePlus var6 = IJ.getImage();
            var5.doTransformation(var6, false, 0, var4);
         }

      } else {
         IJ.error("Unable to process either RGB or HSB stacks");
      }
   }

   public ArrayList<Transformer> stackRegister(ImagePlus var1, int var2) {
      this.runRegister(var1, var2);
      return this.transformers;
   }

   public void runRegister(ImagePlus var1, int var2) {
      this.transformType = 1;
      int var4 = var1.getWidth();
      int var5 = var1.getHeight();
      int var6 = var1.getCurrentSlice();
      double[][] var7 = new double[][]{{1.0D, 0.0D, 0.0D}, {0.0D, 1.0D, 0.0D}, {0.0D, 0.0D, 1.0D}};
      Object var8 = null;
      this.transformers = new ArrayList();
      double[][] var13;
      switch(1) {
      case 0:
         var13 = new double[1][3];
         var13[0][0] = (double)(var4 / 2);
         var13[0][1] = (double)(var5 / 2);
         var13[0][2] = 1.0D;
         break;
      case 1:
         var13 = new double[3][3];
         var13[0][0] = (double)(var4 / 2);
         var13[0][1] = (double)(var5 / 2);
         var13[0][2] = 1.0D;
         var13[1][0] = (double)(var4 / 2);
         var13[1][1] = (double)(var5 / 4);
         var13[1][2] = 1.0D;
         var13[2][0] = (double)(var4 / 2);
         var13[2][1] = (double)(3 * var5 / 4);
         var13[2][2] = 1.0D;
         break;
      case 2:
         var13 = new double[2][3];
         var13[0][0] = (double)(var4 / 4);
         var13[0][1] = (double)(var5 / 2);
         var13[0][2] = 1.0D;
         var13[1][0] = (double)(3 * var4 / 4);
         var13[1][1] = (double)(var5 / 2);
         var13[1][2] = 1.0D;
         break;
      case 3:
         var13 = new double[3][3];
         var13[0][0] = (double)(var4 / 2);
         var13[0][1] = (double)(var5 / 4);
         var13[0][2] = 1.0D;
         var13[1][0] = (double)(var4 / 4);
         var13[1][1] = (double)(3 * var5 / 4);
         var13[1][2] = 1.0D;
         var13[2][0] = (double)(3 * var4 / 4);
         var13[2][1] = (double)(3 * var5 / 4);
         var13[2][2] = 1.0D;
         break;
      default:
         IJ.error("Unexpected transformation");
         return;
      }

      ImagePlus var9 = null;
      ImagePlus var10 = null;
      double[] var11 = null;
      switch(var1.getType()) {
      case 0:
         var10 = new ImagePlus("StackRegTarget", new ByteProcessor(var4, var5, new byte[var4 * var5], var1.getProcessor().getColorModel()));
         var10.getProcessor().copyBits(var1.getProcessor(), 0, 0, 0);
         break;
      case 1:
         var10 = new ImagePlus("StackRegTarget", new ShortProcessor(var4, var5, new short[var4 * var5], var1.getProcessor().getColorModel()));
         var10.getProcessor().copyBits(var1.getProcessor(), 0, 0, 0);
         break;
      case 2:
         var10 = new ImagePlus("StackRegTarget", new FloatProcessor(var4, var5, new float[var4 * var5], var1.getProcessor().getColorModel()));
         var10.getProcessor().copyBits(var1.getProcessor(), 0, 0, 0);
         break;
      case 3:
      case 4:
         var11 = this.getColorWeightsFromPrincipalComponents(var1);
         var1.setSlice(var6);
         var10 = this.getGray32("StackRegTarget", var1, var11);
         break;
      default:
         IJ.error("Unexpected image type");
         return;
      }

      int var12;
      for(var12 = var6 - 1; 0 < var12; --var12) {
         var9 = this.registerSlice(var9, var10, var1, var4, var5, 1, var7, var13, var11, var12, var2);
         if (var9 == null) {
            var1.setSlice(var6);
            return;
         }
      }

      if (1 < var6 && var6 < var1.getStackSize()) {
         var7[0][0] = 1.0D;
         var7[0][1] = 0.0D;
         var7[0][2] = 0.0D;
         var7[1][0] = 0.0D;
         var7[1][1] = 1.0D;
         var7[1][2] = 0.0D;
         var7[2][0] = 0.0D;
         var7[2][1] = 0.0D;
         var7[2][2] = 1.0D;
         var1.setSlice(var6);
         switch(var1.getType()) {
         case 0:
         case 1:
         case 2:
            var10.getProcessor().copyBits(var1.getProcessor(), 0, 0, 0);
            break;
         case 3:
         case 4:
            var10 = this.getGray32("StackRegTarget", var1, var11);
            break;
         default:
            IJ.error("Unexpected image type");
            return;
         }
      }

      for(var12 = var6 + 1; var12 <= var1.getStackSize(); ++var12) {
         var9 = this.registerSlice(var9, var10, var1, var4, var5, 1, var7, var13, var11, var12, var2);
         if (var9 == null) {
            var1.setSlice(var6);
            return;
         }
      }

      var1.setSlice(var6);
      IJ.showProgress(-1.0D);
      var1.updateAndDraw();
   }

   private void computeStatistics(ImagePlus var1, double[] var2, double[][] var3) {
      int var4 = var1.getWidth() * var1.getHeight();
      double var5;
      double var7;
      double var9;
      if (var1.getProcessor().getPixels() instanceof byte[]) {
         IndexColorModel var11 = (IndexColorModel)var1.getProcessor().getColorModel();
         int var12 = var11.getMapSize();
         byte[] var13 = new byte[var12];
         byte[] var14 = new byte[var12];
         byte[] var15 = new byte[var12];
         var11.getReds(var13);
         var11.getGreens(var14);
         var11.getBlues(var15);
         double[] var16 = new double[var12];

         int var17;
         for(var17 = 0; var17 < var12; ++var17) {
            var16[var17] = 0.0D;
         }

         for(var17 = 1; var17 <= var1.getStackSize(); ++var17) {
            var1.setSlice(var17);
            byte[] var18 = (byte[])var1.getProcessor().getPixels();

            for(int var19 = 0; var19 < var4; ++var19) {
               ++var16[var18[var19] & 255];
            }
         }

         for(var17 = 0; var17 < var12; ++var17) {
            var5 = (double)(var13[var17] & 255);
            var7 = (double)(var14[var17] & 255);
            var9 = (double)(var15[var17] & 255);
            var2[0] += var16[var17] * var5;
            var2[1] += var16[var17] * var7;
            var2[2] += var16[var17] * var9;
            var3[0][0] += var16[var17] * var5 * var5;
            var3[0][1] += var16[var17] * var5 * var7;
            var3[0][2] += var16[var17] * var5 * var9;
            var3[1][1] += var16[var17] * var7 * var7;
            var3[1][2] += var16[var17] * var7 * var9;
            var3[2][2] += var16[var17] * var9 * var9;
         }
      } else if (var1.getProcessor().getPixels() instanceof int[]) {
         for(int var20 = 1; var20 <= var1.getStackSize(); ++var20) {
            var1.setSlice(var20);
            int[] var21 = (int[])var1.getProcessor().getPixels();

            for(int var22 = 0; var22 < var4; ++var22) {
               var5 = (double)((var21[var22] & 16711680) >>> 16);
               var7 = (double)((var21[var22] & '\uff00') >>> 8);
               var9 = (double)(var21[var22] & 255);
               var2[0] += var5;
               var2[1] += var7;
               var2[2] += var9;
               var3[0][0] += var5 * var5;
               var3[0][1] += var5 * var7;
               var3[0][2] += var5 * var9;
               var3[1][1] += var7 * var7;
               var3[1][2] += var7 * var9;
               var3[2][2] += var9 * var9;
            }
         }
      } else {
         IJ.error("Internal type mismatch");
      }

      var4 *= var1.getStackSize();
      var2[0] /= (double)var4;
      var2[1] /= (double)var4;
      var2[2] /= (double)var4;
      var3[0][0] /= (double)var4;
      var3[0][1] /= (double)var4;
      var3[0][2] /= (double)var4;
      var3[1][1] /= (double)var4;
      var3[1][2] /= (double)var4;
      var3[2][2] /= (double)var4;
      var3[0][0] -= var2[0] * var2[0];
      var3[0][1] -= var2[0] * var2[1];
      var3[0][2] -= var2[0] * var2[2];
      var3[1][1] -= var2[1] * var2[1];
      var3[1][2] -= var2[1] * var2[2];
      var3[2][2] -= var2[2] * var2[2];
      var3[2][1] = var3[1][2];
      var3[2][0] = var3[0][2];
      var3[1][0] = var3[0][1];
   }

   private double[] getColorWeightsFromPrincipalComponents(ImagePlus var1) {
      double[] var2 = new double[]{0.0D, 0.0D, 0.0D};
      double[][] var3 = new double[][]{{0.0D, 0.0D, 0.0D}, {0.0D, 0.0D, 0.0D}, {0.0D, 0.0D, 0.0D}};
      this.computeStatistics(var1, var2, var3);
      double[] var4 = this.getEigenvalues(var3);
      if (var4[0] * var4[0] + var4[1] * var4[1] + var4[2] * var4[2] <= this.TINY) {
         return this.getLuminanceFromCCIR601();
      } else {
         double var5 = this.getLargestAbsoluteEigenvalue(var4);
         double[] var7 = this.getEigenvector(var3, var5);
         double var8 = var7[0] + var7[1] + var7[2];
         if (this.TINY < Math.abs(var8)) {
            var7[0] /= var8;
            var7[1] /= var8;
            var7[2] /= var8;
         }

         return var7;
      }
   }

   private double[] getEigenvalues(double[][] var1) {
      double[] var2 = new double[]{var1[0][0] * var1[1][1] * var1[2][2] + 2.0D * var1[0][1] * var1[1][2] * var1[2][0] - var1[0][1] * var1[0][1] * var1[2][2] - var1[1][2] * var1[1][2] * var1[0][0] - var1[2][0] * var1[2][0] * var1[1][1], var1[0][1] * var1[0][1] + var1[1][2] * var1[1][2] + var1[2][0] * var1[2][0] - var1[0][0] * var1[1][1] - var1[1][1] * var1[2][2] - var1[2][2] * var1[0][0], var1[0][0] + var1[1][1] + var1[2][2], -1.0D};
      double[] var3 = new double[3];
      double var4 = (3.0D * var2[1] - var2[2] * var2[2] / var2[3]) / (9.0D * var2[3]);
      double var6 = (var2[1] * var2[2] - 3.0D * var2[0] * var2[3] - 0.2222222222222222D * var2[2] * var2[2] * var2[2] / var2[3]) / (6.0D * var2[3] * var2[3]);
      double var8 = var4 * var4 * var4 + var6 * var6;
      double var10;
      if (var8 < 0.0D) {
         var8 = 2.0D * Math.sqrt(-var4);
         var6 /= Math.sqrt(-var4 * var4 * var4);
         var6 = 0.3333333333333333D * Math.acos(var6);
         var4 = 0.3333333333333333D * var2[2] / var2[3];
         var3[0] = var8 * Math.cos(var6) - var4;
         var3[1] = var8 * Math.cos(var6 + 2.0943951023931953D) - var4;
         var3[2] = var8 * Math.cos(var6 + 4.1887902047863905D) - var4;
         if (var3[0] < var3[1]) {
            if (var3[2] < var3[1]) {
               var10 = var3[1];
               var3[1] = var3[2];
               var3[2] = var10;
               if (var3[1] < var3[0]) {
                  var10 = var3[0];
                  var3[0] = var3[1];
                  var3[1] = var10;
               }
            }
         } else {
            var10 = var3[0];
            var3[0] = var3[1];
            var3[1] = var10;
            if (var3[2] < var3[1]) {
               var10 = var3[1];
               var3[1] = var3[2];
               var3[2] = var10;
               if (var3[1] < var3[0]) {
                  var10 = var3[0];
                  var3[0] = var3[1];
                  var3[1] = var10;
               }
            }
         }
      } else if (var8 == 0.0D) {
         var10 = 2.0D * (var6 < 0.0D ? Math.pow(-var6, 0.3333333333333333D) : Math.pow(var6, 0.3333333333333333D));
         var4 = 0.3333333333333333D * var2[2] / var2[3];
         if (var10 < 0.0D) {
            var3[0] = var10 - var4;
            var3[1] = -0.5D * var10 - var4;
            var3[2] = var3[1];
         } else {
            var3[0] = -0.5D * var10 - var4;
            var3[1] = var3[0];
            var3[2] = var10 - var4;
         }
      } else {
         IJ.error("Warning: complex eigenvalue found; ignoring imaginary part.");
         var8 = Math.sqrt(var8);
         var4 = var6 + var8 < 0.0D ? -Math.exp(0.3333333333333333D * Math.log(-var6 - var8)) : Math.exp(0.3333333333333333D * Math.log(var6 + var8));
         var6 = var4 + (var6 < var8 ? -Math.exp(0.3333333333333333D * Math.log(var8 - var6)) : Math.exp(0.3333333333333333D * Math.log(var6 - var8)));
         var4 = -0.3333333333333333D * var2[2] / var2[3];
         var8 = var4 + var6;
         var3[0] = var4 - var6 / 2.0D;
         var3[1] = var3[0];
         var3[2] = var3[1];
         if (var8 < var3[0]) {
            var3[0] = var8;
         } else {
            var3[2] = var8;
         }
      }

      return var3;
   }

   private double[] getEigenvector(double[][] var1, double var2) {
      int var4 = var1.length;
      double[][] var5 = new double[var4][var4];

      for(int var6 = 0; var6 < var4; ++var6) {
         System.arraycopy(var1[var6], 0, var5[var6], 0, var4);
         var5[var6][var6] -= var2;
      }

      double[] var20 = new double[var4];

      double var11;
      int var13;
      int var14;
      for(var13 = 0; var13 < var4; ++var13) {
         var11 = 0.0D;

         for(var14 = 0; var14 < var4; ++var14) {
            var11 += var5[var13][var14] * var5[var13][var14];
         }

         var11 = Math.sqrt(var11);
         if (this.TINY < var11) {
            for(var14 = 0; var14 < var4; ++var14) {
               var5[var13][var14] /= var11;
            }
         }
      }

      double var7;
      double var9;
      int var15;
      for(var13 = 0; var13 < var4; ++var13) {
         var9 = var5[var13][var13];
         var7 = Math.abs(var9);
         var14 = var13;

         for(var15 = var13 + 1; var15 < var4; ++var15) {
            if (var7 < Math.abs(var5[var15][var13])) {
               var9 = var5[var15][var13];
               var7 = Math.abs(var9);
               var14 = var15;
            }
         }

         if (var14 != var13) {
            double[] var22 = new double[var4 - var13];
            System.arraycopy(var5[var13], var13, var22, 0, var4 - var13);
            System.arraycopy(var5[var14], var13, var5[var13], var13, var4 - var13);
            System.arraycopy(var22, 0, var5[var14], var13, var4 - var13);
         }

         if (this.TINY < var7) {
            for(var14 = 0; var14 < var4; ++var14) {
               var5[var13][var14] /= var9;
            }
         }

         for(var15 = var13 + 1; var15 < var4; ++var15) {
            var9 = var5[var15][var13];

            for(var14 = 0; var14 < var4; ++var14) {
               var5[var15][var14] -= var9 * var5[var13][var14];
            }
         }
      }

      boolean[] var21 = new boolean[var4];
      var14 = var4;

      int var16;
      for(var15 = 0; var15 < var4; ++var15) {
         var21[var15] = false;
         if (Math.abs(var5[var15][var15]) < this.TINY) {
            var21[var15] = true;
            --var14;
            var20[var15] = 1.0D;
         } else {
            if (this.TINY < Math.abs(var5[var15][var15] - 1.0D)) {
               IJ.error("Insufficient accuracy.");
               var20[0] = 0.212671D;
               var20[1] = 0.71516D;
               var20[2] = 0.072169D;
               return var20;
            }

            var11 = 0.0D;

            for(var16 = 0; var16 < var15; ++var16) {
               var11 += var5[var15][var16] * var5[var15][var16];
            }

            for(var16 = var15 + 1; var16 < var4; ++var16) {
               var11 += var5[var15][var16] * var5[var15][var16];
            }

            if (Math.sqrt(var11) < this.TINY) {
               var21[var15] = true;
               --var14;
               var20[var15] = 0.0D;
            }
         }
      }

      if (0 < var14) {
         double[][] var23 = new double[var14][var14];
         var16 = 0;

         int var17;
         int var18;
         int var19;
         for(var17 = 0; var16 < var4; ++var16) {
            if (!var21[var16]) {
               var18 = 0;

               for(var19 = 0; var18 < var4; ++var18) {
                  if (!var21[var18]) {
                     var23[var17][var19] = var5[var16][var18];
                     ++var19;
                  }
               }

               ++var17;
            }
         }

         double[] var24 = new double[var14];
         var17 = 0;

         for(var18 = 0; var17 < var4; ++var17) {
            if (!var21[var17]) {
               for(var19 = 0; var19 < var4; ++var19) {
                  if (var21[var19]) {
                     var24[var18] -= var5[var17][var19] * var20[var19];
                  }
               }

               ++var18;
            }
         }

         var24 = this.linearLeastSquares(var23, var24);
         var17 = 0;

         for(var18 = 0; var17 < var4; ++var17) {
            if (!var21[var17]) {
               var20[var17] = var24[var18];
               ++var18;
            }
         }
      }

      var11 = 0.0D;

      for(var15 = 0; var15 < var4; ++var15) {
         var11 += var20[var15] * var20[var15];
      }

      var11 = Math.sqrt(var11);
      if (Math.sqrt(var11) < this.TINY) {
         IJ.error("Insufficient accuracy.");
         var20[0] = 0.212671D;
         var20[1] = 0.71516D;
         var20[2] = 0.072169D;
         return var20;
      } else {
         var7 = Math.abs(var20[0]);
         var14 = 0;

         for(var15 = 1; var15 < var4; ++var15) {
            var9 = Math.abs(var20[var15]);
            if (var7 < var9) {
               var7 = var9;
               var14 = var15;
            }
         }

         var11 = var20[var14] < 0.0D ? -var11 : var11;

         for(var15 = 0; var15 < var4; ++var15) {
            var20[var15] /= var11;
         }

         return var20;
      }
   }

   private ImagePlus getGray32(String var1, ImagePlus var2, double[] var3) {
      int var4 = var2.getWidth() * var2.getHeight();
      ImagePlus var5 = new ImagePlus(var1, new FloatProcessor(var2.getWidth(), var2.getHeight()));
      float[] var6 = (float[])var5.getProcessor().getPixels();
      double var7;
      double var9;
      double var11;
      if (var2.getProcessor().getPixels() instanceof byte[]) {
         byte[] var13 = (byte[])var2.getProcessor().getPixels();
         IndexColorModel var14 = (IndexColorModel)var2.getProcessor().getColorModel();
         int var15 = var14.getMapSize();
         byte[] var16 = new byte[var15];
         byte[] var17 = new byte[var15];
         byte[] var18 = new byte[var15];
         var14.getReds(var16);
         var14.getGreens(var17);
         var14.getBlues(var18);

         for(int var20 = 0; var20 < var4; ++var20) {
            int var19 = var13[var20] & 255;
            var7 = (double)(var16[var19] & 255);
            var9 = (double)(var17[var19] & 255);
            var11 = (double)(var18[var19] & 255);
            var6[var20] = (float)(var3[0] * var7 + var3[1] * var9 + var3[2] * var11);
         }
      } else if (var2.getProcessor().getPixels() instanceof int[]) {
         int[] var21 = (int[])var2.getProcessor().getPixels();

         for(int var22 = 0; var22 < var4; ++var22) {
            var7 = (double)((var21[var22] & 16711680) >>> 16);
            var9 = (double)((var21[var22] & '\uff00') >>> 8);
            var11 = (double)(var21[var22] & 255);
            var6[var22] = (float)(var3[0] * var7 + var3[1] * var9 + var3[2] * var11);
         }
      }

      return var5;
   }

   private double getLargestAbsoluteEigenvalue(double[] var1) {
      double var2 = var1[0];

      for(int var4 = 1; var4 < var1.length; ++var4) {
         if (Math.abs(var2) < Math.abs(var1[var4])) {
            var2 = var1[var4];
         }

         if (Math.abs(var2) == Math.abs(var1[var4]) && var2 < var1[var4]) {
            var2 = var1[var4];
         }
      }

      return var2;
   }

   private double[] getLuminanceFromCCIR601() {
      double[] var1 = new double[]{0.299D, 0.587D, 0.114D};
      return var1;
   }

   private double[][] getTransformationMatrix(double[][] var1, double[][] var2, int var3) {
      double[][] var4;
      var4 = new double[3][3];
      double[][] var5;
      double[] var6;
      int var7;
      int var8;
      label81:
      switch(var3) {
      case 0:
         var4[0][0] = 1.0D;
         var4[0][1] = 0.0D;
         var4[0][2] = var2[0][0] - var1[0][0];
         var4[1][0] = 0.0D;
         var4[1][1] = 1.0D;
         var4[1][2] = var2[0][1] - var1[0][1];
         break;
      case 1:
         double var11 = Math.atan2(var1[2][0] - var1[1][0], var1[2][1] - var1[1][1]) - Math.atan2(var2[2][0] - var2[1][0], var2[2][1] - var2[1][1]);
         double var12 = Math.cos(var11);
         double var9 = Math.sin(var11);
         var4[0][0] = var12;
         var4[0][1] = -var9;
         var4[0][2] = var2[0][0] - var12 * var1[0][0] + var9 * var1[0][1];
         var4[1][0] = var9;
         var4[1][1] = var12;
         var4[1][2] = var2[0][1] - var9 * var1[0][0] - var12 * var1[0][1];
         break;
      case 2:
         var5 = new double[3][3];
         var6 = new double[3];
         var5[0][0] = var1[0][0];
         var5[0][1] = var1[0][1];
         var5[0][2] = 1.0D;
         var5[1][0] = var1[1][0];
         var5[1][1] = var1[1][1];
         var5[1][2] = 1.0D;
         var5[2][0] = var1[0][1] - var1[1][1] + var1[1][0];
         var5[2][1] = var1[1][0] + var1[1][1] - var1[0][0];
         var5[2][2] = 1.0D;
         this.invertGauss(var5);
         var6[0] = var2[0][0];
         var6[1] = var2[1][0];
         var6[2] = var2[0][1] - var2[1][1] + var2[1][0];

         for(var7 = 0; var7 < 3; ++var7) {
            var4[0][var7] = 0.0D;

            for(var8 = 0; var8 < 3; ++var8) {
               var4[0][var7] += var5[var7][var8] * var6[var8];
            }
         }

         var6[0] = var2[0][1];
         var6[1] = var2[1][1];
         var6[2] = var2[1][0] + var2[1][1] - var2[0][0];
         var7 = 0;

         while(true) {
            if (var7 >= 3) {
               break label81;
            }

            var4[1][var7] = 0.0D;

            for(var8 = 0; var8 < 3; ++var8) {
               var4[1][var7] += var5[var7][var8] * var6[var8];
            }

            ++var7;
         }
      case 3:
         var5 = new double[3][3];
         var6 = new double[3];
         var5[0][0] = var1[0][0];
         var5[0][1] = var1[0][1];
         var5[0][2] = 1.0D;
         var5[1][0] = var1[1][0];
         var5[1][1] = var1[1][1];
         var5[1][2] = 1.0D;
         var5[2][0] = var1[2][0];
         var5[2][1] = var1[2][1];
         var5[2][2] = 1.0D;
         this.invertGauss(var5);
         var6[0] = var2[0][0];
         var6[1] = var2[1][0];
         var6[2] = var2[2][0];

         for(var7 = 0; var7 < 3; ++var7) {
            var4[0][var7] = 0.0D;

            for(var8 = 0; var8 < 3; ++var8) {
               var4[0][var7] += var5[var7][var8] * var6[var8];
            }
         }

         var6[0] = var2[0][1];
         var6[1] = var2[1][1];
         var6[2] = var2[2][1];
         var7 = 0;

         while(true) {
            if (var7 >= 3) {
               break label81;
            }

            var4[1][var7] = 0.0D;

            for(var8 = 0; var8 < 3; ++var8) {
               var4[1][var7] += var5[var7][var8] * var6[var8];
            }

            ++var7;
         }
      default:
         IJ.error("Unexpected transformation");
      }

      var4[2][0] = 0.0D;
      var4[2][1] = 0.0D;
      var4[2][2] = 1.0D;
      return var4;
   }

   private void invertGauss(double[][] var1) {
      int var2 = var1.length;
      double[][] var3 = new double[var2][var2];

      int var4;
      double var5;
      double var7;
      int var9;
      for(var4 = 0; var4 < var2; ++var4) {
         var5 = var1[var4][0];
         var7 = Math.abs(var5);

         for(var9 = 0; var9 < var2; ++var9) {
            var3[var4][var9] = 0.0D;
            if (var7 < Math.abs(var1[var4][var9])) {
               var5 = var1[var4][var9];
               var7 = Math.abs(var5);
            }
         }

         var3[var4][var4] = 1.0D / var5;

         for(var9 = 0; var9 < var2; ++var9) {
            var1[var4][var9] /= var5;
         }
      }

      for(var4 = 0; var4 < var2; ++var4) {
         var5 = var1[var4][var4];
         var7 = Math.abs(var5);
         var9 = var4;

         int var10;
         for(var10 = var4 + 1; var10 < var2; ++var10) {
            if (var7 < Math.abs(var1[var10][var4])) {
               var5 = var1[var10][var4];
               var7 = Math.abs(var5);
               var9 = var10;
            }
         }

         if (var9 != var4) {
            double[] var13 = new double[var2 - var4];
            double[] var11 = new double[var2];
            System.arraycopy(var1[var4], var4, var13, 0, var2 - var4);
            System.arraycopy(var1[var9], var4, var1[var4], var4, var2 - var4);
            System.arraycopy(var13, 0, var1[var9], var4, var2 - var4);
            System.arraycopy(var3[var4], 0, var11, 0, var2);
            System.arraycopy(var3[var9], 0, var3[var4], 0, var2);
            System.arraycopy(var11, 0, var3[var9], 0, var2);
         }

         for(var9 = 0; var9 <= var4; ++var9) {
            var3[var4][var9] /= var5;
         }

         for(var9 = var4 + 1; var9 < var2; ++var9) {
            var1[var4][var9] /= var5;
            var3[var4][var9] /= var5;
         }

         for(var10 = var4 + 1; var10 < var2; ++var10) {
            for(var9 = 0; var9 <= var4; ++var9) {
               var3[var10][var9] -= var1[var10][var4] * var3[var4][var9];
            }

            for(var9 = var4 + 1; var9 < var2; ++var9) {
               var1[var10][var9] -= var1[var10][var4] * var1[var4][var9];
               var3[var10][var9] -= var1[var10][var4] * var3[var4][var9];
            }
         }
      }

      for(var4 = var2 - 1; 1 <= var4; --var4) {
         for(int var12 = var4 - 1; 0 <= var12; --var12) {
            int var6;
            for(var6 = 0; var6 <= var4; ++var6) {
               var3[var12][var6] -= var1[var12][var4] * var3[var4][var6];
            }

            for(var6 = var4 + 1; var6 < var2; ++var6) {
               var1[var12][var6] -= var1[var12][var4] * var1[var4][var6];
               var3[var12][var6] -= var1[var12][var4] * var3[var4][var6];
            }
         }
      }

      for(var4 = 0; var4 < var2; ++var4) {
         System.arraycopy(var3[var4], 0, var1[var4], 0, var2);
      }

   }

   private double[] linearLeastSquares(double[][] var1, double[] var2) {
      int var3 = var1.length;
      int var4 = var1[0].length;
      double[][] var5 = new double[var3][var4];
      double[][] var6 = new double[var4][var4];
      double[] var7 = new double[var4];

      int var10;
      int var11;
      for(var10 = 0; var10 < var3; ++var10) {
         for(var11 = 0; var11 < var4; ++var11) {
            var5[var10][var11] = var1[var10][var11];
         }
      }

      this.QRdecomposition(var5, var6);

      double var8;
      for(var10 = 0; var10 < var4; ++var10) {
         var8 = 0.0D;

         for(var11 = 0; var11 < var3; ++var11) {
            var8 += var5[var11][var10] * var2[var11];
         }

         var7[var10] = var8;
      }

      for(var10 = var4 - 1; 0 <= var10; --var10) {
         var8 = var6[var10][var10];
         if (var8 * var8 == 0.0D) {
            var7[var10] = 0.0D;
         } else {
            var7[var10] /= var8;
         }

         for(var11 = var10 - 1; 0 <= var11; --var11) {
            var7[var11] -= var6[var11][var10] * var7[var10];
         }
      }

      return var7;
   }

   private void QRdecomposition(double[][] var1, double[][] var2) {
      int var3 = var1.length;
      int var4 = var1[0].length;
      double[][] var5 = new double[var3][var4];

      int var8;
      int var9;
      int var10;
      for(var8 = 0; var8 < var4; ++var8) {
         for(var9 = 0; var9 < var3; ++var9) {
            var5[var9][var8] = var1[var9][var8];
         }

         double var6;
         for(var9 = 0; var9 < var8; ++var9) {
            var6 = 0.0D;

            for(var10 = 0; var10 < var3; ++var10) {
               var6 += var5[var10][var8] * var1[var10][var9];
            }

            for(var10 = 0; var10 < var3; ++var10) {
               var1[var10][var8] -= var6 * var1[var10][var9];
            }
         }

         var6 = 0.0D;

         for(var9 = 0; var9 < var3; ++var9) {
            var6 += var1[var9][var8] * var1[var9][var8];
         }

         if (var6 * var6 == 0.0D) {
            var6 = 0.0D;
         } else {
            var6 = 1.0D / Math.sqrt(var6);
         }

         for(var9 = 0; var9 < var3; ++var9) {
            var1[var9][var8] *= var6;
         }
      }

      for(var8 = 0; var8 < var4; ++var8) {
         for(var9 = 0; var9 < var8; ++var9) {
            var2[var8][var9] = 0.0D;
         }

         for(var9 = var8; var9 < var4; ++var9) {
            var2[var8][var9] = 0.0D;

            for(var10 = 0; var10 < var3; ++var10) {
               var2[var8][var9] += var1[var10][var8] * var5[var10][var9];
            }
         }
      }

   }

   private ImagePlus registerSlice(ImagePlus var1, ImagePlus var2, ImagePlus var3, int var4, int var5, int var6, double[][] var7, double[][] var8, double[] var9, int var10, int var11) {
      var3.setSlice(var10);

      try {
         Object var12 = null;
         Method var13 = null;
         Object var14 = null;
         Object var15 = null;
         Object var16 = null;
         switch(var3.getType()) {
         case 0:
            var1 = new ImagePlus("StackRegSource", new ByteProcessor(var4, var5, (byte[])var3.getProcessor().getPixels(), var3.getProcessor().getColorModel()));
            break;
         case 1:
            var1 = new ImagePlus("StackRegSource", new ShortProcessor(var4, var5, (short[])var3.getProcessor().getPixels(), var3.getProcessor().getColorModel()));
            break;
         case 2:
            var1 = new ImagePlus("StackRegSource", new FloatProcessor(var4, var5, (float[])var3.getProcessor().getPixels(), var3.getProcessor().getColorModel()));
            break;
         case 3:
         case 4:
            var1 = this.getGray32("StackRegSource", var3, var9);
            break;
         default:
            IJ.error("Unexpected image type");
            return null;
         }

         FileSaver var17 = new FileSaver(var1);
         String var18 = IJ.getDirectory("temp") + var1.getTitle() + "_" + var11 + ".tif";
         var17.saveAsTiff(var18);
         FileSaver var19 = new FileSaver(var2);
         String var20 = IJ.getDirectory("temp") + var2.getTitle() + "_" + var11 + ".tif";
         var19.saveAsTiff(var20);
         switch(var6) {
         case 0:
            var12 = IJ.runPlugIn("TurboReg_", "-align -file " + var18 + " 0 0 " + (var4 - 1) + " " + (var5 - 1) + " -file " + var20 + " 0 0 " + (var4 - 1) + " " + (var5 - 1) + " -translation " + var4 / 2 + " " + var5 / 2 + " " + var4 / 2 + " " + var5 / 2 + " -hideOutput");
            break;
         case 1:
            var12 = IJ.runPlugIn("TurboReg_", "-align -file " + var18 + " 0 0 " + (var4 - 1) + " " + (var5 - 1) + " -file " + var20 + " 0 0 " + (var4 - 1) + " " + (var5 - 1) + " -rigidBody " + var4 / 2 + " " + var5 / 2 + " " + var4 / 2 + " " + var5 / 2 + " " + var4 / 2 + " " + var5 / 4 + " " + var4 / 2 + " " + var5 / 4 + " " + var4 / 2 + " " + 3 * var5 / 4 + " " + var4 / 2 + " " + 3 * var5 / 4 + " -hideOutput");
            break;
         case 2:
            var12 = IJ.runPlugIn("TurboReg_", "-align -file " + var18 + " 0 0 " + (var4 - 1) + " " + (var5 - 1) + " -file " + var20 + " 0 0 " + (var4 - 1) + " " + (var5 - 1) + " -scaledRotation " + var4 / 4 + " " + var5 / 2 + " " + var4 / 4 + " " + var5 / 2 + " " + 3 * var4 / 4 + " " + var5 / 2 + " " + 3 * var4 / 4 + " " + var5 / 2 + " -hideOutput");
            break;
         case 3:
            var12 = IJ.runPlugIn("TurboReg_", "-align -file " + var18 + " 0 0 " + (var4 - 1) + " " + (var5 - 1) + " -file " + var20 + " 0 0 " + (var4 - 1) + " " + (var5 - 1) + " -affine " + var4 / 2 + " " + var5 / 4 + " " + var4 / 2 + " " + var5 / 4 + " " + var4 / 4 + " " + 3 * var5 / 4 + " " + var4 / 4 + " " + 3 * var5 / 4 + " " + 3 * var4 / 4 + " " + 3 * var5 / 4 + " " + 3 * var4 / 4 + " " + 3 * var5 / 4 + " -hideOutput");
            break;
         default:
            IJ.error("Unexpected transformation");
            return null;
         }

         if (var12 == null) {
            throw new ClassNotFoundException();
         } else {
            var2.setProcessor((String)null, var1.getProcessor());
            var13 = var12.getClass().getMethod("getSourcePoints", (Class[])null);
            double[][] var52 = (double[][])var13.invoke(var12);
            var13 = var12.getClass().getMethod("getTargetPoints", (Class[])null);
            double[][] var53 = (double[][])var13.invoke(var12);
            double[][] var54 = this.getTransformationMatrix(var53, var52, var6);
            double[][] var21 = new double[][]{{var7[0][0], var7[0][1], var7[0][2]}, {var7[1][0], var7[1][1], var7[1][2]}, {var7[2][0], var7[2][1], var7[2][2]}};

            int var22;
            int var23;
            for(var22 = 0; var22 < 3; ++var22) {
               for(var23 = 0; var23 < 3; ++var23) {
                  var7[var22][var23] = 0.0D;

                  for(int var24 = 0; var24 < 3; ++var24) {
                     var7[var22][var23] += var54[var22][var24] * var21[var24][var23];
                  }
               }
            }

            byte[] var26;
            byte[] var27;
            ImagePlus var29;
            ImagePlus var30;
            ImagePlus var31;
            ImagePlus var32;
            ImagePlus var33;
            FileSaver var34;
            int var41;
            ImageConverter var42;
            Object var56;
            Object var57;
            ImageConverter var71;
            switch(var3.getType()) {
            case 0:
            case 1:
            case 2:
               switch(var6) {
               case 0:
                  var52 = new double[1][3];

                  for(var22 = 0; var22 < 3; ++var22) {
                     var52[0][var22] = 0.0D;

                     for(var23 = 0; var23 < 3; ++var23) {
                        var52[0][var22] += var7[var22][var23] * var8[0][var23];
                     }
                  }

                  var12 = IJ.runPlugIn("TurboReg_", "-transform -file " + var18 + " " + var4 + " " + var5 + " -translation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " -hideOutput");
                  return var1;
               case 1:
                  var52 = new double[3][3];

                  for(var22 = 0; var22 < 3; ++var22) {
                     var52[0][var22] = 0.0D;
                     var52[1][var22] = 0.0D;
                     var52[2][var22] = 0.0D;

                     for(var23 = 0; var23 < 3; ++var23) {
                        var52[0][var22] += var7[var22][var23] * var8[0][var23];
                        var52[1][var22] += var7[var22][var23] * var8[1][var23];
                        var52[2][var22] += var7[var22][var23] * var8[2][var23];
                     }
                  }

                  Transformer var60 = new Transformer();
                  var60.setSize(var4, var5);
                  var60.setSource(var52);
                  this.transformers.add(var60);
                  return var1;
               case 2:
                  var52 = new double[2][3];

                  for(var22 = 0; var22 < 3; ++var22) {
                     var52[0][var22] = 0.0D;
                     var52[1][var22] = 0.0D;

                     for(var23 = 0; var23 < 3; ++var23) {
                        var52[0][var22] += var7[var22][var23] * var8[0][var23];
                        var52[1][var22] += var7[var22][var23] * var8[1][var23];
                     }
                  }

                  var12 = IJ.runPlugIn("TurboReg_", "-transform -file " + var18 + " " + var4 + " " + var5 + " -scaledRotation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 4 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + 3 * var4 / 4 + " " + var5 / 2 + " -hideOutput");
                  return var1;
               case 3:
                  var52 = new double[3][3];

                  for(var22 = 0; var22 < 3; ++var22) {
                     var52[0][var22] = 0.0D;
                     var52[1][var22] = 0.0D;
                     var52[2][var22] = 0.0D;

                     for(var23 = 0; var23 < 3; ++var23) {
                        var52[0][var22] += var7[var22][var23] * var8[0][var23];
                        var52[1][var22] += var7[var22][var23] * var8[1][var23];
                        var52[2][var22] += var7[var22][var23] * var8[2][var23];
                     }
                  }

                  var12 = IJ.runPlugIn("TurboReg_", "-transform -file " + var18 + " " + var4 + " " + var5 + " -affine " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 4 + " " + 3 * var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + 3 * var4 / 4 + " " + 3 * var5 / 4 + " -hideOutput");
                  return var1;
               default:
                  IJ.error("Unexpected transformation");
                  return null;
               }
            case 3:
               var1 = new ImagePlus("StackRegSource", new ByteProcessor(var4, var5, (byte[])var3.getProcessor().getPixels(), var3.getProcessor().getColorModel()));
               ImageConverter var58 = new ImageConverter(var1);
               var58.convertToRGB();
               var56 = null;
               var57 = null;
               Object var59 = null;
               var26 = new byte[var4 * var5];
               var27 = new byte[var4 * var5];
               byte[] var61 = new byte[var4 * var5];
               ((ColorProcessor)var1.getProcessor()).getRGB(var26, var27, var61);
               var29 = new ImagePlus("StackRegSourceR", new ByteProcessor(var4, var5));
               var30 = new ImagePlus("StackRegSourceG", new ByteProcessor(var4, var5));
               var31 = new ImagePlus("StackRegSourceB", new ByteProcessor(var4, var5));
               var29.getProcessor().setPixels(var26);
               var30.getProcessor().setPixels(var27);
               var31.getProcessor().setPixels(var61);
               var32 = null;
               var33 = null;
               var34 = null;
               FileSaver var63 = new FileSaver(var29);
               String var64 = IJ.getDirectory("temp") + var29.getTitle() + "_" + var11 + ".tif";
               var63.saveAsTiff(var64);
               FileSaver var65 = new FileSaver(var30);
               String var66 = IJ.getDirectory("temp") + var30.getTitle() + "_" + var11 + ".tif";
               var65.saveAsTiff(var66);
               FileSaver var67 = new FileSaver(var31);
               String var70 = IJ.getDirectory("temp") + var31.getTitle() + "_" + var11 + ".tif";
               var67.saveAsTiff(var70);
               int var68;
               switch(var6) {
               case 0:
                  var52 = new double[1][3];

                  for(var41 = 0; var41 < 3; ++var41) {
                     var52[0][var41] = 0.0D;

                     for(var68 = 0; var68 < 3; ++var68) {
                        var52[0][var41] += var7[var41][var68] * var8[0][var68];
                     }
                  }

                  var56 = IJ.runPlugIn("TurboReg_", "-transform -file " + var64 + " " + var4 + " " + var5 + " -translation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " -hideOutput");
                  if (var56 == null) {
                     throw new ClassNotFoundException();
                  }

                  var57 = IJ.runPlugIn("TurboReg_", "-transform -file " + var66 + " " + var4 + " " + var5 + " -translation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " -hideOutput");
                  var59 = IJ.runPlugIn("TurboReg_", "-transform -file " + var70 + " " + var4 + " " + var5 + " -translation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " -hideOutput");
                  break;
               case 1:
                  var52 = new double[3][3];

                  for(var41 = 0; var41 < 3; ++var41) {
                     var52[0][var41] = 0.0D;
                     var52[1][var41] = 0.0D;
                     var52[2][var41] = 0.0D;

                     for(var68 = 0; var68 < 3; ++var68) {
                        var52[0][var41] += var7[var41][var68] * var8[0][var68];
                        var52[1][var41] += var7[var41][var68] * var8[1][var68];
                        var52[2][var41] += var7[var41][var68] * var8[2][var68];
                     }
                  }

                  var56 = IJ.runPlugIn("TurboReg_", "-transform -file " + var64 + " " + var4 + " " + var5 + " -rigidBody " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + var4 / 2 + " " + 3 * var5 / 4 + " -hideOutput");
                  if (var56 == null) {
                     throw new ClassNotFoundException();
                  }

                  var57 = IJ.runPlugIn("TurboReg_", "-transform -file " + var66 + " " + var4 + " " + var5 + " -rigidBody " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + var4 / 2 + " " + 3 * var5 / 4 + " -hideOutput");
                  var59 = IJ.runPlugIn("TurboReg_", "-transform -file " + var70 + " " + var4 + " " + var5 + " -rigidBody " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + var4 / 2 + " " + 3 * var5 / 4 + " -hideOutput");
                  break;
               case 2:
                  var52 = new double[2][3];

                  for(var41 = 0; var41 < 3; ++var41) {
                     var52[0][var41] = 0.0D;
                     var52[1][var41] = 0.0D;

                     for(var68 = 0; var68 < 3; ++var68) {
                        var52[0][var41] += var7[var41][var68] * var8[0][var68];
                        var52[1][var41] += var7[var41][var68] * var8[1][var68];
                     }
                  }

                  var56 = IJ.runPlugIn("TurboReg_", "-transform -file " + var64 + " " + var4 + " " + var5 + " -scaledRotation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 4 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + 3 * var4 / 4 + " " + var5 / 2 + " -hideOutput");
                  if (var56 == null) {
                     throw new ClassNotFoundException();
                  }

                  var57 = IJ.runPlugIn("TurboReg_", "-transform -file " + var66 + " " + var4 + " " + var5 + " -scaledRotation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 4 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + 3 * var4 / 4 + " " + var5 / 2 + " -hideOutput");
                  var59 = IJ.runPlugIn("TurboReg_", "-transform -file " + var70 + " " + var4 + " " + var5 + " -scaledRotation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 4 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + 3 * var4 / 4 + " " + var5 / 2 + " -hideOutput");
                  break;
               case 3:
                  var52 = new double[3][3];

                  for(var41 = 0; var41 < 3; ++var41) {
                     var52[0][var41] = 0.0D;
                     var52[1][var41] = 0.0D;
                     var52[2][var41] = 0.0D;

                     for(var68 = 0; var68 < 3; ++var68) {
                        var52[0][var41] += var7[var41][var68] * var8[0][var68];
                        var52[1][var41] += var7[var41][var68] * var8[1][var68];
                        var52[2][var41] += var7[var41][var68] * var8[2][var68];
                     }
                  }

                  var56 = IJ.runPlugIn("TurboReg_", "-transform -file " + var64 + " " + var4 + " " + var5 + " -affine " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 4 + " " + 3 * var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + 3 * var4 / 4 + " " + 3 * var5 / 4 + " -hideOutput");
                  if (var56 == null) {
                     throw new ClassNotFoundException();
                  }

                  var57 = IJ.runPlugIn("TurboReg_", "-transform -file " + var66 + " " + var4 + " " + var5 + " -affine " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 4 + " " + 3 * var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + 3 * var4 / 4 + " " + 3 * var5 / 4 + " -hideOutput");
                  var59 = IJ.runPlugIn("TurboReg_", "-transform -file " + var70 + " " + var4 + " " + var5 + " -affine " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 4 + " " + 3 * var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + 3 * var4 / 4 + " " + 3 * var5 / 4 + " -hideOutput");
                  break;
               default:
                  IJ.error("Unexpected transformation");
                  return null;
               }

               var13 = var56.getClass().getMethod("getTransformedImage", (Class[])null);
               var32 = (ImagePlus)var13.invoke(var56);
               var13 = var57.getClass().getMethod("getTransformedImage", (Class[])null);
               var33 = (ImagePlus)var13.invoke(var57);
               var13 = var59.getClass().getMethod("getTransformedImage", (Class[])null);
               ImagePlus var62 = (ImagePlus)var13.invoke(var59);
               var32.getStack().deleteLastSlice();
               var33.getStack().deleteLastSlice();
               var62.getStack().deleteLastSlice();
               var32.getProcessor().setMinAndMax(0.0D, 255.0D);
               var33.getProcessor().setMinAndMax(0.0D, 255.0D);
               var62.getProcessor().setMinAndMax(0.0D, 255.0D);
               var71 = new ImageConverter(var32);
               var42 = new ImageConverter(var33);
               ImageConverter var43 = new ImageConverter(var62);
               var71.convertToGray8();
               var42.convertToGray8();
               var43.convertToGray8();
               IndexColorModel var44 = (IndexColorModel)var3.getProcessor().getColorModel();
               byte[] var45 = (byte[])var3.getProcessor().getPixels();
               var26 = (byte[])var32.getProcessor().getPixels();
               var27 = (byte[])var33.getProcessor().getPixels();
               var61 = (byte[])var62.getProcessor().getPixels();
               int[] var46 = new int[4];
               var46[3] = 255;

               for(int var47 = 0; var47 < var45.length; ++var47) {
                  var46[0] = var26[var47] & 255;
                  var46[1] = var27[var47] & 255;
                  var46[2] = var61[var47] & 255;
                  var45[var47] = (byte)var44.getDataElement(var46, 0);
               }
               break;
            case 4:
               Object var55 = null;
               var56 = null;
               var57 = null;
               byte[] var25 = new byte[var4 * var5];
               var26 = new byte[var4 * var5];
               var27 = new byte[var4 * var5];
               ((ColorProcessor)var3.getProcessor()).getRGB(var25, var26, var27);
               ImagePlus var28 = new ImagePlus("StackRegSourceR", new ByteProcessor(var4, var5));
               var29 = new ImagePlus("StackRegSourceG", new ByteProcessor(var4, var5));
               var30 = new ImagePlus("StackRegSourceB", new ByteProcessor(var4, var5));
               var28.getProcessor().setPixels(var25);
               var29.getProcessor().setPixels(var26);
               var30.getProcessor().setPixels(var27);
               var31 = null;
               var32 = null;
               var33 = null;
               var34 = new FileSaver(var28);
               String var35 = IJ.getDirectory("temp") + var28.getTitle() + "_" + var11 + ".tif";
               var34.saveAsTiff(var35);
               FileSaver var36 = new FileSaver(var29);
               String var37 = IJ.getDirectory("temp") + var29.getTitle() + "_" + var11 + ".tif";
               var36.saveAsTiff(var37);
               FileSaver var38 = new FileSaver(var30);
               String var39 = IJ.getDirectory("temp") + var30.getTitle() + "_" + var11 + ".tif";
               var38.saveAsTiff(var39);
               int var40;
               switch(var6) {
               case 0:
                  var52 = new double[1][3];

                  for(var40 = 0; var40 < 3; ++var40) {
                     var52[0][var40] = 0.0D;

                     for(var41 = 0; var41 < 3; ++var41) {
                        var52[0][var40] += var7[var40][var41] * var8[0][var41];
                     }
                  }

                  var55 = IJ.runPlugIn("TurboReg_", "-transform -file " + var35 + " " + var4 + " " + var5 + " -translation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " -hideOutput");
                  if (var55 == null) {
                     throw new ClassNotFoundException();
                  }

                  var56 = IJ.runPlugIn("TurboReg_", "-transform -file " + var37 + " " + var4 + " " + var5 + " -translation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " -hideOutput");
                  var57 = IJ.runPlugIn("TurboReg_", "-transform -file " + var39 + " " + var4 + " " + var5 + " -translation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " -hideOutput");
                  break;
               case 1:
                  var52 = new double[3][3];

                  for(var40 = 0; var40 < 3; ++var40) {
                     var52[0][var40] = 0.0D;
                     var52[1][var40] = 0.0D;
                     var52[2][var40] = 0.0D;

                     for(var41 = 0; var41 < 3; ++var41) {
                        var52[0][var40] += var7[var40][var41] * var8[0][var41];
                        var52[1][var40] += var7[var40][var41] * var8[1][var41];
                        var52[2][var40] += var7[var40][var41] * var8[2][var41];
                     }
                  }

                  var55 = IJ.runPlugIn("TurboReg_", "-transform -file " + var35 + " " + var4 + " " + var5 + " -rigidBody " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + var4 / 2 + " " + 3 * var5 / 4 + " -hideOutput");
                  if (var55 == null) {
                     throw new ClassNotFoundException();
                  }

                  var56 = IJ.runPlugIn("TurboReg_", "-transform -file " + var37 + " " + var4 + " " + var5 + " -rigidBody " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + var4 / 2 + " " + 3 * var5 / 4 + " -hideOutput");
                  var57 = IJ.runPlugIn("TurboReg_", "-transform -file " + var39 + " " + var4 + " " + var5 + " -rigidBody " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + var4 / 2 + " " + 3 * var5 / 4 + " -hideOutput");
                  break;
               case 2:
                  var52 = new double[2][3];

                  for(var40 = 0; var40 < 3; ++var40) {
                     var52[0][var40] = 0.0D;
                     var52[1][var40] = 0.0D;

                     for(var41 = 0; var41 < 3; ++var41) {
                        var52[0][var40] += var7[var40][var41] * var8[0][var41];
                        var52[1][var40] += var7[var40][var41] * var8[1][var41];
                     }
                  }

                  var55 = IJ.runPlugIn("TurboReg_", "-transform -file " + var35 + " " + var4 + " " + var5 + " -scaledRotation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 4 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + 3 * var4 / 4 + " " + var5 / 2 + " -hideOutput");
                  if (var55 == null) {
                     throw new ClassNotFoundException();
                  }

                  var56 = IJ.runPlugIn("TurboReg_", "-transform -file " + var37 + " " + var4 + " " + var5 + " -scaledRotation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 4 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + 3 * var4 / 4 + " " + var5 / 2 + " -hideOutput");
                  var57 = IJ.runPlugIn("TurboReg_", "-transform -file " + var39 + " " + var4 + " " + var5 + " -scaledRotation " + var52[0][0] + " " + var52[0][1] + " " + var4 / 4 + " " + var5 / 2 + " " + var52[1][0] + " " + var52[1][1] + " " + 3 * var4 / 4 + " " + var5 / 2 + " -hideOutput");
                  break;
               case 3:
                  var52 = new double[3][3];

                  for(var40 = 0; var40 < 3; ++var40) {
                     var52[0][var40] = 0.0D;
                     var52[1][var40] = 0.0D;
                     var52[2][var40] = 0.0D;

                     for(var41 = 0; var41 < 3; ++var41) {
                        var52[0][var40] += var7[var40][var41] * var8[0][var41];
                        var52[1][var40] += var7[var40][var41] * var8[1][var41];
                        var52[2][var40] += var7[var40][var41] * var8[2][var41];
                     }
                  }

                  var55 = IJ.runPlugIn("TurboReg_", "-transform -file " + var35 + " " + var4 + " " + var5 + " -affine " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 4 + " " + 3 * var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + 3 * var4 / 4 + " " + 3 * var5 / 4 + " -hideOutput");
                  if (var55 == null) {
                     throw new ClassNotFoundException();
                  }

                  var56 = IJ.runPlugIn("TurboReg_", "-transform -file " + var37 + " " + var4 + " " + var5 + " -affine " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 4 + " " + 3 * var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + 3 * var4 / 4 + " " + 3 * var5 / 4 + " -hideOutput");
                  var57 = IJ.runPlugIn("TurboReg_", "-transform -file " + var39 + " " + var4 + " " + var5 + " -affine " + var52[0][0] + " " + var52[0][1] + " " + var4 / 2 + " " + var5 / 4 + " " + var52[1][0] + " " + var52[1][1] + " " + var4 / 4 + " " + 3 * var5 / 4 + " " + var52[2][0] + " " + var52[2][1] + " " + 3 * var4 / 4 + " " + 3 * var5 / 4 + " -hideOutput");
                  break;
               default:
                  IJ.error("Unexpected transformation");
                  return null;
               }

               var13 = var55.getClass().getMethod("getTransformedImage", (Class[])null);
               var31 = (ImagePlus)var13.invoke(var55);
               var13 = var56.getClass().getMethod("getTransformedImage", (Class[])null);
               var32 = (ImagePlus)var13.invoke(var56);
               var13 = var57.getClass().getMethod("getTransformedImage", (Class[])null);
               var33 = (ImagePlus)var13.invoke(var57);
               var31.getStack().deleteLastSlice();
               var32.getStack().deleteLastSlice();
               var33.getStack().deleteLastSlice();
               var31.getProcessor().setMinAndMax(0.0D, 255.0D);
               var32.getProcessor().setMinAndMax(0.0D, 255.0D);
               var33.getProcessor().setMinAndMax(0.0D, 255.0D);
               ImageConverter var69 = new ImageConverter(var31);
               var71 = new ImageConverter(var32);
               var42 = new ImageConverter(var33);
               var69.convertToGray8();
               var71.convertToGray8();
               var42.convertToGray8();
               ((ColorProcessor)var3.getProcessor()).setRGB((byte[])var31.getProcessor().getPixels(), (byte[])var32.getProcessor().getPixels(), (byte[])var33.getProcessor().getPixels());
               break;
            default:
               IJ.error("Unexpected image type");
               return null;
            }

            return var1;
         }
      } catch (NoSuchMethodException var48) {
         IJ.error("Unexpected NoSuchMethodException " + var48);
         return null;
      } catch (IllegalAccessException var49) {
         IJ.error("Unexpected IllegalAccessException " + var49);
         return null;
      } catch (InvocationTargetException var50) {
         IJ.error("Unexpected InvocationTargetException " + var50);
         return null;
      } catch (ClassNotFoundException var51) {
         IJ.error("Please download TurboReg_ from\nhttp://bigwww.epfl.ch/thevenaz/turboreg/");
         return null;
      }
   }
}
