package fr.insee.stamina.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The <code>ExplanatoryNote</code> class represents an explanatory note in a classification or concept scheme.
 * 
 * This is a base class which needs to be extended for use with specific classifications.
 * 
 * @author Franck Cotton
 * @version 0.1, 12 December 2016
 */
public class ExplanatoryNote {

	/**
	 * The type of the note.
	 * 
	 * @see NoteType
	 */
	private NoteType noteType;

	/** The raw text of the note as a list of strings. */
	private List<String> sourceText;

	/** The language of the explanatory note. */
	private Locale language;

	/** The start of the note validity period. */
	private Date validFrom;

	/** The end of the note validity period. */
	private Date validUntil;

	/**
	 * Constructs a note with unknown type, null validity dates, and English for default language.
	 */
	public ExplanatoryNote() {
		this(NoteType.UNKNOWN);
	}

	/**
	 * Constructs a note of a given type, with null validity dates, and English for default language.
	 */
	public ExplanatoryNote(NoteType noteType) {
		setNoteType(noteType);
		setSourceText(new ArrayList<String>());
		setLanguage(Locale.ENGLISH);
	}

	/**
	 * Returns the explanatory note as plain text.
	 * 
	 * In this basic implementation, the plain text is the concatenation of the <code>sourceText</code> strings, with an optional separator.
	 * 
	 * @param separator A <code>String</code> separator that will be inserted between the source lines.
	 * @return A <code>String</code> containing the note text as specified above.
	 */
	public String getPlainText(String separator) {

		if (sourceText.size() == 0) return ""; // We know the List is not null

		String nonNullSeparator = (separator == null ? "" : separator);
		return String.join(nonNullSeparator, sourceText);
	}

	/**
	 * Returns the explanatory note as an XHTML block.
	 * 
	 * In this basic implementation, the formatted text is a XHTML div wrapping the concatenation of paragraph elements containing the source lines.
	 * 
	 * @return A <code>String</code> containing the note text formatted as specified above.
	 */
	public String getFormattedText() {

		StringBuilder xhtml = new StringBuilder("<div xmlns=\"http://www.w3.org/1999/xhtml\">");

		for (String line : sourceText) {
			xhtml.append("<p>").append(line).append("</p>");
		}
		xhtml.append("</div>");

		return xhtml.toString();
	}

	/**
	 * Sets the note type to a new value.
	 * 
	 * @param noteType New value for the note type.
	 */
	public void setNoteType(NoteType noteType) {
		this.noteType = noteType;
	}

	/**
	 * Gets the note type to a new value.
	 * 
	 * @return The note type.
	 */
	public NoteType getNoteType() {
		return noteType;
	}

	/**
	 * Gets the note source text.
	 * 
	 * @return The source text of the note.
	 */
	public List<String> getSourceText() {
		return sourceText;
	}

	/**
	 * Sets the note source text to a new value.
	 * 
	 * @param sourceText New value for the note source text.
	 */
	public void setSourceText(List<String> sourceText) {
		this.sourceText = sourceText;
	}

	/**
	 * Adds a string to the note source text.
	 * 
	 * @param sourceLine Line to add to the note source text.
	 */
	public void addSourceLine(String sourceLine) {
		this.sourceText.add(sourceLine);
	}

	/**
	 * Gets the note language.
	 * 
	 * @return The language as a <code>Locale</code> object.
	 */
	public Locale getLanguage() {
		return language;
	}

	/**
	 * Sets the note language to a new value.
	 * 
	 * @param language New value for the note language (<code>Locale</code> object).
	 */
	public void setLanguage(Locale language) {
		this.language = language;
	}

	/**
	 * Gets the start date of the validity period of the note.
	 * 
	 * @return The start date of the validity period of the note.
	 */
	public Date getValidFrom() {
		return validFrom;
	}

	/**
	 * Sets the start date of the validity period of the note to a new value.
	 * 
	 * @param validFrom New value for the start date of the validity period of the note.
	 */
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	/**
	 * Gets the end date of the validity period of the note.
	 * 
	 * @return The end date of the validity period of the note.
	 */
	public Date getValidUntil() {
		return validUntil;
	}

	/**
	 * Sets the end date of the validity period of the note to a new value.
	 * 
	 * @param validFrom New value for the end date of the validity period of the note.
	 */
	public void setValidUntil(Date validUntil) {
		this.validUntil = validUntil;
	}
}
