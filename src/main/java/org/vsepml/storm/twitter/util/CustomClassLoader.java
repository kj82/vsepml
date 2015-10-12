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
package org.vsepml.storm.twitter.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by ferrynico on 26/09/15.
 */
public class CustomClassLoader extends ClassLoader{

    public CustomClassLoader(ClassLoader parent) {
        super(parent);
    }

    public synchronized Class loadClass(String className, boolean resolveIt) throws
            ClassNotFoundException {
        Class result;
        byte[] classData;


        try {
            result = super.findSystemClass(className);
            return result;
        } catch(ClassNotFoundException e) {
            System.out.println("Not a System Class!");
        }

        classData = getImplFromWWW(className);
        if(classData == null) {
            throw new ClassNotFoundException();
        }

        result=defineClass(className,classData,0,classData.length);

        if(resolveIt)
            resolveClass(result);


        return result;
    }

    private byte[] getImplFromWWW(String urlClassName){
        URL url = null;
        try {
            url = new URL(urlClassName);
            URLConnection uc = url.openConnection();
            int length = uc.getContentLength();
            InputStream is = uc.getInputStream();
            byte[] data = new byte[length];
            is.read(data);
            is.close();
            return data;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
