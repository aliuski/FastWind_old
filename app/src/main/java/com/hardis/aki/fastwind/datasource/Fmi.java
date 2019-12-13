package com.hardis.aki.fastwind.datasource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Fmi extends WeatherData {
    public Fmi(String input) throws Exception{
        readWeather(input);
    }

    public void readWeather(String input) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.parse(input);

        doc.getDocumentElement().normalize();
        NodeList nodeLst = doc.getElementsByTagName("wfs:member");
        Vector time = new Vector();
        Vector v1 = readMember((Element) nodeLst.item(0),time);
        Vector v2 = readMember((Element) nodeLst.item(1),null);
        Vector v3 = readMember((Element) nodeLst.item(2),null);
        Vector v4 = readMember((Element) nodeLst.item(3),null);
        fillTables(time,v1,v2,v3,v4);
    }

    private Vector readMember(Element element, Vector time) {
        Vector v = new Vector();
        NodeList nodeLst = element.getElementsByTagName("omso:PointTimeSeriesObservation");
        element = (Element) nodeLst.item(0);
        nodeLst = element.getElementsByTagName("om:result");
        element = (Element) nodeLst.item(0);
        nodeLst = element.getElementsByTagName("wml2:MeasurementTimeseries");
        element = (Element) nodeLst.item(0);

        NodeList nodeLst1 = element.getElementsByTagName("wml2:point");

        for (int k = 0; k < nodeLst1.getLength(); k++) {
            Element element1 = (Element) nodeLst1.item(k);

            NodeList nodeLst2 = element1.getElementsByTagName("wml2:MeasurementTVP");
            Element element2 = (Element) nodeLst2.item(0);

            if(time != null) {
                nodeLst2 = element2.getElementsByTagName("wml2:time");
                element1 = (Element) nodeLst2.item(0);
                nodeLst2 = element1.getChildNodes();
                time.add(((Node) nodeLst2.item(0)).getNodeValue());
            }
            nodeLst2 = element2.getElementsByTagName("wml2:value");
            element1 = (Element) nodeLst2.item(0);
            nodeLst2 = element1.getChildNodes();

            v.add(((Node) nodeLst2.item(0)).getNodeValue());
        }
        return v;
    }

    private void fillTables(Vector time,Vector v1,Vector v2,Vector v3,Vector v4) throws Exception {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        step = new Date[v1.size()];
        windspeed = new double[v1.size()];
        winddirection = new int[v1.size()];
        windspeedwg = new double[v1.size()];
        tempature = new double[v1.size()];

        for (int loop = 0; loop < v1.size(); loop++) {
            try{
                step[loop] = df.parse((String)time.get(loop));
            } catch (Exception e) {
                e.printStackTrace();
            }
            windspeed[loop] = Double.parseDouble((String) v1.get(loop));
            winddirection[loop] = (int)(Double.parseDouble((String) v2.get(loop)) + 90.0);
            windspeedwg[loop] = Double.parseDouble((String) v3.get(loop));
            tempature[loop] = Double.parseDouble((String) v4.get(loop));
        }

        minutesincycle = (int)(step[1].getTime() - step[0].getTime()) / 60000;
        updated = new java.util.Date();
    }
}
