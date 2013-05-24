package ftAnalysis;

/**
 * @author songjiguo
 * 
 *         RTA Class -- Response Time Analysis
 * 
 *         equation: \item Eager recovery \begin{equation} R_i^{n+1} =
 *         e_i+b_i+\sum\limits_{j< i} \lceil\frac{R_i^n}{p_j}\rceil
 *         e_j+\lceil\frac{R_i^n}{p_{re}}\rceil (e_{reboot}+\sum
 *         \limits_{k=1}^{N}e_{ft}^k) \end{equation}
 * 
 *         \item Lazy recovery \begin{equation} R_i^{n+1} =
 *         e_i+b_i+\sum\limits_{j< i} \lceil\frac{R_i^n}{p_j}\rceil
 *         e_j+\lceil\frac{R_i^n}{p_{re}}\rceil (e_{\mu r}+\sum_{j\leq
 *         i}\{r_{j}\}) \end{equation}
 * 
 * 
 *         Parameters:
 * 
 */
public class RTA {
	// if a system is schedulable after RTA
	private boolean				schedulable;

	/* Define all parametes in FT_RTA equation */
	private double				e;
	private double				b;
	private double				p;

	private double				p_re;
	private double				ureboot;
	private double				rec;

	public static final double	epsilon	= 0.0001;

	// RTA mode
	private RecoveryMode		rtaMode;

	public double getE() {
		return e;
	}

	public void setE(double e) {
		this.e = e;
	}

	public double getB() {
		return b;
	}

	public void setB(double b) {
		this.b = b;
	}

	public double getP() {
		return p;
	}

	public void setP(double p) {
		this.p = p;
	}

	public double getP_re() {
		return p_re;
	}

	public void setP_re(double p_re) {
		this.p_re = p_re;
	}

	public double getUreboot() {
		return ureboot;
	}

	public void setUreboot(double ureboot) {
		this.ureboot = ureboot;
	}

	public double getRec() {
		return rec;
	}

	public void setR(double rec) {
		this.rec = rec;
	}

	public RecoveryMode getRtaMode() {
		return rtaMode;
	}

	public void setRtaMode(RecoveryMode rtaMode) {
		this.rtaMode = rtaMode;
	}

	public void printRTAinfo() {
		System.out.print(this.getE() + "  ");
		System.out.print(this.getB() + "  ");
		System.out.print(this.getP() + "  ");

		System.out.print(this.getUreboot() + "  ");
		System.out.print(this.getRec() + "  ");
		System.out.print(this.getP_re());

		System.out.println(" ");
	}

	public void doRTA(WorkLoad wk) {
		for (int i = 0; i < wk.sysPars.getTask_nums(); i++) {
			Task myTask = wk.tasks.elementAt(i);

			setE(myTask.getC());
			setB(0);
			setP(myTask.getT());
			setUreboot(wk.sysFault.getuReboot());
			setP_re(wk.sysFault.getfPeriod());

			if (rta_fn(myTask, wk) == false) {
				this.setSchedulable(false);
				return;
			}
		}
		this.setSchedulable(true);
	}

