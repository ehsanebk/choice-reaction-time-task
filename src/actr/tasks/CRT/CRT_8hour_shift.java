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

public class CRT_8hour_shift extends Task {
	private TaskLabel label;
	private double lastTime = 0;
	private String[] stimuli = {"TRUE", "FALSE"};
	private String stimulus;
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;
	private String response = null;
	private double responseTime = 0;
	// the following two variables are for handling sleep attacks
	private int sleepAttackIndex = 0;
	
	Random random = new Random();

	private double[] timesOfCRT = {
			//time points
			//---1-----  -----2----- -----3-----
			14.0 + 24  , 20.0 + 24  , 22.0 +24  , // day1
			14.0 + 24*2, 20.0 + 24*2, 22.0 +24*2, // day2
			7.0  + 24*3, 13.0 + 24*3, 15.0 +24*3, // day3
			6.0  + 24*4, 12.0 + 24*4, 14.0 +24*4, // day4
			22.0 + 24*4, 4.0  + 24*5, 6.0  +24*5, // day5
	};
	int sessionNumber = 0; // starts from 0
	private Session currentSession;
	private Vector<Session> sessions = new Vector<Session>();

	@SuppressWarnings("unused")
	private PrintStream data;

	private class Session {
		double startTime = 0;
		int falseStarts = 0;
		int alertRosponses = 0;
		int alertResponseSpread[] = new int[35]; // Alert responses (150-500ms,
													// 10ms
		// intervals )

		double totalSessionTime = 0;
		int lapses = 0;
		int sleepAttacks = 0;
		int stimulusIndex = 0;
		int responses = 0; // number of responses, this can be diff from the
							// stimulusIndex because of false resonces
		double responseTotalTime = 0;
		int reactionErrors = 0;
		
		Vector<Double> responseTimes = new Vector<Double>();
	}

	public CRT_8hour_shift() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		lastTime = 0;

		currentSession = new Session();
		stimulusVisibility = false;

		getModel().getFatigue().setFatigueHour(timesOfCRT[sessionNumber]);
		getModel().getFatigue().startFatigueSession();

		addUpdate(1.0);

