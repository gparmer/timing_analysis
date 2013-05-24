package ftAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBElement.GlobalScope;

/**
 * @author songjiguo
 * 
 *         Fault Tolerance Analysis
 */

public class A01 {
	static boolean			Cfirst		= false;
	static int				taskSet		= 50;
	static int				task_nums	= 0;
	static double			utilMax		= 0;
	static double			utilStart	= 0;
	static double			utilStep	= 0;
	static double			objsMax		= 0;

	static boolean			file_empty;
	static boolean			DEBUG;
	static boolean			WRITE_TO_FILE;

	public static Globals	myGlobals	= new Globals();

	public static void main(String[] args) {

		System.out.println(" << Start FT_RTA/FT_RTC....>> ");

		// **********************************
		// Paramer defined for graphs (ms)
		// **********************************

		int[] P_task = { 100 }; // Have this number fixed
		double uReboot = 0.02;
		double ObjRec = 0.005;

		int ObjNum = 10;

		int[] TaskNum = { 20, 50, 100 };
		//int[] TaskNum = { 50 };
		int[] Util = { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
		//int[] UtilCP = { 45, 65, 85 };
		int[] UtilCP = { 85 };
		int util_step = 5;

		String[] Target = { "Util", "Obj", "FPeriod", "CPeriod" };
		int[] Graphs = { 1, 2, 3, 4 };
		String[] Recovery = { "Gen", "Normal", "Lazy", "Eager", "Checkpoint01",
				"Checkpoint1", "Checkpoint99" }; // consistent with directory
													// name, 01->0.1ms, 1->10ms,
													// 99->1ms
		double[] CPS = { 0.1, 10, 1 };
		double[] CPR = { 0.1, 10, 1 };

		int plot;
		
		
		myGlobals.experiNum = 1; // experiment number, 50 for "Gen"
		// ****************************
		plot = 3;
		myGlobals.measurement = Recovery[6]; // 2,3,4,5,6
		// ****************************
		DEBUG = false;
		WRITE_TO_FILE = false;

		double fault_freq = 2; // fault period 2*P = (200ms)
		double check_freq = 2; // for checkpoint only (200ms)

		if (myGlobals.experiNum == 1)
			WRITE_TO_FILE = false;
		if (myGlobals.experiNum > 1)
			WRITE_TO_FILE = true;

		// Global parameters
		// Util graph1
		if (plot == 1) {
			myGlobals.target = Target[0];
			myGlobals.graph = Graphs[0];
		}
		// OBJ graph2 (C3 only)
		if (plot == 2) {
			myGlobals.target = Target[1];
			myGlobals.graph = Graphs[1];
		}
		// FaultPeriod graph3
		if (plot == 3) {
			myGlobals.target = Target[2];
			myGlobals.graph = Graphs[2];
		}
		// PCP_Period graph4 (Checkpoint only)
		if (plot == 4) {
			myGlobals.target = Target[3];
			myGlobals.graph = Graphs[3];
		}

		myGlobals.chkpt_period = P_task[0]; // initilization to be same as avgP
		myGlobals.fPeriod = P_task[0]; // initilization

		// int thisObject = 100; // max objs
		myGlobals.ureboot = uReboot;
		myGlobals.objRec = ObjRec;

		// for checkPoint only
		if (myGlobals.measurement == "Checkpoint01") {
			myGlobals.chkpt_scost = CPS[0];
			myGlobals.chkpt_rcost = CPR[0];
		} else if (myGlobals.measurement == "Checkpoint1") {
			myGlobals.chkpt_scost = CPS[1];
			myGlobals.chkpt_rcost = CPR[1];
		} else if (myGlobals.measurement == "Checkpoint99") {
			myGlobals.chkpt_scost = CPS[2];
			myGlobals.chkpt_rcost = CPR[2];
		} else {
			myGlobals.chkpt_scost = 0;
			myGlobals.chkpt_rcost = 0;
		}

		myGlobals.find_chkp = 0; // for different checkpoint period

		if (myGlobals.measurement == "Eager" || myGlobals.measurement == "Lazy"
				|| myGlobals.measurement == "Checkpoint01"
				|| myGlobals.measurement == "Checkpoint1"
				|| myGlobals.measurement == "Checkpoint99") {
			DEBUG = true;
			switch (myGlobals.graph) {
			case 1: // done!
				// ****************
				// Sched-Util
				// ****************
				utilStart = Util[0];
				utilMax = Util[Util.length - 1];
				utilStep = util_step;
				myGlobals.obj_num = ObjNum;

				for (int i = 0; i < P_task.length; i++) {
					myGlobals.avgP = P_task[i];
					myGlobals.fPeriod = fault_freq * P_task[i];
					myGlobals.chkpt_period = check_freq * P_task[i];

					if (constrain() == -1)
						return;

					double ra = (double) P_task[i] / myGlobals.ureboot;
					myGlobals.ratio = (int) ra;

					for (int k = 0; k < TaskNum.length; k++) {
						myGlobals.task_num = TaskNum[k];
						for (int n = 0; n < myGlobals.experiNum; n++) {
							System.out.println("<<<< set:" + n + ">>>>" + "("
									+ myGlobals.task_num + " " + myGlobals.avgP
									+ ")");
							for (RecoveryMode rtaMode : RecoveryMode.values()) {
								if (rtaMode.name() == myGlobals.measurement) {
									process(rtaMode, n + 1);
								}
							}
						}
					}
				}
				break;
			case 2:
				// ****************
				// Sched-Obj
				// ****************
				int Obj_MAX_Size = 200;
				utilStart = 70;
				utilMax = 70;
				utilStep = 1;

				for (int oo = 295; oo < 500; oo = oo + 5) {
					if (oo == 0) {
						myGlobals.obj_num = 1;
					} else
						myGlobals.obj_num = oo;
					for (int i = 0; i < P_task.length; i++) {
						myGlobals.avgP = P_task[i];
						myGlobals.fPeriod = fault_freq * P_task[i];

						double ra = (double) P_task[i] / myGlobals.ureboot;
						myGlobals.ratio = (int) ra;

						for (int k = 0; k < TaskNum.length; k++) {
							myGlobals.task_num = TaskNum[k];
							for (int n = 0; n < myGlobals.experiNum; n++) {
								System.out.println("<<<< set:" + n + ">>>>"
										+ "(" + myGlobals.task_num + " "
										+ myGlobals.obj_num + ")");
								for (RecoveryMode rtaMode : RecoveryMode
										.values()) {
									if (rtaMode.name() == myGlobals.measurement) {
										process(rtaMode, n + 1);
									}
								}
							}
						}
					}
				}
				break;
			case 3: // done!!
				// ****************
				// Sched-P_fault
				// ****************
				utilStart = 70;
				utilMax = 70;
				utilStep = 1;
				myGlobals.obj_num = ObjNum;

				// no constrain with faultP since atomic
				double step = 2;
				for (double pf = 3; pf < 8; pf = pf + 0.1) {
					for (int i = 0; i < P_task.length; i++) {
						myGlobals.avgP = P_task[i];
						myGlobals.fPeriod = pf;
						myGlobals.chkpt_period = check_freq * P_task[i];

						if (constrain() == -1)
							return;

						double ra = (double) P_task[i] / myGlobals.ureboot;
						myGlobals.ratio = (int) ra;

						for (int k = 0; k < TaskNum.length; k++) {
							myGlobals.task_num = TaskNum[k];
							for (int n = 0; n < myGlobals.experiNum; n++) {
								for (RecoveryMode rtaMode : RecoveryMode
										.values()) {
									if (rtaMode.name() == myGlobals.measurement) {
										process(rtaMode, n + 1);
									}
								}
							}
						}
					}
				}
				break;
			case 4: // done!!
				// ****************
				// Sched-P_cp "No fault!!!"
				// ****************
				myGlobals.chkpt_rcost = 0;

				utilStart = 70;
				utilMax = 70;
				utilStep = 1;
				myGlobals.obj_num = ObjNum;

				// based on constrain here...
				double base = myGlobals.chkpt_scost;
				int a = 0;
//				for (double pcp = base * 1.1; pcp < base * 100; pcp = pcp + base*5
	//					/ 10) {
				for (double pcp = 7; pcp < 100; pcp = pcp + 1) {
					for (int i = 0; i < P_task.length; i++) {
						myGlobals.avgP = P_task[i];
						myGlobals.chkpt_period = pcp;
						myGlobals.fPeriod = 100000000;

						if (constrain() == -1)
							return;

						double ra = (double) P_task[i] / myGlobals.ureboot;
						myGlobals.ratio = (int) ra;

						for (int k = 0; k < 1; k++) {
							utilStart = UtilCP[k];
							utilMax = UtilCP[k];
							myGlobals.task_num = TaskNum[1]; // 50 tasks
							for (int n = 0; n < myGlobals.experiNum; n++) {
								for (RecoveryMode rtaMode : RecoveryMode
										.values()) {
									if (rtaMode.name() == myGlobals.measurement) {
										process(rtaMode, n + 1);
									}
								}
							}
						}

					}
				}

				break;
			default:
				break;
			}
			System.out.println(" << ALL RTA DONE! >> ");
		}

		if (myGlobals.measurement == "Gen") {
			myGlobals.objRec = 0;
			myGlobals.chkpt_scost = 0;
			myGlobals.chkpt_rcost = 0;
			utilStart = Util[0];
			utilMax = Util[Util.length - 1];
			utilStep = util_step;
			for (int i = 0; i < P_task.length; i++) {
				myGlobals.avgP = P_task[i];
				double ra = (double) P_task[i] / myGlobals.ureboot;
				myGlobals.ratio = (int) ra;
				for (int k = 0; k < TaskNum.length; k++) {
					myGlobals.task_num = TaskNum[k];
					for (int n = 1; n < myGlobals.experiNum; n++) {
						System.out.println("<<<< set:" + n + ">>>>" + "("
								+ myGlobals.task_num + " " + myGlobals.avgP
								+ ")");
						for (RecoveryMode rtaMode : RecoveryMode.values()) {
							if (rtaMode.name() == myGlobals.measurement) {
								process(rtaMode, n);
							}
						}
					}
				}
			}
			System.out.println(" << Generation Tasks DONE! >> ");
		}
	}

	public static void process(RecoveryMode rtaMode, int experiment) {
		file_empty = true;
		doCal(myGlobals.fPeriod, myGlobals.obj_num, utilStart, utilMax,
				utilStep, rtaMode, experiment);
	}

	public static void doCal(double faultPeriod, double objsNum, double start,
			double end, double step, RecoveryMode rtaMode, int experiment) {
		double var;

		double wk_sched;
		double wk_sched_rate;

		String file_name = new String();

		for (var = start; var <= end; var = var + step) {
			wk_sched = 0;
			wk_sched_rate = 0;

			int actual_taskSet = 0;
			for (int k = 1; k <= taskSet; k++) { // 50 sets
				WorkLoad rtWorkload = new WorkLoad();
				rtWorkload.sysPars.initParameters();

				rtWorkload.sysPars.setTask_nums(myGlobals.task_num);

				rtWorkload.sysPars.setTotal_util(var);
				rtWorkload.sysFault.initFault(faultPeriod);
				rtWorkload.sysFault.setObjNumbers(objsNum);

				rtWorkload.sysFault.setChkpt_scost(myGlobals.chkpt_scost);
				rtWorkload.sysFault.setChkpt_rcost(myGlobals.chkpt_rcost);
				// already initalized.... in fault paras

				rtWorkload.sysFault.setuReboot(myGlobals.ureboot);
				rtWorkload.sysFault.setwRecovery(myGlobals.objRec);
				rtWorkload.sysFault.setChkpt_period(myGlobals.chkpt_period);

				file_name = "util";
				rtWorkload.sysPars.setForRTA(true);
				rtWorkload.sysPars.setForRTC(false);

				Object_Type objType = Object_Type.SCHED;
				rtWorkload.sysFault.setObjType(objType);

				if (rtaMode.name() == "Gen") {
					// *************************************
					// Generate Tasks
					// *************************************
					rtWorkload.sysPars.setAvg_period(myGlobals.avgP);
					rtWorkload.sysPars.setC_first(Cfirst);
					rtWorkload.initializeAllTasks();
					rtWorkload.sortTasks();
				} else {
					// *************************************
					// Read from Generated Tasks
					// *************************************
					String path = new String();
					String qq = "../../tasks/tasks_pool_" + myGlobals.ratio
							+ "/" + myGlobals.task_num + "/" + experiment + "/"
							+ (int) Math.round(var) + "/";
					try {
						path = new File(qq).getCanonicalPath();
					} catch (IOException e) {
						e.printStackTrace();
					}
					String tmp_name = path + "/" + k + "_" + file_name + "_"
							+ var + ".data";
					File f = new File(tmp_name);
					if (!f.exists()) {
						continue;
					}
					actual_taskSet++;
					rtWorkload.setAllTasksFromFiles(tmp_name);
				}

				rtWorkload.sysRTA.setRtaMode(rtaMode);
				// save each task set into a separate category;
				rtWorkload.sysPars.setTaskset_indx(k);
				String my_path = new String();
				my_path = file_name;
				rtWorkload.sysRTA.doRTA(rtWorkload);
				if (rtWorkload.sysRTA.isSchedulable()) {
					wk_sched++;
					if (rtaMode.name() == "Gen") {
						rtWorkload.saveTasks(my_path, file_name, var,
								myGlobals.ratio, experiment);
					}
				}
			}
			// *************************************
			// Convert to %
			// *************************************

			if (rtaMode.name() == "Gen") {
				wk_sched_rate = (double) wk_sched / (double) taskSet;
			} else {
				wk_sched_rate = (double) wk_sched / (double) actual_taskSet;
			}
			wk_sched_rate = (double) Math.round(wk_sched_rate * 100);

			// *******************
			// DEBUG
			// *******************
			if (DEBUG == true) {
				if (myGlobals.target == "Util") {
					System.out.println(myGlobals.task_num + " " + var + " "
							+ (myGlobals.fPeriod) + "(" + myGlobals.ratio
							+ ")-->" + wk_sched);
				}

				if (myGlobals.target == "FPeriod") {
					System.out.println(myGlobals.task_num + " " + var + " "
							+ (int) (myGlobals.fPeriod * 10) + "("
							+ myGlobals.ratio + ")-->" + wk_sched);
				}

				if (myGlobals.target == "CPeriod") {
					System.out.println(myGlobals.task_num + " " + var + " "
							+ (int) (myGlobals.chkpt_period * 1000) + "("
							+ myGlobals.ratio + ")-->" + wk_sched);
				}

			}

			// *******************
			// Write to Files
			// *******************

			if (WRITE_TO_FILE == true) {
				try {
					String path = new String();
					String qq = new String();
					if (myGlobals.target == "Obj") {
						// continue;
						qq = "../../data/OBJ/" + rtaMode.name()
								+ myGlobals.target + "/util"
								+ (int) Math.round(utilStart) + "/"
								+ myGlobals.task_num + "/";
					}
					if (myGlobals.target == "Util") {
						// continue;
						qq = "../../data/UTIL/" + rtaMode.name()
								+ myGlobals.target + "/" + myGlobals.ratio
								+ "/" + myGlobals.task_num + "/";
					}
					if (myGlobals.target == "FPeriod") {
						// continue;
						qq = "../../data/FRATE/" + rtaMode.name()
								+ myGlobals.target + "/" + myGlobals.ratio
								+ "/" + myGlobals.task_num + "/";
					}
					// if (myGlobals.target == "CPeriod") {
					// // continue;
					// qq = "../../data/CKP/" + rtaMode.name() +
					// myGlobals.target
					// + "/" + myGlobals.ratio + "/" + myGlobals.task_num
					// + "/";
					// }

					if (myGlobals.target == "CPeriod") {
						// continue;
						qq = "../../data/CKP/" + rtaMode.name()
								+ myGlobals.target + "/" + myGlobals.ratio
								+ "/" + (int) utilStart + "/";
					}

					if (rtaMode.name() != "Gen") {
						path = new File(qq).getCanonicalPath();
						file_name = file_name
								+ Integer.toString(experiment)
								+ "_"
								+ Integer.toString((int) Math
										.round(myGlobals.fPeriod));

						if (myGlobals.target == "Obj") {
							if (myGlobals.obj_num == 1) // just for saving files
								myGlobals.obj_num = 0;
							file_name = file_name + "_" + myGlobals.obj_num;
						}

						if (myGlobals.target == "FPeriod") {
							file_name = file_name + "_"
									+ (int) (myGlobals.fPeriod * 10);
						}

						if (myGlobals.target == "CPeriod") {
							file_name = file_name + "_"
									+ (int) (myGlobals.chkpt_period * 1000);
						}

						if (file_empty == true) {
							FileWriter fstream = new FileWriter(path + "/"
									+ rtaMode.name() + file_name + ".data");
							BufferedWriter out = new BufferedWriter(fstream);
							out.write("");
							out.flush();
							out.close();
							file_empty = false;
						}

						FileWriter fstream = new FileWriter(path + "/"
								+ rtaMode.name() + file_name + ".data", true);

						BufferedWriter out = new BufferedWriter(fstream);

						out.write(var + " " + wk_sched_rate);
						out.newLine();
						out.flush();
						out.close();
					}
				} catch (Exception e) {// Catch exception if any
					System.err.println("Error: " + e.getMessage());
				}
			}
			if (rtaMode.name() == "Gen") {
				System.out.println("Gen mode: generate tasks");
			}
		}
	}

	public static int constrain() {
		// This is not constrain, the duration of cp is important
		// if (myGlobals.chkpt_period >= myGlobals.fPeriod) {
		// System.out.println("NO 3 !!!!!");
		// return;
		// }
		if (myGlobals.measurement == "Checkpoint01"
				|| myGlobals.measurement == "Checkpoint1"
				|| myGlobals.measurement == "Checkpoint99") {

			if (myGlobals.chkpt_scost >= myGlobals.chkpt_period
					|| myGlobals.chkpt_rcost >= myGlobals.chkpt_period) {
				System.out.println("NO 1 !!!!!");
				System.out
						.println("myGlobals.chkpt_cost  myGlobals.chkpt_period"
								+ myGlobals.chkpt_scost + " "
								+ myGlobals.chkpt_period);
				return -1;
			}
			// Atomic....
			// if (myGlobals.chkpt_scost >= myGlobals.fPeriod
			// || myGlobals.chkpt_rcost >= myGlobals.fPeriod) {
			// System.out.println("NO 2 !!!!!");
			// System.out.println("myGlobals.chkpt_cost  myGlobals.fPeriod"
			// + myGlobals.chkpt_scost + " " + myGlobals.fPeriod);
			// return -1;
			// }
		}
		return 0;
	}
}
