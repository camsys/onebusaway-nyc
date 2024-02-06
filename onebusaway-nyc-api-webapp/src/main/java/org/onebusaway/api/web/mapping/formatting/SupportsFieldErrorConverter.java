package org.onebusaway.api.web.mapping.formatting;

import org.onebusaway.api.web.actions.api.ValidationErrorBean;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ParamConverter;
import java.util.*;

public abstract class SupportsFieldErrorConverter <T> implements ParamConverter<T> {

    String _field;

    public SupportsFieldErrorConverter(String field){
        _field=field;
    }

    @Override
    public T fromString(String value) {
        try {
            return convertFromString(value);
        }
        catch (ClassCastException exception){
            // yes this is weird, it's to make things backwards compatable w/ Struts, sry!
            LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
            List<String> errorMessages = new ArrayList<>();
            errorMessages.add("invalid field value for field "+_field);
            fieldErrors.put(_field,errorMessages);
            ValidationErrorBean bean = new ValidationErrorBean(null, fieldErrors);
            Response response = Response.serverError().entity(bean).build();
            throw new WebApplicationException(response);
        }
    }

    public abstract T convertFromString(String value);

}