		try {
			File dataFile = new File("./results/data.txt");
			if (!dataFile.exists())
				dataFile.createNewFile();
			data = new PrintStream(dataFile);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void update(double time) {
		currentSession.totalSessionTime = getModel().getTime() - currentSession.startTime;

		if (currentSession.stimulusIndex <= 150) {
			stimulus = stimuli[random.nextInt(2)];
			label.setText(stimulus);
			label.setVisible(true);
			processDisplay();
			stimulusVisibility = true;
			lastTime = getModel().getTime();
			// setting up the state to wait
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("stimulus"));

			// calling percentage reset after any new task presentation (audio
			// or visual)
			getModel().getFatigue().fatigueResetPercentages();

			// Handling the sleep attacks -- adding an event in 30 s to see if
			// the current stimulus is still on
			currentSession.stimulusIndex++;
			addEvent(new Event(getModel().getTime() + 30.0, "task", "update") {
				@Override
				public void action() {
					sleepAttackIndex++;
					if (sleepAttackIndex == currentSession.stimulusIndex && stimulusVisibility == true) {
						label.setVisible(false);
						processDisplay();
						stimulusVisibility = false;
						currentSession.sleepAttacks++;
						currentSession.responses++; // when sleep attack happens
													// we add to the number of
													// responses
						System.out
								.println("Sleep attack at time ==>" + (getModel().getTime() - currentSession.startTime)
										+ "model time :" + getModel().getTime());
						System.out.println(currentSession.stimulusIndex + " " + sleepAttackIndex);
						addUpdate(1.0);
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),
								Symbol.get("wait"));
					}
					repaint();

				}
			});
		}

		// Starting a new Session
		else {
			sessionNumber++;
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfCRT.length) {
				addEvent(new Event(getModel().getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						sessions.add(currentSession);
						currentSession = new Session();
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = getModel().getTime();
						getModel().getFatigue().setFatigueHour(timesOfCRT[sessionNumber]);
						// System.out.println(sessionNumber +" : "+
						// getModel().getFatigue().computeBioMathValueForHour());
						getModel().getFatigue().startFatigueSession();
						addUpdate(1.0);
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),
								Symbol.get("wait"));
					}
				});

			} else {
				sessions.add(currentSession);
				getModel().stop();
			}

		}
	}

	@Override
	public void typeKey(char c) {

		if (stimulusVisibility == true) {
			response = c + "";
			
			// handling an error reaction to stimulus
			if ( stimulus == "TRUE" && !response.contains("t"))
				currentSession.reactionErrors++;
			if ( stimulus == "FALSE" && !response.contains("f"))
				currentSession.reactionErrors++;
			
			responseTime = getModel().getTime() - lastTime;
			currentSession.responseTimes.add(responseTime);

			if (response != null) // && response.equals("spc"))
			{
				currentSession.responses++;
				currentSession.responseTotalTime += responseTime;
			}

			label.setVisible(false);
			processDisplay();
			
			interStimulusInterval = random.nextInt(4) + 2; // A random number with a range of 2-5
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;

			if (responseTime < .150) {
				currentSession.falseStarts++;
			} else if (responseTime > .150 && responseTime <= .500) {
				currentSession.alertResponseSpread[(int) ((responseTime - .150) * 100)]++; // making
																							// the
																							// array
																							// for
																							// response
																							// time
				currentSession.alertRosponses++;
			} else if (responseTime > .500 && responseTime < 30.0) {
				currentSession.lapses++;
			}
			// setting up the state to wait
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("wait"));

		} else {
			currentSession.responses++;
			currentSession.falseStarts++;
		}

	}

	public int analysisIterations ()
	{
		return 100;
	}
	
	@Override
	public Result analyze(Task[] tasks, boolean output) {
		try {
			int numberOfSessions = timesOfCRT.length;
			Values[] totallReactionTimesValues = new Values[numberOfSessions];
			Values[] totallReactionErrosValues = new Values[numberOfSessions];
			Values[] totallLapsesValues = new Values[numberOfSessions];
			Values[] totallFalseAlerts = new Values[numberOfSessions];
			Values[] totallSleepAtacks = new Values[numberOfSessions];
			Values[] totallAlertResponces = new Values[numberOfSessions];
			Values[][] totallAlertResponcesSpread = new Values[numberOfSessions][35];
			Values[] totallResponsesNumber = new Values[numberOfSessions];

			Values[] totallProportionLapsesValues = new Values[numberOfSessions];
			Values[] totallProportionFalseAlerts = new Values[numberOfSessions];
			Values[] totallProportionSleepAtacks = new Values[numberOfSessions];
			Values[] totallProportionAlertRresponces = new Values[numberOfSessions];
			Values[][] totallProportionAlertResponcesSpread = new Values[numberOfSessions][35];

			// allocating memory to the vectors
			for (int i = 0; i < numberOfSessions; i++) {
				totallReactionTimesValues[i] = new Values();
				totallReactionErrosValues[i] = new Values();
				totallLapsesValues[i] = new Values();
				totallFalseAlerts[i] = new Values();
				totallSleepAtacks[i] = new Values();
				totallAlertResponces[i] = new Values();
				totallResponsesNumber[i] = new Values();
				totallProportionLapsesValues[i] = new Values();
				totallProportionFalseAlerts[i] = new Values();
				totallProportionSleepAtacks[i] = new Values();
				totallProportionAlertRresponces[i] = new Values();
				for (int j = 0; j < 35; j++) {
					totallAlertResponcesSpread[i][j] = new Values();
					totallProportionAlertResponcesSpread[i][j] = new Values();
				}
			}

			for (Task taskCast : tasks) {
				CRT_8hour_shift task = (CRT_8hour_shift) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					totallReactionTimesValues[i].add(task.sessions.elementAt(i).responseTimes);
					totallReactionErrosValues[i].add(task.sessions.elementAt(i).reactionErrors);
					totallFalseAlerts[i].add(task.sessions.elementAt(i).falseStarts);
					totallLapsesValues[i].add(task.sessions.get(i).lapses);
					totallSleepAtacks[i].add(task.sessions.get(i).sleepAttacks);
					totallAlertResponces[i].add(task.sessions.get(i).alertRosponses);
					totallResponsesNumber[i].add(task.sessions.get(i).responses);
					for (int j = 0; j < 35; j++) {
						totallAlertResponcesSpread[i][j].add(task.sessions.get(i).alertResponseSpread[j]);
					}

					totallProportionFalseAlerts[i]
							.add((double) task.sessions.get(i).falseStarts / task.sessions.get(i).responses);
					totallProportionLapsesValues[i]
							.add((double) task.sessions.get(i).lapses / task.sessions.get(i).responses);
					totallProportionSleepAtacks[i]
							.add((double) task.sessions.get(i).sleepAttacks / task.sessions.get(i).responses);
					totallProportionAlertRresponces[i]
							.add((double) task.sessions.get(i).alertRosponses / task.sessions.get(i).responses);
					for (int j = 0; j < 35; j++) {
						totallProportionAlertResponcesSpread[i][j].add(
								(double) task.sessions.get(i).alertResponseSpread[j] / task.sessions.get(i).responses);
					}
				}
			}

			DecimalFormat df3 = new DecimalFormat("#.000");

			getModel().output("*******    Choice Reaction Time    **********");
			getModel().output("\tTimePoint" + "\tRT(msec)" + "\tSTD\t" + "\tNo. Erros" + "\tSTD\t" +"\tNo. falseStarts" );

			int s =0;
			while(s < numberOfSessions) {
				
				getModel().output("\t"+ (s%3+1)+ "       \t" + df3.format(totallReactionTimesValues[s].mean()) + 
						"    \t" + df3.format(totallReactionTimesValues[s].stddev()) + "\t" + 
						"\t" + df3.format(totallReactionErrosValues[s].mean()) +
						"     \t" + df3.format(totallReactionErrosValues[s].stddev()) + "\t" +
						"\t" + totallFalseAlerts[s].mean());
				s++;
				if ((s%3) == 0)
					getModel().output("");
			
			}

			
			getModel().output("\n*******************************************\n");

			File dataFile = new File("./results/BioMathValues8hour.txt");
			if (!dataFile.exists())
				dataFile.createNewFile();
			PrintStream data = new PrintStream(dataFile);

			data.println("hr\tBioMath\ttest\tsleep");
			for (int h = 0; h < timesOfCRT[timesOfCRT.length - 1]; h++) {
				data.print((h) + "\t" + df3.format(getModel().getFatigue().getBioMathModelValueforHour(h)));
				data.print("\t");
				if (contains(timesOfCRT, h * 1.0))
					data.print(12);
				data.print("\t");
				if (getModel().getFatigue().isSleep(h))
					data.print(12);
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
