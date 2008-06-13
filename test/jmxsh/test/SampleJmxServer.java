/*
 * $URL$
 * 
 * $Revision$ 
 * 
 * $LastChangedDate$
 *
 * $LastChangedBy$
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package jmxsh.test;

import javax.management.*;
import java.lang.management.*;
import java.io.IOException;

/**
 * A simple JMX server to use for testing.
 *
 * @author Dad
  */
public class SampleJmxServer {

     public static void main(String[] args) {
	 System.out.println("Starting up server...");

	 MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	 
	 
	 System.out.println("Press enter to quit.");
	 try {
	     int c = System.in.read();
	 }
	 catch (IOException e) {
	     System.err.println("Error reading input.");
	 }

	 System.out.println("Shutting down server...");
    }

}
