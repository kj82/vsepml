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

/**
 * Created by ferrynico on 07/07/2016.
 */
public class Header {

    private String type="";
    private String unit="";
    private String coeff="";
    private String name="";
    private String fullName="";

    public Header(String h){
        this.fullName=h;
        String[] head = h.split("_");
        if(head.length > 1) {
            name = head[0] + "_" + head[1];//Name is always the two firsts
            if (head.length > 4) {//Axis or spindle, then there is a coeff
                type = head[2];
                unit = head[3];
                coeff = head[4];
            } else {
                if(head[1].equals("Program") || head[1].equals("Tool") ){//Rule is broken need ad-hoc crap
                    name=head[0] + "_" + head[1] + "_" + head[2];
                    if (head.length == 4) {
                        type = head[3];
                    }
                }else {
                    if (head.length > 2) {
                        type = head[2];
                        if (head.length == 4) {
                            unit = head[3];
                        }
                    }
                }
            }
        }else{
            name = head[0];
        }

    }

    public String getFullName(){
        return fullName ;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCoeff() {
        return coeff;
    }

    public void setCoeff(String coeff) {
        this.coeff = coeff;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
