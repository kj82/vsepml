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

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 20/01/2017.
 */
public class SavvySpout  extends BaseRichSpout {

    private static SpoutOutputCollector collector;
    private LinkedBlockingQueue<String> queue= new LinkedBlockingQueue<String>(1000000);
    private String loc2="/v1/stream?track=";

    private static final Logger journal = Logger.getLogger(SavvySpout.class.getName());

    public SavvySpout(String r){
        super();
        System.setProperty("jsse.enableSNIExtension", "false");

        if(!r.equals("")) {
            org.json.simple.parser.JSONParser p = new JSONParser();
            try {
                JSONArray o = (JSONArray) p.parse(r);
                Boolean first = true;
                for (Object obj : o) {
                    if (first)
                        loc2 += ((JSONObject) obj).get("machineId");
                    else loc2 += ((JSONObject) obj).get(",machineId");
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("measurement"));
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.collector = spoutOutputCollector;
        new Thread(new Runnable() {
            @Override
            public void run() {
                journal.log(Level.INFO, ">>>>>>>>>>>>>>>>>>>>"+loc2);
                callSavvy(loc2, true);
            }
        }).start();
    }

    @Override
    public void nextTuple() {
        String st = queue.poll();
        if (st == null) {
            Utils.sleep(5);
        } else {
            collector.emit(new Values(st));
        }
    }

    private final String HMAC_SHA1_ALGORITHM = "HmacSHA1";


    public String calculateRFC2104HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        byte[] signBytes =mac.doFinal(data.getBytes("UTF-8"));
        String signature = Base64.encode(signBytes);
        return signature;
    }

    public String callSavvy(String loc, Boolean isStream){
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
                if(isStream) {
                    if(i%2 == 1) {
                        queue.offer(inputLine);
                    }
                    i++;
                }else{
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
