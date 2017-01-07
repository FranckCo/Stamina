package fr.insee.stamina.utils;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.SKOS;

/**
 * Enumeration corresponding to the different types of explanatory notes.
 */
public enum NoteType {
	GENERAL("GENERAL") {
		@Override
		public String pathComponent() {return "generalNote";}

		@Override
		public Property owlProperty() {return SKOS.definition;}

		@Override
		public String getTag() {return "G";}
	},
	CENTRAL_CONTENT("CORE") {
		@Override
		public String pathComponent() {return "coreContentNote";}

		@Override
		public Property owlProperty()  {return XKOS.coreContentNote;}

		@Override
		public String getTag() {return "CC";}
	},
	LIMIT_CONTENT("ADDITIONAL") {
		@Override
		public String pathComponent() {return "additionalContentNote";}

		@Override
		public Property owlProperty()  {return XKOS.additionalContentNote;}

		@Override
		public String getTag() {return "AC";}
	},
	EXCLUSIONS("EXCLUDED") {
		@Override
		public String pathComponent() {return "exclusionNote";}

		@Override
		public Property owlProperty()  {return XKOS.exclusionNote;}

		@Override
		public String getTag() {return "XC";}
	},
	REMARK("REMARK") {
		@Override
		public String pathComponent() {return "remark";}

		@Override
		public Property owlProperty()  {return SKOS.scopeNote;}

		@Override
		public String getTag() {return "R";}
	},
	UNKNOWN("UNKNOWN") {
		@Override
		public String pathComponent() {return null;}

		@Override
		public Property owlProperty()  {return null;}

		@Override
		public String getTag() {return null;}
	};

	private String text;

	NoteType(String text) {
		this.text = text;
	}

	public String toString() {
		return this.text;
	}

	public static NoteType fromString(String text) {
	    if (text != null) {
	      for (NoteType noteType : NoteType.values()) {
	        if (text.equals(noteType.text)) {
	          return noteType;
	        }
	      }
	    }
	    return null;
	}

	public static NoteType fromTag(String tag) {
	    if (tag != null) {
	      for (NoteType noteType : NoteType.values()) {
	        if (tag.equals(noteType.getTag())) {
	          return noteType;
	        }
	      }
	    }
	    return null;
	}

	public abstract String pathComponent();

	public abstract String getTag();

	public abstract Property owlProperty();

}
