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
package jmxsh;

/**
   A tuplet that identifies a server/mbean/attribute/operation.
*/

class Context {
    String server;
    String domain;
    String mbean;
    String attrop;

    Context() {
	this.server = null;
	this.domain = null;
	this.mbean = null;
	this.attrop = null;
    }

    static private Context tclContext = new Context();

    static Context fromTcl() {
	tclContext.server = JInterp.getGlobal("SERVER", null);
	tclContext.domain = JInterp.getGlobal("DOMAIN", null);
	tclContext.mbean = JInterp.getGlobal("MBEAN", null);
	tclContext.attrop = JInterp.getGlobal("ATTROP", null);
	return tclContext;
    }
}
