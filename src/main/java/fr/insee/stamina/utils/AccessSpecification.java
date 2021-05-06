package fr.insee.stamina.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AccessSpecification {

    File accessFile;
    String tableName;
    Map<String, String> columns;

    public File getAccessFile() {
        return accessFile;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    @Override
    public String toString() {
        return "AccessSpecification{" +
                "accessFile=" + accessFile +
                ", tableName='" + tableName + '\'' +
                ", columns=" + columns +
                '}';
    }

    public AccessSpecification(File accessFile, String tableName, Map<String, String> columns) {
        this.accessFile = accessFile;
        this.tableName = tableName;
        this.columns = columns;
    }

    public static void main(String... arg) {
        Map<String, String> cpc2Columns = new HashMap<String, String>() {{
            put("code", "Code");
            put("label", "Description");
            put("note", "ExplanatoryNote");
        }};
        AccessSpecification cpc2AccessInfo = new AccessSpecification(new File("CPCv2_english.mdb"), "CPC2-structure", cpc2Columns);
        System.out.println(cpc2AccessInfo);
    }
}
