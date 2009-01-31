/*
 * Copyright 2003-2004 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 */

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

public class ImplVersionCommand {

    public static void main(String[] args) throws Exception {
	// Instantiate the MBean server
	//
	System.out.println("Create the MBean server");
	MBeanServer mbs = MBeanServerFactory.createMBeanServer();

	// Get the JMX implementation version from the MBeanServerDelegateMBean
	//
	System.out.println("Get the JMX implementation version");
	ObjectName mbsdName =
	    new ObjectName("JMImplementation:type=MBeanServerDelegate");
	String mbsdAttribute = "ImplementationVersion";
	String mbsdVersion = (String) mbs.getAttribute(mbsdName, mbsdAttribute);

	// Display JMX implementation version and JVM implementation version
	//
	System.out.println("JMX implementation version          = " +
			   mbsdVersion);
	System.out.println("Java Runtime implementation version = " +
			   args[0]);

	// Check JMX implementation version vs. JVM implementation version
	//
	if (Boolean.valueOf(args[1]).booleanValue()) {
	    if (!mbsdVersion.equals(args[0]))
		throw new IllegalArgumentException(
		  "JMX and Java Runtime implementation versions do not match!");
	    // Test OK!
	    //
	    System.out.println("JMX and Java Runtime implementation " +
			       "versions match!");
	} else {
	    // Test OK!
	    //
	    System.out.println("JMX and Java Runtime implementation " +
			       "versions do not match because the test " +
			       "is using an unbundled version of JMX!");
	}
	System.out.println("Bye! Bye!");
    }
}
