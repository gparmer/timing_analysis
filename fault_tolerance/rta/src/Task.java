package ftAnalysis;

/**
 * @author songjiguo
 * 
 *         Task Parameters Class
 */

public class Task {
	private int		task_type;
	private double	C;			/* execution time */
	private double	T;			/* period */
	private double	D;			/* deadline, assume that D == T */
	private int		PRIO;		/* priority of a task, smaller T, higer P */

	/* worst recovery cost for this task */
	private double	wRecovery;

	// settling time of the task
	private double	ts;
	// # of missed deadlines of the task within the system ts
	private int		dlmiss;

	public Task() {
		setTask_type(0);
		setC(0);
		setT(0);
		setD(0);
		setPRIO(0);
		setwRecovery(0);
	}

	public int getTask_type() {
		return task_type;
	}

	public void setTask_type(int task_type) {
		this.task_type = task_type;
	}

	public double getC() {
		return C;
	}

	public void setC(double c) {
		C = c;
	}

	public double getD() {
		return D;
	}

	public void setD(double d) {
		D = d;
	}

	public double getT() {
		return T;
	}

	public void setT(double t) {
		T = t;
	}

	public int getPRIO() {
		return PRIO;
	}

	public void setPRIO(int pRIO) {
		PRIO = pRIO;
	}

	public double getwRecovery() {
		return wRecovery;
	}

	public void setwRecovery(double wRecovery) {
		this.wRecovery = wRecovery;
	}

	public double getTs() {
		return ts;
	}

	public void setTs(double ts) {
		this.ts = ts;
	}

	public int getDlmiss() {
		return dlmiss;
	}

	public void setDlmiss(int dlmiss) {
		this.dlmiss = dlmiss;
	}
}
