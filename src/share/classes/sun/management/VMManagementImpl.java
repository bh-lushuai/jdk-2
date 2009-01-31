/*
 * Copyright 2003-2006 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.management;

import sun.misc.Perf;
import sun.management.counter.*;
import sun.management.counter.perf.*;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.security.action.GetPropertyAction;

/**
 * Implementation of VMManagement interface that accesses the management 
 * attributes and operations locally within the same Java virtual
 * machine.
 */
class VMManagementImpl implements VMManagement {

    private static String version;

    private static boolean compTimeMonitoringSupport;
    private static boolean threadContentionMonitoringSupport;
    private static boolean currentThreadCpuTimeSupport; 
    private static boolean otherThreadCpuTimeSupport;
    private static boolean bootClassPathSupport;
    private static boolean objectMonitorUsageSupport;
    private static boolean synchronizerUsageSupport;

    static {
        version = getVersion0();
        if (version == null) {
            throw new InternalError("Invalid Management Version");
        }
        initOptionalSupportFields();
    }
    private native static String getVersion0();
    private native static void initOptionalSupportFields();

    // Optional supports
    public boolean isCompilationTimeMonitoringSupported() {
        return compTimeMonitoringSupport;
    }

    public boolean isThreadContentionMonitoringSupported() {
        return threadContentionMonitoringSupport;
    }

    public boolean isCurrentThreadCpuTimeSupported() {
        return currentThreadCpuTimeSupport;
    }

    public boolean isOtherThreadCpuTimeSupported() {
        return otherThreadCpuTimeSupport; 
    }

    public boolean isBootClassPathSupported() {
        return bootClassPathSupport;
    }

    public boolean isObjectMonitorUsageSupported() {
        return objectMonitorUsageSupport;
    }

    public boolean isSynchronizerUsageSupported() {
        return synchronizerUsageSupport;
    }

    public native boolean isThreadContentionMonitoringEnabled(); 
    public native boolean isThreadCpuTimeEnabled();


    // Class Loading Subsystem
    public int    getLoadedClassCount() {
        long count = getTotalClassCount() - getUnloadedClassCount();
        return (int) count;
    }
    public native long getTotalClassCount();
    public native long getUnloadedClassCount();

    public native boolean getVerboseClass();

    // Memory Subsystem
    public native boolean getVerboseGC();

    // Runtime Subsystem
    public String   getManagementVersion() {
        return version;
    }

    public String getVmId() {
        int pid = getProcessId();
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // ignore
        }
 
        return pid + "@" + hostname;
    }
    private native int getProcessId();

    public String   getVmName() {
        return System.getProperty("java.vm.name");
    }

    public String   getVmVendor() {
        return System.getProperty("java.vm.vendor");
    }
    public String   getVmVersion() {
        return System.getProperty("java.vm.version");
    }
    public String   getVmSpecName()  {
        return System.getProperty("java.vm.specification.name");
    }
    public String   getVmSpecVendor() {
        return System.getProperty("java.vm.specification.vendor");
    }
    public String   getVmSpecVersion() {
        return System.getProperty("java.vm.specification.version");
    }
    public String   getClassPath() {
        return System.getProperty("java.class.path");
    }
    public String   getLibraryPath()  {
        return System.getProperty("java.library.path");
    }

    public String   getBootClassPath( ) {
        PrivilegedAction<String> pa
            = new GetPropertyAction("sun.boot.class.path");
        String result =  AccessController.doPrivileged(pa);
        return result;
    }

    private List<String> vmArgs = null;
    public synchronized List<String> getVmArguments() {
        if (vmArgs == null) {
            String[] args = getVmArguments0();
            List<String> l = ((args != null && args.length != 0) ? Arrays.asList(args) : 
					Collections.<String>emptyList());
            vmArgs = Collections.unmodifiableList(l); 
        }
        return vmArgs;
    }
    public native String[] getVmArguments0();

    public native long getStartupTime();
    public native int getAvailableProcessors();

    // Compilation Subsystem
    public String   getCompilerName() {
        String name =  AccessController.doPrivileged(
            new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty("sun.management.compiler");
                }
            });
        return name;
    }
    public native long getTotalCompileTime();

    // Thread Subsystem
    public native long getTotalThreadCount();
    public native int  getLiveThreadCount();
    public native int  getPeakThreadCount();
    public native int  getDaemonThreadCount();

    // Operating System
    public String getOsName() {
        return System.getProperty("os.name");
    }
    public String getOsArch() {
        return System.getProperty("os.arch");
    }
    public String getOsVersion() {
        return System.getProperty("os.version");
    }

    // Hotspot-specific runtime support
    public native long getSafepointCount();
    public native long getTotalSafepointTime();
    public native long getSafepointSyncTime();
    public native long getTotalApplicationNonStoppedTime();

    public native long getLoadedClassSize();
    public native long getUnloadedClassSize();
    public native long getClassLoadingTime();
    public native long getMethodDataSize();
    public native long getInitializedClassCount();
    public native long getClassInitializationTime();
    public native long getClassVerificationTime();

    // Performance Counter Support
    private PerfInstrumentation perfInstr = null;
    private boolean noPerfData = false;

    private synchronized PerfInstrumentation getPerfInstrumentation() {  
        if (noPerfData || perfInstr != null) {
             return perfInstr;
        }

        // construct PerfInstrumentation object 
        Perf perf =  AccessController.doPrivileged(new Perf.GetPerfAction());
        try {
            ByteBuffer bb = perf.attach(0, "r");
            if (bb.capacity() == 0) {
                noPerfData = true;
                return null;
            }
            perfInstr = new PerfInstrumentation(bb);
        } catch (IllegalArgumentException e) {
            // If the shared memory doesn't exist e.g. if -XX:-UsePerfData 
            // was set
            noPerfData = true;
        } catch (IOException e) {
            throw new InternalError(e.getMessage());
        }
        return perfInstr;
    }

    public List<Counter> getInternalCounters(String pattern) {
        PerfInstrumentation perf = getPerfInstrumentation();
        if (perf != null) {
            return perf.findByPattern(pattern);
        } else {
            return Collections.emptyList();
        }
    }
}
