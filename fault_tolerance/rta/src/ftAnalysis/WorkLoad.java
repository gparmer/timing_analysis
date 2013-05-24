package ftAnalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Scanner;
//import java.io.StreamCorruptedException;
//import java.text.DecimalFormat;
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author songjiguo
 * 
 *         WorkLoad Class
 * 
 *         -- Task set
 * 
 *         -- HRT or BEST
 * 
 *         -- Mode
 * 
 *         -- Distribution
 */
public class WorkLoad {

	private static final int	scale		= 1;

	// set up parameters themself
	public Pars_Workload		sysPars		= new Pars_Workload();
	// fault model parameters
	public Pars_Fault			sysFault	= new Pars_Fault();
	// RTA
	public RTA					sysRTA		= new RTA();

	// all utilization
	public Vector<Double>		utilDistr	= new Vector<Double>();
	// all period/deadline
	public Vector<Double>		periodDistr	= new Vector<Double>();
	// all execution time
	public Vector<Double>		execDistr	= new Vector<Double>();
	// all tasks
	public Vector<Task>			tasks		= new Vector<Task>();

	public void initializeAllTasks() {

		while (true) {

			double max_c = 0;

			if (this.sysPars.isC_first()) {
				// C setting first
				// double total_exe = 0;
				for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
					double rate = new Double(1)
							/ new Double(this.sysPars.getAvg_exe());

					double c_tmp = Distribution.exp(rate);
					c_tmp = Distribution.uniform(0.001, 2);
					// p_tmp = (double) Math.round(p_tmp * digits) / digits;
					while (c_tmp < this.sysPars.getMin_exe()) {
						c_tmp = Distribution.exp(rate);
					}
					// DecimalFormat twoDForm = new DecimalFormat("#.##");
					// p_tmp = Double.valueOf(twoDForm.format(p_tmp));
					c_tmp = c_tmp * scale;
					// System.out.println("C: " + c_tmp);

					this.execDistr.add(c_tmp);
					// total_exe = total_exe + c_tmp;
					// P is required to be larger than 1 in rtc toolbox min_U =
					// C/P = C =
					// min_C
					if (max_c < c_tmp)
						max_c = c_tmp;
				}
			} else {
				// P setting first
				// double total_exe = 0;
				for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
					double rate = new Double(1)
							/ new Double(this.sysPars.getAvg_period());
					double c_tmp = 0;
					// p_tmp = (double) Math.round(p_tmp * digits) / digits;
					while (c_tmp < this.sysPars.getMin_period()) {
						c_tmp = Distribution.exp(rate);
					}
					// DecimalFormat twoDForm = new DecimalFormat("#.##");
					// p_tmp = Double.valueOf(twoDForm.format(p_tmp));
					c_tmp = c_tmp * scale;
					// System.out.println("P: " + c_tmp);

					this.periodDistr.add(c_tmp);
					// total_exe = total_exe + c_tmp;
				}
			}

			// double avg_exe_per_task =
			// total_exe/(double)this.sysPars.getTask_nums();
			// System.out.println("avg_exe: " + avg_exe_per_task);