	// RTA on the passed in task
	public boolean rta_fn(Task task, WorkLoad wk) {
		// wk.printTasks();
		// wk.printFaultParams();
		// wk.sysRTA.printRTAinfo();
		// wk.sysFault.printFaultParams();
		// at this point, workload should have Tasks, Fault and RTA setupa

		int alg2 = 0; // another equation

		double r_new, r_old;
		double e, b, p;
		double p_re, e_ur;
		double p_chk;
		double Term1, Term2, Term3;
		Term1 = 0;
		Term2 = 0;
		Term3 = 0;

		double objsNum;
		// task
		e = task.getC();
		b = 0; // assume 0 for now
		p = task.getT();

		// fault related terms
		p_re = wk.sysFault.getfPeriod();
		e_ur = wk.sysFault.getuReboot();
		objsNum = wk.sysFault.getObjNumbers();

		p_chk = wk.sysFault.getChkpt_period();
		// R_n1 and R_n
		r_new = r_old = e; // start value

		// start equation
		// Equation: r_new = c + b + Term1 + Term2

		/* <<<<< Term2 >>>>> */

		// double my_ceil = Math.ceil((double) r_old / (double) p_re);
		// double my_ceil2 = Math.ceil((double) r_old / (double) p_chk);
		if (this.rtaMode != RecoveryMode.Normal
				&& this.rtaMode != RecoveryMode.Gen) {
			if (this.rtaMode == RecoveryMode.Checkpoint01
					|| this.rtaMode == RecoveryMode.Checkpoint1
					|| this.rtaMode == RecoveryMode.Checkpoint99) { // CheckPoint
				Term2 = wk.sysFault.getChkpt_rcost();
				// Term2 = 0; // this is for no faults, only making the
				// checkpoint
				Term3 = wk.sysFault.getChkpt_scost();
				if (alg2 == 1)
					Term3 = 0;
				// Term3 = 0;
			} else { // Lazy or Eager
				Term3 = 0;
				for (int i = 0; i < wk.sysPars.getTask_nums(); i++) {

					Task tmp = wk.tasks.elementAt(i);
					if (this.rtaMode == RecoveryMode.Lazy && tmp.getT() > p)
						continue;
					Term2 = Term2 + task.getwRecovery() * objsNum;
				}
				Term2 = Term2 + e_ur;
			}
		}
		boolean ret = false;
		while (true) {
			/* <<<<< Term1 >>>>> */
			Term1 = 0;
			if (this.rtaMode == RecoveryMode.Checkpoint01
					|| this.rtaMode == RecoveryMode.Checkpoint1
					|| this.rtaMode == RecoveryMode.Checkpoint99) {
				for (int i = 0; i < wk.sysPars.getTask_nums(); i++) {
					Task tmp = wk.tasks.elementAt(i);
					if (tmp.getT() < p) {
						if (alg2 == 1) {
							Term1 = Term1
									+ (tmp.getC() + tmp.getC()
											* wk.sysFault.getChkpt_scost()
											/ p_chk)
									* Math.ceil((double) r_old / tmp.getT());
						} else {
							Term1 = Term1 + tmp.getC()
									* Math.ceil((double) r_old / tmp.getT());
							boolean isDebug = java.lang.management.ManagementFactory
									.getRuntimeMXBean().getInputArguments()
									.toString().indexOf("-agentlib:jdwp") > 0;
							if (isDebug)
								System.out.println("(double) T : " + (double)  tmp.getT());
						}
					}
				}
				if (alg2 == 1)
					Term1 = Term1 + e * wk.sysFault.getChkpt_scost() / p_chk;
			} else {
				for (int i = 0; i < wk.sysPars.getTask_nums(); i++) {
					Task tmp = wk.tasks.elementAt(i);
					boolean isDebug = java.lang.management.ManagementFactory
							.getRuntimeMXBean().getInputArguments()
							.toString().indexOf("-agentlib:jdwp") > 0;
					if (isDebug)
						System.out.println("(double) T : " + (double)  tmp.getT());
					if (tmp.getT() < p) {
						Term1 = Term1 + tmp.getC()
								* Math.ceil((double) r_old / tmp.getT());
					}
				}
			}

			if (this.rtaMode == RecoveryMode.Normal
					|| this.rtaMode == RecoveryMode.Gen) {
				// System.out.println(this.rtaMode.name());
				r_new = e + b + Term1;
			} else {
				// System.out.println(this.rtaMode.name());
				// System.out.println("p_re:  " + p_re);
				r_new = e + b + Term1 + Term2
						* Math.ceil((double) r_old / (double) p_re) + Term3
						* Math.ceil((double) r_old / (double) p_chk);

				boolean isDebug = java.lang.management.ManagementFactory
						.getRuntimeMXBean().getInputArguments().toString()
						.indexOf("-agentlib:jdwp") > 0;
				if (isDebug)
					System.out.println("(double) r_old : (double) p_re "
							+ (double) r_old + " " + (double) p_re);
			}
			double diff = Math.abs(r_new - r_old);
			if (diff <= epsilon) {
				// System.out.println("Schedulable? [Y]");
				// System.out.println("r_new  " + r_new + "  r_old  " + r_old);
				if (r_new < p || r_new == p) {// R must be in [0,D]??
					ret = true;
					break;
				} else {
					ret = false;
					break;
				}
			} else if (r_new > p) {
				// System.out.println("Schedulable? [N]");
				ret = false;
				break;
			}
			r_old = r_new;
		}

		return ret;
	}

	public boolean isSchedulable() {
		return schedulable;
	}

	public void setSchedulable(boolean schedulable) {
		this.schedulable = schedulable;
	}

}
