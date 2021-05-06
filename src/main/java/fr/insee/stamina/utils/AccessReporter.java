package fr.insee.stamina.utils;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * This class contains methods that report information about Access databases.
 */
public class AccessReporter {

    final static String BASE_INPUT_PATH = "src/main/resources/data/in/";

    public static void main(String... args) throws IOException {

        BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/data/access-report.txt"));
        writer.write(folderReport(BASE_INPUT_PATH).toString());
        writer.close();
    }

    public static StringBuilder folderReport(String folderPath) throws IOException {

        StringBuilder builder = new StringBuilder();
        builder.append("Detailed information on Access databases in folder ").append(folderPath);
        for (File accessFile : listAccessFiles(new File(folderPath))) {
            builder.append("\n\nAccess file: ").append(accessFile.toString());
            builder.append(accessFileInfo(accessFile));
        }
        return builder;
    }

    private static StringBuilder accessFileInfo(File accessFile) throws IOException {

        StringBuilder builder = new StringBuilder();
        Database database = DatabaseBuilder.open(accessFile);
        builder.append("\nNumber of tables: ").append(database.getTableNames().size());
        for (String tableName : database.getTableNames()) {
            Table table = database.getTable(tableName);
            builder.append(accessTableInfo(table));
        }
        return builder;
    }

    private static StringBuilder accessTableInfo(Table table) {

        StringBuilder builder = new StringBuilder();
        builder.append("\nColumns in table ").append(table.getName());
        for (Column column : table.getColumns()) {
            builder.append("\n").append(column.toString());
        }
        return builder;
    }

    public static List<File> listAccessFiles(File folder) {
        return (List<File>) FileUtils.listFiles(folder, new String[] {"mdb"}, true);
    }
}
