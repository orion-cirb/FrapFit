package Frap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;

public class FunctionFitting extends AbstractCurveFitter {
   protected ExponentialFunction myfunc;

   public FunctionFitting(ExponentialFunction var1) {
      this.myfunc = var1;
   }

   protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> var1) {
      int var2 = var1.size();
      double[] var3 = new double[var2];
      double[] var4 = new double[var2];
      double[] var5 = this.myfunc.getInitialGuess();
      int var6 = 0;

      for(Iterator var7 = var1.iterator(); var7.hasNext(); ++var6) {
         WeightedObservedPoint var8 = (WeightedObservedPoint)var7.next();
         var3[var6] = var8.getY();
         var4[var6] = var8.getWeight();
      }

      TheoreticalValuesFunction var9 = new TheoreticalValuesFunction(this.myfunc, var1);
      return (new LeastSquaresBuilder()).maxEvaluations(Integer.MAX_VALUE).maxIterations(Integer.MAX_VALUE).start(var5).target(var3).weight(new DiagonalMatrix(var4)).model(var9.getModelFunction(), var9.getModelFunctionJacobian()).build();
   }

   public double[] dofit(double[] var1, double[] var2) {
      ArrayList var3 = new ArrayList();

      for(int var4 = 0; var4 < var1.length; ++var4) {
         WeightedObservedPoint var5 = new WeightedObservedPoint(1.0D, var1[var4], var2[var4]);
         var3.add(var5);
      }

      double[] var6 = this.fit(var3);
      this.getProblem(var3);
      return var6;
   }
}
