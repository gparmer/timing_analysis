package ftAnalysis;

/**
 * @author songjiguo
 * enum class --  the recovery mode
 */
public enum RecoveryMode {
	Gen,
	Normal,
	Lazy,
	Eager,
	Checkpoint01,  // for convinience of saving the result
	Checkpoint1,     // consisten with directory name
	Checkpoint99     // consisten with directory name
}
