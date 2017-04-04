/**
 * Copyright 2015 Nicolas Ferry <${email}>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vsepml.storm.mcsuite;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.AuthorizationException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.vsepml.storm.twitter.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 20/01/2017.
 */
public class SavvyTopology {
    static final String TOPOLOGY_NAME = "Savvy Topology";
    private static final Logger journal = Logger.getLogger(SavvyTopology.class.getName());
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException {
        Config config = new Config();
        config.setNumWorkers(4);
        HashMap<String,String> map=new HashMap<String,String>();

        Properties props = new Properties();
        props.put("metadata.broker.list", args[0]);
        props.put("request.required.acks", "1");
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        config.put("kafka.broker.properties", props);


        String loc="/v1/locations/E15L17/machines";
        String r=callSavvy(loc, false);
        journal.log(Level.INFO, ">>>>>>>>>>>>>>>>>>>>>>");
        journal.log(Level.INFO, ">>>>>>>>>>>>>>>>>>>>>>:"+r);
        journal.log(Level.INFO, ">>>>>>>>>>>>>>>>>>>>>>");

        String loc3="/v1/locations/E15L17/machines/E15L17_VCVFQY_1/groups/CZDY1B/indicators";
        //String r=callSavvy(loc);
        String r2=callSavvy(loc3, false);
        org.json.simple.parser.JSONParser p =new JSONParser();
        try {
            JSONArray o= (JSONArray) p.parse(r2);
            for(Object obj: o){
                map.put(((JSONObject)obj).get("indicatorId").toString(), ((JSONObject)obj).get("indicatorName").toString());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }


        TopologyBuilder b = new TopologyBuilder();

        b.setSpout("sensorStream", new SavvySpout(r),1);
        b.setBolt("Splitter", new SavvySplitter(map),1).shuffleGrouping("sensorStream"); //split per line and add numbering
        b.setBolt("Splitter2", new SavvyMeasurementSplitter(map),1).shuffleGrouping("sensorStream"); //split per line and add numbering

        // CouchDB configuration
        config.put(CouchBolt.COUCHDB_URL, "http://"+args[1]+":5984");
        config.put(CouchBolt.COUCHDB_USER, "couchdb");
        config.put(CouchBolt.COUCHDB_PASSWORD, "couchdb");
        b.setBolt("couch", new CouchBolt(new SensorSerializer()).withBatching(1, 20),5).shuffleGrouping("Splitter");//store into couchdb
        b.setBolt("Kafka", new org.vsepml.storm.twitter.StormKafkaBolt<String,String>("Sensors"),2).shuffleGrouping("Splitter");//publish on kafka

        b.setBolt("movingAverage", new NumericalWindowAverageBolt(10), 5) // Windowed average
                .fieldsGrouping("Splitter2", new Fields("SensorId"));
        b.setBolt("spikes", new SpikeDetectionBolt(0.10f), 2) // Detect spike with respect to average
                .shuffleGrouping("movingAverage");

        b.setBolt("Kafka2", new org.vsepml.storm.twitter.StormKafkaBolt<String, Double>("Averages"),2).shuffleGrouping("movingAverage");
        b.setBolt("Kafka3", new org.vsepml.storm.twitter.StormKafkaBolt<String, Double>("Spikes"),1).shuffleGrouping("spikes");
        //b.setBolt("Kafka4", new StormKafkaBolt<String, Double>("Grafter"),2).shuffleGrouping("test");

        try {
            StormSubmitter.submitTopology(TOPOLOGY_NAME, config, b.createTopology());
        } catch (AuthorizationException e) {
            e.printStackTrace();
        }


    }

    public static String calculateRFC2104HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        byte[] signBytes =mac.doFinal(data.getBytes("UTF-8"));
        String signature = Base64.encode(signBytes);
        return signature;
    }

    public static String callSavvy(String loc, Boolean isStream){
        URL url = null;
        StringBuffer buffer = new StringBuffer();
        try {
            url = new URL("https://api-soraluce.savvyds.com"+loc);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type","text/plain; charset=UTF-8");
            long epoch = System.currentTimeMillis();
            connection.setRequestProperty("X-M2C-Sequence",epoch+"");
            connection.setDoOutput(true);

            String Request = "GET" + "\n"
                    + "text/plain; charset=UTF-8" + "\n"
                    + epoch+ "\n"
                    + loc;

            String Authorization="M2C" + " " + "xptbQCSLWL" + ":"
                    + calculateRFC2104HMAC(Request,"iY6PtqLBBbCgFaC69EAc");

            connection.setRequestProperty("Authorization", Authorization);

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String inputLine;
            int i=0;
            while ((inputLine = in.readLine()) != null) {
                if(!isStream) {
                    buffer.append(inputLine);
                }
            }
            in.close();


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }
}
