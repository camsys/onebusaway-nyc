package org.onebusaway.api.model.where;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class CookieData {


    class CookieValueMapper {

        public String toString(Object value){
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonString = objectMapper.writeValueAsString(value);
                return applyForbiddenChars(jsonString);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public ObaCookieValue toObj(String jsonString, Class<? extends ObaCookieValue> targetClass){
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                ObaCookieValue user = objectMapper.readValue(removeForbiddenChars(jsonString), targetClass);
                return user;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        Map<String, String> replacements;
        public CookieValueMapper(){
            // Define replacements for forbidden delimiters
            replacements = new HashMap<>();
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

        public String removeForbiddenChars(String jsonString){
            // Replace forbidden delimiters in the JSON string
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                jsonString = jsonString.replace(entry.
                        getValue(),entry.getKey());
            }
            return jsonString;
        }
        public String applyForbiddenChars(String jsonString){
            // Replace forbidden delimiters in the JSON string
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                jsonString = jsonString.replace(entry.getKey(), entry.getValue());
            }
            return jsonString;
        }
    }

    class ObaCookieValue{}
}
