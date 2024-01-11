package org.onebusaway.api.web.mapping.formatting;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldErrorMessage {

    /**
     * Defines a message the converter should supply on any conversion errors
     * during initialization of the annontated method argument, class field,
     * or bean property. The name is specified in decoded form, any percent
     * encoded literals within the value will not be decoded and will instead be
     * treated as literal text. E.g. if the parameter name is "a b" then the
     * value of the annotation is "a b", <i>not</i> "a+b" or "a%20b".
     */
    String value();
}
