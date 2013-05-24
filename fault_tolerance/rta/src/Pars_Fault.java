package ftAnalysis;

/**
 * @author songjiguo
 * 
 *  SWIFI Fault Class: Define the fault model and parameters
 */

public class Pars_Fault {
	//default number
	static int		def_objNumers	= 0;		// # of objects to be recovered
	
	static double	def_fPeriod		= 0;
	static double	def_wRecovery	= 0;	// keep this ratio 1:4
	static double	def_uReboot		= 0;	// 

	private double		objNumbers;	/* Fault period/rate */
	private double		fPeriod;	/* Fault period/rate */
	private double		wRecovery;	/* worst recovery cost, depends on obj_type */
	private double		uReboot;	/* cost of micro-reboot, depends on obj_type */
	
	private double		chkpt_scost =  0;  /* checkpoint saving cost, 100us */
	private double		chkpt_rcost =  0;  /* checkpoint restoring cost, 100us */
	private double 		chkpt_period = 0;
	
	private double		TS;		/* Settling time */

	private Object_Type	objType;

	public double getTS() {
		return TS;
	}

	public void setTS(double TS) {
		this.TS = TS;
	}

	public Pars_Fault() {
		setwRecovery(0);
		setuReboot(0);
		setfPeriod(0);
		
		setTS(0);
	}

	public double getuReboot() {
		return uReboot;
	}

	public void setuReboot(double uReboot) {
		this.uReboot = uReboot;
	}

	public double getwRecovery() {
		return wRecovery;
	}

	public void setwRecovery(double wRecovery) {
		this.wRecovery = wRecovery;
	}

	public double getfPeriod() {
		return fPeriod;
	}

	public void setfPeriod(double fPeriod) {
		this.fPeriod = fPeriod;
	}

	public Object_Type getObjType() {
		return objType;
	}

	public void setObjType(Object_Type objType) {
		this.objType = objType;
	}

	public double getObjNumbers() {
		return objNumbers;
	}

	public void setObjNumbers(double objNumbers) {
		this.objNumbers = objNumbers;
	}
	
	
	public void initFault(double faultPeriod) {
		this.setObjNumbers(def_objNumers);
		this.setuReboot(def_uReboot);
		this.setwRecovery(def_wRecovery);
		this.setChkpt_scost(chkpt_scost);
		this.setChkpt_rcost(chkpt_rcost);
		this.setChkpt_period(chkpt_period);
		this.setfPeriod(faultPeriod);
		
		}

	public void printFaultParams() {
//		System.out.println("Printing fault model information...");
		System.out.println("Fault Object: " + this.objType.name());
		System.out.println("Objects number " + this.getObjNumbers());
		System.out.println("uReboot cost " + this.getuReboot());
		System.out.println("Worst Case Recovery Cost " + this.getwRecovery());
		System.out.println("Fault Period " + this.getfPeriod());
	}

	public double getChkpt_scost() {
		return chkpt_scost;
	}

	public void setChkpt_scost(double chkpt_scost) {
		this.chkpt_scost = chkpt_scost;
	}

	public double getChkpt_rcost() {
		return chkpt_rcost;
	}

	public void setChkpt_rcost(double chkpt_rcost) {
		this.chkpt_rcost = chkpt_rcost;
	}

	public double getChkpt_period() {
		return chkpt_period;
	}

	public void setChkpt_period(double chkpt_period) {
		this.chkpt_period = chkpt_period;
	}


}
