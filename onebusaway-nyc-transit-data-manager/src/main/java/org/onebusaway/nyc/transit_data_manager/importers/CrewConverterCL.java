package org.onebusaway.nyc.transit_data_manager.importers;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

import tcip_final_3_0_5_1.ObjectFactory;
import tcip_final_3_0_5_1.SCHOperatorAssignment;
import tcip_final_3_0_5_1.SchPushOperatorAssignments;

public class CrewConverterCL {
    public static void main (String[] args) {
        String inputFile = null;
        String outputFile = null;
        
        if (2 != args.length) {
            System.out.println("Please add the input and output files as arguments");
        } else {
            inputFile = args[0];
            outputFile = args[1];
            
            FileReader inputFileReader = null;
            FileWriter outputFileWriter = null;
            try {
                System.out.println("opening " + inputFile);
                inputFileReader = new FileReader(inputFile);
            } catch (FileNotFoundException e) {
                System.out.println("Could not find " + inputFile);
                e.printStackTrace();
            }
            
            try {
            	System.out.println("Opening " + outputFile + " for writing.");
            	outputFileWriter = new FileWriter(outputFile);
            } catch (IOException e) {
            	System.out.println("Could not create FileWriter for " + outputFile);
            	e.printStackTrace();
            }
            
            if (inputFileReader != null && outputFileWriter != null) {
            	ObjectFactory tcipFinalObjectFactory = new ObjectFactory();
            	
                
                CrewAssignsInputConverter inConv = new CSVCrewAssignsInputConverter(inputFileReader);
                
                List<MtaUtsCrewAssignment> crewAssignments = inConv.getCrewAssignments();
                
                System.out.println("ran getCrewAssignments and got " + crewAssignments.size() + " results");
                
                CrewAssignmentsOutputConverter converter = new TCIPCrewAssignmentsOutputConverter(crewAssignments);
                List<SCHOperatorAssignment> opAssignments = converter.convertAssignments();
                
                GregorianCalendar nowCal = new GregorianCalendar();
                PushOperatorAssignsGenerator opAssignsGen = new PushOperatorAssignsGenerator(nowCal);
                SchPushOperatorAssignments opAssignsPush = opAssignsGen.generateFromOpAssignList(opAssignments);
                
                JAXBElement<SchPushOperatorAssignments> opAssignsPushJaxbElement = tcipFinalObjectFactory.createSchPushOperatorAssignments(opAssignsPush);
                
                StringWriter wrtr = new StringWriter();
        		String xml = null;
                try {
                	JAXBContext jc = JAXBContext.newInstance(SchPushOperatorAssignments.class);
                	Marshaller m = jc.createMarshaller();
                	m.setProperty("jaxb.formatted.output", new Boolean(true));
                	//m.setProperty("jaxb.fragment", new Boolean(true));
                	m.marshal(opAssignsPushJaxbElement, wrtr);
                	xml = wrtr.toString();
                } catch (JAXBException e) {
                	e.printStackTrace();
                }
                
                try {
                	outputFileWriter.write(xml);
                } catch (IOException e) {
                	e.printStackTrace();
                }
                
                System.out.println("done!");
                
            }
            
        }
    }
}