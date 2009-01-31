/*
 * Copyright 2000-2003 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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

package com.sun.jmx.snmp.agent;

// jmx imports
//
import com.sun.jmx.snmp.SnmpValue;
import com.sun.jmx.snmp.SnmpStatusException;

/**
 * <p>
 * This interface defines the methods that must be implemented by an
 * SNMP metadata object that needs to interact with an
 * {@link com.sun.jmx.snmp.agent.SnmpStandardObjectServer} object.
 * </p>
 * <p>
 * All these methods are usually generated by <code>mibgen</code> when
 * run in standard-metadata mode (default).
 * </p>
 * <p><b><i>
 * This interface is used internally between the generated Metadata and 
 * the SNMP runtime and you shouldn't need to worry about it, because 
 * you will never have to use it directly.
 * </b></i></p>
 *
 * <p><b>This API is a Sun Microsystems internal API  and is subject 
 * to change without notice.</b></p>
 **/ 
public interface SnmpStandardMetaServer {
    /**
     * Returns the value of the scalar object identified by the given
     * OID arc.
     *
     * @param arc OID arc of the querried scalar object.
     *
     * @return The <CODE>SnmpValue</CODE> of the scalar object identified 
     *         by <CODE>arc</CODE>.
     *
     * @param userData A contextual object containing user-data.
     *        This object is allocated through the <code>
     *        {@link com.sun.jmx.snmp.agent.SnmpUserDataFactory}</code>
     *        for each incoming SNMP request.
     *
     * @exception SnmpStatusException If the arc is not valid, or if
     *    access is denied.
     *
     **/
    public SnmpValue get(long arc, Object userData) 
	throws SnmpStatusException ;

    /**
     * Sets the value of the scalar object identified by the given
     * OID arc.
     *
     * @param x New value for the scalar object identified by 
     *    <CODE>arc</CODE>
     *
     * @param arc OID arc of the scalar object whose value is set.
     *
     * @return The new <CODE>SnmpValue</CODE> of the scalar object 
     *    identified by <CODE>arc</CODE>.
     *
     * @param userData A contextual object containing user-data.
     *        This object is allocated through the <code>
     *        {@link com.sun.jmx.snmp.agent.SnmpUserDataFactory}</code>
     *        for each incoming SNMP request.
     *
     * @exception SnmpStatusException If the arc is not valid, or if
     *    access is denied.
     *
     **/
    public SnmpValue set(SnmpValue x, long arc, Object userData) 
	throws SnmpStatusException ;

    /**
     * Checks that the new desired value of the scalar object identified 
     * by the given OID arc is valid.
     *
     * @param x New value for the scalar object identified by 
     *    <CODE>arc</CODE>
     *
     * @param arc OID arc of the scalar object whose value is set.
     *
     * @param userData A contextual object containing user-data.
     *        This object is allocated through the <code>
     *        {@link com.sun.jmx.snmp.agent.SnmpUserDataFactory}</code>
     *        for each incoming SNMP request.
     *
     * @exception SnmpStatusException If the arc is not valid, or if
     *    access is denied, or if the new desired value is not valid.
     *
     **/
    public void check(SnmpValue x, long arc, Object userData) 
	throws SnmpStatusException ;

}
