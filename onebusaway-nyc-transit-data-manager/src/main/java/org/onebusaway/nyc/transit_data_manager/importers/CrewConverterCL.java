package org.onebusaway.nyc.transit_data_manager.importers;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

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
            
            FileReader firstFile = null;
            try {
                System.out.println("opening " + inputFile);
                firstFile = new FileReader(inputFile);
            } catch (FileNotFoundException e) {
                System.out.println("Could not find " + inputFile);
                System.out.println(e.toString());
            }
            
            if (firstFile != null) {
                
                CrewAssignsInputConverter inConv = new CSVCrewAssignsInputConverter(firstFile);
                
                List<MtaUtsCrewAssignment> crewAssignments = inConv.getCrewAssignments();
                
                System.out.println("ran getCrewAssignments and got " + crewAssignments.size() + " results");
                
                CrewAssignmentsOutputConverter converter = new TCIPCrewAssignmentsOutputConverter(crewAssignments);
                List<SCHOperatorAssignment> opAssignments = converter.convertAssignments();
                
                StringWriter wrtr = new StringWriter();
        		String xml = null;
                try {
                	JAXBContext jc = JAXBContext.newInstance(SCHOperatorAssignment.class);
                	Marshaller m = jc.createMarshaller();
                	m.setProperty("jaxb.formatted.output", new Boolean(true));
                	m.setProperty("jaxb.fragment", new Boolean(true));
                	m.marshal(opAssignments.get(1), wrtr);
                	xml = wrtr.toString();
                } catch (JAXBException e) {
                	e.printStackTrace();
                }
                
                System.out.println(xml);
            }
            
        }
    }
}