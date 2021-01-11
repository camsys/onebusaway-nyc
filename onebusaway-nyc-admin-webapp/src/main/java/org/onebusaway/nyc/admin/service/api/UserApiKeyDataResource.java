/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.admin.service.api;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.admin.model.UserApiKeyData;
import org.onebusaway.nyc.admin.service.UserApiKeyDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static javax.ws.rs.core.Response.ok;

@Path("/api-key")
@Component
public class UserApiKeyDataResource {
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private UserApiKeyDataService _service;
    private static Logger log = LoggerFactory.getLogger(UserApiKeyDataResource.class);

    @GET
    @Path("/data")
    @Produces("application/json")
    public Response getApiKeyData(@QueryParam("offset") Integer offset, @QueryParam("size") Integer size) {

        log.info("Getting user api key data");

        List<UserApiKeyData> apiKeyData = _service.getAllUserApiKeyDataPaged(offset, size);

        return constructResponse(apiKeyData);
    }

    @GET
    @Path("/export/csv")
    @Produces("text/csv")
    public Response exportToCSV() throws IOException {

        List<UserApiKeyData> apiKeyData = _service.getAllUserApiKeyData();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(output);

        ICsvBeanWriter csvWriter = new CsvBeanWriter(writer, CsvPreference.STANDARD_PREFERENCE);
        String[] csvHeader = {"Created", "Name", "Email", "Project Name", "Project URL", "Platform", "API Key", "Comment"};
        String[] nameMapping = {"created", "name", "email", "projectName", "projectUrl", "platform", "apiKey", "comment"};
        CellProcessor[] processors = { new Optional(new FmtDate("yyyy/MM/dd HH:mm:ss")),null,null,null,null,null,null,null };

        csvWriter.writeHeader(csvHeader);

        for (UserApiKeyData data : apiKeyData) {
            csvWriter.write(data, nameMapping, processors);
        }

        csvWriter.close();

        Response.ResponseBuilder responseBuilder = Response.ok(output.toByteArray());
        responseBuilder.type("text/csv");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=" + getExportFileName();
        responseBuilder.header(headerKey, headerValue);
        return responseBuilder.build();
    }

    private String getExportFileName(){
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateTime = dateFormatter.format(new Date());
        return "api_keys_" + currentDateTime + ".csv";
    }

    private Response constructResponse(Object result) {
        final StringWriter sw = new StringWriter();
        final MappingJsonFactory jsonFactory = new MappingJsonFactory();
        Response response = null;
        try {
            final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
            mapper.setDateFormat(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
            mapper.writeValue(jsonGenerator, result);
            response = ok(sw.toString()).build();
        } catch (JsonGenerationException e) {
            log.error("Error generating response JSON");
            response = Response.serverError().build();
            e.printStackTrace();
        } catch (JsonMappingException e) {
            log.error("Error mapping response to JSON");
            response = Response.serverError().build();
            e.printStackTrace();
        } catch (IOException e) {
            log.error("I/O error while creating response JSON");
            response = Response.serverError().build();
            e.printStackTrace();
        }

        return response;
    }


}