			// U setting
			find_util_top: while (true) {
				double sumNum = 0;
				double util_sumNUM = this.sysPars.getTotal_util();
				this.utilDistr.clear();
				int i = 0;
				find_util: for (i = 0; i < this.sysPars.getTask_nums(); i++) {
					double avg_util = this.sysPars.getTotal_util()
							/ this.sysPars.getTask_nums();
					double rate = 1 / avg_util;
					double u_tmp = 0;

					while (u_tmp <= avg_util / 10) {
						u_tmp = Distribution.exp(rate);
						// u_tmp = (double) Math.round(u_tmp * digits) / digits;
					}
					// add into vector
					// DecimalFormat twoDForm = new DecimalFormat("#.##");
					// u_tmp = Double.valueOf(twoDForm.format(u_tmp));
					this.utilDistr.add(u_tmp);

					sumNum = sumNum + u_tmp;
					// sumNum = (double) Math.round(sumNum * digits) / digits;
					// System.out.println("single util " + u_tmp + "(" + i + ")"
					// + sumNum);
					double left = util_sumNUM;
					util_sumNUM = util_sumNUM - u_tmp;
					if (util_sumNUM <= 0) {
						if (i < this.sysPars.getTask_nums() - 1) {
							break find_util;
						}
						sumNum = sumNum - u_tmp;
						this.utilDistr.remove(u_tmp);
						u_tmp = left;
						// u_tmp = (double) Math.round(u_tmp * digits) / digits;
						// DecimalFormat two2DForm = new DecimalFormat("#.##");
						// u_tmp = Double.valueOf(two2DForm.format(u_tmp));
						this.utilDistr.add(u_tmp);
						sumNum = sumNum + u_tmp;
						// sumNum = (double) Math.round(sumNum * digits) /
						// digits;
						// System.out.println("single util " + u_tmp + "(" + i +
						// ")"
						// + sumNum);
						break find_util_top;
					}
				}
			}

