package shouju.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class Receipt implements Serializable {
    private String id;
    private Map<String, String> fields;

    public Receipt(Map<String, String> fields) {
        this.id = "R" + System.currentTimeMillis();
        this.fields = new LinkedHashMap<>(fields);
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
}