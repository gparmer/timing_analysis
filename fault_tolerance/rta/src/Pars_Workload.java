package ftAnalysis;

/**
 * @author songjiguo
 * 
 *         Workload Parameters Class
 */
public class Pars_Workload {
	// default number
	static int		def_task_nums	= 100;		// # of tasks

	static int		digits			= 100;		// 3 digits accuracy
	static int		def_sub_sys		= 1;		// HRT
	static int		def_distr		= 1;		// exponential
	
	static double	def_avg_period	= 100*10^3;   // average P  100ms
	static double	def_min_period	= 1;		// min P -- 0 or not will matter
												// for RTC task set
	static double	def_avg_exe		= 0.2;		// average C
	static double	def_min_exe		= 0.01;		// min C
	
	static double	def_max_util	= 100;		// max U
	static double	def_total_util	= 80;		// total U

	static double	def_uRebUtil	= 0;		// by default, no urebot in
												// total utilization

	private boolean	forRTA			= true;
	private boolean	forRTC			= false;
	private boolean	C_first			= false;

	private int		task_nums;					// total task numbers
	private int		sub_sys;					// hard real time
	private int		distr;						// exponential
	private double	avg_period;				// average period
	private double	min_period;				// minimun period
	private double	avg_exe;					// average c
	private double	min_exe;					// minimun c
	private double	total_util;				// total utilization
	private double	avg_util;					// total utilization
	private double	max_util;					// max utilization
	private double	min_util;					// min utilization

	private double	uRebAvgUtil;				// ratio:  ureboot/avg util
												// cost

	private int		taskset_indx;				// belong to which task set

	public void initParameters() {
		setSub_sys(def_sub_sys);
		setDistr(def_distr);
		setTask_nums(def_task_nums);
		setAvg_period(def_avg_period);
		setMin_period(def_min_period);
		setMin_exe(def_min_exe);
		setAvg_exe(def_avg_exe);
		setMax_util(def_max_util);
		setTotal_util(def_total_util);
		
		setuRebAvgUtil(def_uRebUtil);
	}

	public int getSub_sys() {
		return sub_sys;
	}

	public void setSub_sys(int sub_sys) {
		this.sub_sys = sub_sys;
	}

	public int getDistr() {
		return distr;
	}

	public void setDistr(int distr) {
		this.distr = distr;
	}

	public int getTask_nums() {
		return task_nums;
	}

	public void setTask_nums(int task_nums) {
		this.task_nums = task_nums;
	}

	public double getAvg_period() {
		return avg_period;
	}

	public void setAvg_period(double avg_period) {
		this.avg_period = avg_period;
	}

	public double getMin_period() {
		return min_period;
	}

	public void setMin_period(double min_period) {
		this.min_period = min_period;
	}

	public double getTotal_util() {
		return total_util;
	}

	public void setTotal_util(double total_util) {
		this.total_util = total_util;
	}

	public double getMax_util() {
		return max_util;
	}

	public void setMax_util(double max_util) {
		this.max_util = max_util;
	}

	public double getMin_util() {
		return min_util;
	}

	public void setMin_util(double min_util) {
		this.min_util = min_util;
	}

	public int getTaskset_indx() {
		return taskset_indx;
	}

	public void setTaskset_indx(int taskset_indx) {
		this.taskset_indx = taskset_indx;
	}

	public double getMin_exe() {
		return min_exe;
	}

	public void setMin_exe(double min_exe) {
		this.min_exe = min_exe;
	}

	public double getAvg_exe() {
		return avg_exe;
	}

	public void setAvg_exe(double avg_exe) {
		this.avg_exe = avg_exe;
	}

	public boolean isC_first() {
		return C_first;
	}

	public void setC_first(boolean c_first) {
		C_first = c_first;
	}

	public double getAvg_util() {
		return avg_util;
	}

	public void setAvg_util(double avg_util) {
		this.avg_util = avg_util;
	}

	public boolean isForRTA() {
		return forRTA;
	}

	public void setForRTA(boolean forRTA) {
		this.forRTA = forRTA;
	}

	public boolean isForRTC() {
		return forRTC;
	}

	public void setForRTC(boolean forRTC) {
		this.forRTC = forRTC;
	}

	public double getuRebAvgUtil() {
		return uRebAvgUtil;
	}

	public void setuRebAvgUtil(double uRebAvgUtil) {
		this.uRebAvgUtil = uRebAvgUtil;
	}

}
