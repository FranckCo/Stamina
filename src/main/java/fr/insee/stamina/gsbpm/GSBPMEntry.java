package fr.insee.stamina.gsbpm;

public class GSBPMEntry {

	private String code;

	private String label;

	private String description;

	private boolean phase = false;

	/**
	 * Constructor with code and label.
	 * 
	 * @param code The code of the GSBPM phase or sub-process.
	 * @param label The label of the GSBPM phase or sub-process.
	 */
	public GSBPMEntry(String code, String label) {
		super();
		this.code = code;
		this.label = label;
		if (code.length() == 1) phase = true;
	}

	/**
	 * Constructor with code, label and description.
	 * @param code The code of the GSBPM phase or sub-process.
	 * @param label The label of the GSBPM phase or sub-process.
	 * @param description The description of the GSBPM phase or sub-process.
	 */
	public GSBPMEntry(String code, String label, String description) {
		this(code, label);
		this.description = description;
	}

	@Override
	public String toString() {
		return this.code + " - " + this.label + "\n" + this.description;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPhase() {
		return phase;
	}

}
