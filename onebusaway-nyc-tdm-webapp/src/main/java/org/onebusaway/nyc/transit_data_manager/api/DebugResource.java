package org.onebusaway.nyc.transit_data_manager.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.nio.file.Paths;

@Component
@Path("/debug")
public class DebugResource {

    private static Logger _log = LoggerFactory.getLogger(DebugResource.class);


    @Path("/versions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersions(@QueryParam("property") String property) throws JSONException {

        JSONObject result = new JSONObject();

        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            Enumeration<URL> resources =
                    cl.getResources("META-INF/maven");

            while (resources.hasMoreElements()) {
                URL baseUrl = resources.nextElement();

                if ("jar".equals(baseUrl.getProtocol())) {
                    scanJar(baseUrl, property, result);
                } else if ("file".equals(baseUrl.getProtocol())) {
                    scanDirectory(baseUrl, property, result);
                }
            }

        } catch (Exception e) {
            _log.error("Unable to process dependency versions request", e);
            return Response.serverError()
                    .entity("Failed to enumerate dependency versions")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        return Response
                .ok(result.toString(2))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    private void scanJar(URL baseUrl,
                         String property,
                         JSONObject result) throws IOException, JSONException {

        String path = baseUrl.getPath();
        String jarPath = path.substring(5, path.indexOf("!"));

        try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (!entry.getName().endsWith("pom.properties")) {
                    continue;
                }

                try (InputStream is = jar.getInputStream(entry)) {
                    processPomProperties(is, property, result);
                }
            }
        }
    }

    private void scanDirectory(URL baseUrl,
                               String property,
                               JSONObject result) throws IOException, URISyntaxException {

        java.nio.file.Path basePath = Paths.get(baseUrl.toURI());

        Files.walk(basePath)
                .filter(p -> p.getFileName().toString().equals("pom.properties"))
                .forEach(p -> {
                    try (InputStream is = Files.newInputStream(p)) {
                        processPomProperties(is, property, result);
                    } catch (Exception ignored) {
                    }
                });
    }


    private void processPomProperties(InputStream is,
                                      String property,
                                      JSONObject result) throws IOException, JSONException {

        Properties props = new Properties();
        props.load(is);

        String key = props.getProperty("groupId") + ":" +
                props.getProperty("artifactId");

        JSONObject entry = new JSONObject();

        if (property != null && !property.isBlank()) {
            String value = props.getProperty(property);
            if (value != null) {
                entry.put(property, value);
            } else {
                return;
            }
        } else {
            for (String name : props.stringPropertyNames()) {
                entry.put(name, props.getProperty(name));
            }
        }

        result.put(key, entry);
    }

    @Path("/protobuf")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProtobuf() throws JSONException {

        JSONObject result = new JSONObject();

        try {
            URL location = com.google.protobuf.util.JsonFormat.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            result.put("protobuf", location.toURI().toString());

        } catch (Exception e) {
            _log.error("Unable to process protobuf request", e);
            return Response.serverError()
                    .entity("Failed to enumerate protobuf request")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        return Response
                .ok(result.toString(2))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }




}

