package com.morocco.appfactory.data;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public final class Project {
    public final String id;
    public final String type;
    public String name;
    public long updatedAt;
    public JSONObject data;

    public Project(String type, String name) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
        this.data = new JSONObject();
    }

    private Project(JSONObject json) throws JSONException {
        id = json.getString("id");
        if (!id.matches("[a-zA-Z0-9-]{1,64}")) throw new JSONException("Invalid project ID");
        type = json.getString("type");
        if (!type.matches("village|game|app|code")) throw new JSONException("Unknown project type");
        name = json.getString("name");
        updatedAt = json.getLong("updatedAt");
        data = json.getJSONObject("data");
    }

    public static Project parse(String json) throws JSONException { return new Project(new JSONObject(json)); }

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("schemaVersion", 1).put("id", id).put("type", type)
                .put("name", name).put("updatedAt", updatedAt).put("data", data);
    }
}
