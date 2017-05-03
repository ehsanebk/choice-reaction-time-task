package actr.tasks.CRT;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;

/**
 * Model of Choice Reaction Time and fatigue mechanism
 * 
 * Paper: Some effects of 8- vs. 10-hour work schedules on the test performance/alertness 
 *        of air traffic control specialists
 * 
 * David J. Schroeder, Roger R. Rosa, L. Alan Witt
 * 
 * @author Ehsan Khosroshahi
 */

public class BiomathValues extends Task {
	
	public BiomathValues() {
		super();
		
	}

	@Override
	public void start() {
			}

	
	public Result analyze(Task[] tasks, boolean output) {

		try{
		DecimalFormat df3 = new DecimalFormat("#.000");
			File dataFile = new File("./results/BioMathValues.txt");
			if (!dataFile.exists())
				dataFile.createNewFile();
			PrintStream data = new PrintStream(dataFile);

			data.println("hr\tBioMath\tsleep");
			for (double h = 0; h < 168; h+=0.5) {
				data.print((h) + "\t" + df3.format(getModel().getFatigue().getBioMathModelValueforHour(h)));
				data.print("\t");
				if (getModel().getFatigue().isSleep(h))
					data.print(14);
				data.print("\n");
			}

			data.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		Result result = new Result();
		return result;
	}
	private static boolean contains(double[] arr, double targetValue) {
		for(Double s: arr){
			if(s.equals(targetValue))
				return true;
		}
		return false;
	}

}
