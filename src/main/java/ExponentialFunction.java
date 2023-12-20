import org.apache.commons.math3.analysis.ParametricUnivariateFunction;

class ExponentialFunction implements ParametricUnivariateFunction {
   private int mode;
   private double[] abc;

   public void setMode(int var1) {
      this.mode = var1;
      switch(this.mode) {
      case 1:
         this.abc = new double[]{0.85D, 0.5D, 0.5D};
         break;
      case 2:
         this.abc = new double[]{0.85D, 0.5D, 0.5D, 0.216D, 0.3D};
      }

   }

   public void setInitialGuess(double[] var1) {
      for(int var2 = 0; var2 < var1.length; ++var2) {
         this.abc[var2] = var1[var2];
      }

   }

   public double[] getInitialGuess() {
      switch(this.mode) {
      case 1:
         return this.abc;
      case 2:
         return new double[]{this.abc[0], 0.5D, 0.563D, 0.216D, 0.36D};
      default:
         return null;
      }
   }

   public double[] getFuncPoints(double[] var1, double[] var2) {
      double[] var3 = new double[var1.length];

      for(int var4 = 0; var4 < var3.length; ++var4) {
         var3[var4] = this.value(var1[var4], var2);
      }

      return var3;
   }

   public double getMobileFraction(double[] var1) {
      double var2 = 0.0D;
      switch(this.mode) {
      case 1:
         var2 = var1[1] / (1.0D - (var1[0] - var1[1]));
         break;
      case 2:
         var2 = (var1[1] + var1[3]) / (1.0D - (var1[0] - var1[1] - var1[3]));
      }

      return var2;
   }

   public double getThalf(double[] var1, double[] var2, double[] var3) {
      switch(this.mode) {
      case 1:
         return Math.log(2.0D) / var1[2];
      case 2:
         double var4 = (2.0D * var1[0] - var1[1] - var1[3]) / 2.0D;
         return this.findHalfT(var2, var3, var4);
      default:
         return -1.0D;
      }
   }

   public double findHalfT(double[] var1, double[] var2, double var3) {
      int var5;
      for(var5 = 0; var5 < var2.length && var2[var5] < var3; ++var5) {
      }

      if (var5 == var2.length) {
         --var5;
      }

      if (var5 <= 0) {
         return -1.0D;
      } else {
         double var6 = (var2[var5] - var3) / (var2[var5] - var2[var5 - 1]);
         double var8 = var1[var5 - 1] + (1.0D - var6) * (var1[var5] - var1[var5 - 1]);
         return var8;
      }
   }

   public double value(double var1, double... var3) {
      switch(this.mode) {
      case 1:
         return var3[0] - var3[1] * Math.exp(-var3[2] * var1);
      case 2:
         return var3[0] - var3[1] * Math.exp(-var3[2] * var1) - var3[3] * Math.exp(-var3[4] * var1);
      default:
         return -1.0D;
      }
   }

   public double[] gradient(double var1, double... var3) {
      double var4 = var3[0];
      double var6 = var3[1];
      double var8 = var3[2];
      switch(this.mode) {
      case 1:
         return new double[]{1.0D, -Math.exp(-var8 * var1), var6 * var1 * Math.exp(-var8 * var1)};
      case 2:
         double var10 = var3[3];
         double var12 = var3[4];
         return new double[]{1.0D, -Math.exp(-var8 * var1), var6 * var1 * Math.exp(-var8 * var1), -Math.exp(-var12 * var1), var10 * var1 * Math.exp(-var12 * var1)};
      default:
         return new double[]{-1.0D};
      }
   }
}
