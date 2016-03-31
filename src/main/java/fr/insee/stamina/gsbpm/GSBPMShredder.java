package fr.insee.stamina.gsbpm;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class GSBPMShredder {

	// File must be encoded in UTF-8 *without BOM*
	static String GSBPM_TXT = "src/main/resources/data/GSBPM 5.0 - phases.txt";
	static String SHRED_PATH = "src/main/resources/data/shreds/";

	private static Logger logger = LogManager.getLogger(GSBPMShredder.class);

	public static void main(String[] args) throws IOException {

		GSBPMReader reader = new GSBPMReader();
		List<GSBPMEntry> gsbpmEntries = reader.read(new File(GSBPM_TXT));

		logger.debug("GSBPM entries read, starting to write shreds");

		for (GSBPMEntry entry : gsbpmEntries) {

			String fileName = SHRED_PATH + (entry.isPhase() ? "phase-" : "sub-process-") + entry.getCode() + ".txt";

			PrintWriter shredFile = new PrintWriter(fileName);
			shredFile.write(entry.getLabel() + "\n" + entry.getDescription());
			shredFile.close();
		}

		logger.debug("Shreds created");
	}

}