			// // // // // Print all utils
			// for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
			// double tmp;
			// tmp = this.utilDistr.elementAt(i);
			// System.out.println("Util :" + tmp);
			// }
			int redo = 0;
			if (this.sysPars.isC_first()) {
				// P setting from above C
				for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
					double pNum, uNum, cNum;
					cNum = this.execDistr.elementAt(i);
					uNum = this.utilDistr.elementAt(i);
					pNum = cNum / uNum * 100;

					if (pNum < 1) {
						redo = 1;
						// need clean the vector
						this.execDistr.setSize(0);
						this.periodDistr.setSize(0);
						this.utilDistr.setSize(0);
						break;
					}

					// DecimalFormat twoDForm = new DecimalFormat("#.##");
					// cNum = Double.valueOf(twoDForm.format(cNum));
					// cNum = (double) Math.round(cNum * digits) / digits;
					this.periodDistr.add(pNum);
					// System.out.println("P :" + pNum);
				}
				if (redo == 1)
					continue;
			} else {
				// C setting from abive P
				for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
					double pNum, uNum, cNum;
					pNum = this.periodDistr.elementAt(i);
					uNum = this.utilDistr.elementAt(i);
					cNum = pNum * uNum / 100;
					// if (pNum < 1) {
					// pNum = pNum * 10;
					// this.execDistr.setElementAt(cNum * 10, i);
					// }

					// DecimalFormat twoDForm = new DecimalFormat("#.##");
					// cNum = Double.valueOf(twoDForm.format(cNum));
					// cNum = (double) Math.round(cNum * digits) / digits;
					this.execDistr.add(cNum);
					// System.out.println("C :" + cNum);
				}
			}

			break;
		}
		// // Debug:
		// for (Double number : periodDistr) {
		// System.out.println(periodDistr.indexOf(number) + ": P "
		// + number);
		// }
		//
		// double test_sum = 0;
		// for (Double number : utilDistr) {
		// System.out.println(utilDistr.indexOf(number) + ": Util "
		// + number);
		// test_sum = test_sum + number;
		// }
		//
		// for (Double number : execDistr) {
		// System.out.println(execDistr.indexOf(number) + ": C "
		// + number);
		// }
		//
		// test_sum = (double) Math.round(test_sum * digits) / digits;
		//
		// System.out.println("Util Sum " + test_sum);
		// System.out.println("...done with C, T and U");

		// // adjust the utilization, 'steal' from the max
		// double sum = 0;
		// double uMax = 0;
		// int max_id = 0;
		// System.out.println("\nUpdating Util Now \n");
		// for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
		// double pNum, cNum, uNum;
		//
		// pNum = this.periodDistr.elementAt(i);
		// cNum = this.execDistr.elementAt(i);
		// uNum = (double) cNum / (double) pNum;
		// DecimalFormat twoDForm = new DecimalFormat("#.###");
		// uNum = Double.valueOf(twoDForm.format(uNum));
		// if (uNum > uMax) {
		// uMax = uNum;
		// max_id = i;
		// }
		// this.utilDistr.setElementAt(uNum, i);
		// sum = sum + uNum;
		// System.out.println("New Util :" + uNum);
		// }
		// System.out.println("New Total Util :" + sum);
		// System.out.println("Max Util :" + uMax);
		//
		// // steal from uMax if sum is larger than 1, hope max is enough!!!
		// double p, c;
		// p = this.periodDistr.elementAt(max_id);
		// c = this.execDistr.elementAt(max_id);
		// while (sum > 1) {
		// p = p + 1;
		// // System.out.println("max p :" + p);
		// this.utilDistr.setElementAt((double) c / (double) p, max_id);
		// double tmp = 0;
		// for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
		// tmp = tmp + this.utilDistr.elementAt(i);
		// }
		// sum = tmp;
		// if (sum <= 1) break;
		// }
		// System.out.println("New Total Util :" + sum);

		// tasks set up, using all C, T got above
		for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
			Task tmpTask = new Task();
			tmpTask.setTask_type(this.sysPars.getSub_sys());
			tmpTask.setC(this.execDistr.elementAt(i));
			tmpTask.setT(this.periodDistr.elementAt(i));

			this.tasks.add(tmpTask);
		}

		// For now, set each task with the same worst case recovery cost
		// this seems from ramFS most....maybe
		for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
			Task tmpTask = this.tasks.elementAt(i);
			tmpTask.setwRecovery(this.sysFault.getwRecovery());
		}
		// System.out.println("\n");
	}

	// RMS
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void sortTasks() {
		// System.out.println("Sorting all tasks according to their period");
		Collections.sort(this.tasks, new Comparator() {
			public int compare(Object a, Object b) {
				return (new Double(((Task) a).getT())).compareTo(new Double(
						((Task) b).getT()));
			}
		});
	}

	public void printTasks() {
		// System.out.println("Printing all tasks information...");
		// System.out.println("The size of tasks are: " + tasks.size());
		System.out.println(" ");
		for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
			Task tmpTask = this.tasks.elementAt(i);
			System.out.print("P: " + String.format("%.2f  ", tmpTask.getT())
					+ "C: " + String.format("%.2f  ", tmpTask.getC()));
			System.out.println("");
		}
	}

	public void deleteSavedTasks(String my_path, String paraName, double var) {
		String path = new String();
		try {
			path = new File("../../../tasks/").getCanonicalPath();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String save_path = new String();
		if (my_path.equals(paraName))
			save_path = path + '/' + paraName + "/";
		else
			save_path = path + '/' + my_path + "/";

		for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
			try {
				String del_file_name = new String();
				del_file_name = save_path + this.sysPars.getTaskset_indx()
						+ '_' + this.sysRTA.getRtaMode() + '_' + paraName + '_'
						+ var + ".data";
				File del_file = new File(del_file_name);
				if (del_file.exists())
					del_file.delete();

			} catch (Exception e) {// Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}
		}
	}

	public void saveTasks(String my_path, String paraName, double var, int ratio, int experiNum) {
		
		String path = new String();
		try {
			path = new File("../../tasks/tasks_pool_"+ ratio).getCanonicalPath();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String save_path = new String();
		if (my_path.equals(paraName))
			// save_path = path + '/' + paraName + "/";
			save_path = path + '/';
		else
			save_path = path + '/' + my_path + "/";

		save_path = save_path + this.sysPars.getTask_nums() + "/" + experiNum  +"/" + 
				(int) this.sysPars.getTotal_util() + "/";

		for (int i = 0; i < this.sysPars.getTask_nums(); i++) {
			Task tmpTask = this.tasks.elementAt(i);
			try {

				FileWriter fstream = new FileWriter(save_path
						+ this.sysPars.getTaskset_indx() + '_' + paraName + '_'
						+ var + ".data", true);

				BufferedWriter out = new BufferedWriter(fstream);

				out.write(tmpTask.getT() + "   " + tmpTask.getC());
				// + "   "
				// + this.sysFault.getuReboot() + "   "
				// + this.sysFault.getwRecovery());
				out.newLine();
				out.flush();
				out.close();

				// System.out.println(" write task set" +
				// this.sysPars.getTaskset_indx());

			} catch (Exception e) {// Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}
		}

		// save settings parameters
		// try {
		// FileWriter fstream = new FileWriter(save_path + "settings" + var);
		//
		// BufferedWriter out = new BufferedWriter(fstream);
		//
		// // out.write("TotalUtil " + this.sysPars.getTotal_util());
		// // out.write("\n");
		// // out.write("TotaTasks " + this.sysPars.getTask_nums());
		// // out.write("\n");
		// // out.write("AvgExec " + this.sysPars.getAvg_exe());
		// // out.write("\n");
		// // out.write("WorkSetIdx " + this.sysPars.getTaskset_indx());
		// // out.write("\n");
		// // out.write("\n");
		// //
		// // out.write("ureboot " + this.sysFault.getuReboot());
		// // out.write("\n");
		// // out.write("perObjRec " + this.sysFault.getwRecovery());
		// // out.write("\n");
		// // out.write("faultRate " + this.sysFault.getfPeriod());
		// // out.write("\n");
		// // out.write("TotalObjNum " + this.sysFault.getObjNumbers());
		// // out.write("\n");
		// double tmp = this.sysPars.getTotal_util();
		// out.write(String.format("%.2f", tmp));
		// out.write("\n");
		//
		// tmp = this.sysPars.getTask_nums();
		// out.write(String.format("%.2f", tmp));
		// out.write("\n");
		//
		// tmp = (double) this.sysPars.getTotal_util()
		// / (double) this.sysPars.getTask_nums();
		// out.write(String.format("%.4f", tmp));
		// out.write("\n");
		//
		// tmp = this.sysPars.getTaskset_indx();
		// out.write(String.format("%.2f", tmp));
		// out.write("\n");
		//
		// tmp = this.sysFault.getuReboot();
		// out.write(String.format("%.6f", tmp));
		// out.write("\n");
		// tmp = this.sysFault.getwRecovery();
		// out.write(String.format("%.6f", tmp));
		// out.write("\n");
		// tmp = this.sysFault.getfPeriod();
		// out.write(String.format("%.2f", tmp));
		// out.write("\n");
		// tmp = this.sysFault.getObjNumbers();
		// out.write(String.format("%.2f", tmp));
		// out.write("\n");
		//
		// out.newLine();
		// out.flush();
		// out.close();
		// } catch (Exception e) {// Catch exception if any
		// System.err.println("Error: " + e.getMessage());
		// }
	}

	public void setAllTasksFromFiles(String fileName) {
//				System.out.println(fileName);
				try {
					Scanner s = new Scanner(new File(fileName));
					while (s.hasNext()){
						
						double c_tmp = 0;
						double p_tmp = 0;
						double u_tmp = 0;
						p_tmp = Double.valueOf(s.next());
						this.periodDistr.add(p_tmp);
						c_tmp = Double.valueOf(s.next());
						this.execDistr.add(c_tmp);
						u_tmp = c_tmp/p_tmp*100;
						
						Task tmpTask = new Task();
						tmpTask.setTask_type(this.sysPars.getSub_sys());
						tmpTask.setC(c_tmp);
						tmpTask.setT(p_tmp);
						tmpTask.setwRecovery(this.sysFault.getwRecovery());
						this.tasks.add(tmpTask);
						
						//System.out.println("c p : " + p_tmp + " "+ c_tmp +" "+ u_tmp);
					}
					s.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		return;
	}

}
