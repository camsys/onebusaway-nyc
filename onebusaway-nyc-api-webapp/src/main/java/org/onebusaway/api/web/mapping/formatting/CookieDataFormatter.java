package org.onebusaway.api.web.mapping.formatting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.onebusaway.api.model.where.CookieData;

import java.util.HashMap;
import java.util.Map;

public class CookieDataFormatter {

    private static final Map<String, String> replacements = new HashMap<>();
    static {
        replacements.put(" ", "!!!");
        replacements.put("(", "!!#");
        replacements.put(")", "!!$");
        replacements.put("=", "!!%");
        replacements.put(",", "!!^");
        replacements.put("\"", "!!&");
        replacements.put("/", "!!*");
        replacements.put("?", "!!+");
        replacements.put("@", "!!|");
        replacements.put(":", "!!~");
        replacements.put(";", "!!%");
        replacements.put("{", "!!<");
        replacements.put("}", "!!>");
    }
    public static String toString(Object value){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(value);
            return jsonString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CookieData toObj(String jsonString, Class<? extends CookieData> targetClass){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            CookieData user = objectMapper.readValue(removeForbiddenChars(jsonString), targetClass);
            return user;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String removeForbiddenChars(String jsonString){
        // Replace forbidden delimiters in the JSON string
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            jsonString = jsonString.replace(entry.
                    getValue(),entry.getKey());
        }
        return jsonString;
    }
    public static String applyForbiddenChars(String jsonString){
        // Replace forbidden delimiters in the JSON string
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            jsonString = jsonString.replace(entry.getKey(), entry.getValue());
        }
        return jsonString;
    }
}

