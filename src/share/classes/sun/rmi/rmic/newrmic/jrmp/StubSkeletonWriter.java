/*
 * Copyright 2003-2005 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.rmi.rmic.newrmic.jrmp;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import sun.rmi.rmic.newrmic.BatchEnvironment;
import sun.rmi.rmic.newrmic.IndentingWriter;

import static sun.rmi.rmic.newrmic.Constants.*;
import static sun.rmi.rmic.newrmic.jrmp.Constants.*;

/**
 * Writes the source code for the stub class and (optionally) skeleton
 * class for a particular remote implementation class.
 *
 * WARNING: The contents of this source file are not part of any
 * supported API.  Code that depends on them does so at its own risk:
 * they are subject to change or removal without notice.
 *
 * @version %I%, %E%
 * @author Peter Jones
 **/
class StubSkeletonWriter {

    /** rmic environment for this object */
    private final BatchEnvironment env;

    /** the remote implemention class to generate code for */
    private final RemoteClass remoteClass;

    /** version of the JRMP stub protocol to generate code for */
    private final StubVersion version;

    /*
     * binary names of the stub and skeleton classes to generate for
     * the remote class
     */
    private final String stubClassName;
    private final String skeletonClassName;

    /* package name and simple names of the stub and skeleton classes */
    private final String packageName;
    private final String stubClassSimpleName;
    private final String skeletonClassSimpleName;

    /** remote methods of class, indexed by operation number */
    private final RemoteClass.Method[] remoteMethods;

    /**
     * Names to use for the java.lang.reflect.Method static fields in
     * the generated stub class corresponding to each remote method.
     **/
    private final String[] methodFieldNames;

    /**
     * Creates a StubSkeletonWriter instance for the specified remote
     * implementation class.  The generated code will implement the
     * specified JRMP stub protocol version.
     **/
    StubSkeletonWriter(BatchEnvironment env,
		       RemoteClass remoteClass,
		       StubVersion version)
    {
	this.env = env;
	this.remoteClass = remoteClass;
	this.version = version;

	stubClassName = Util.binaryNameOf(remoteClass.classDoc()) + "_Stub";
	skeletonClassName =
	    Util.binaryNameOf(remoteClass.classDoc()) + "_Skel";

	int i = stubClassName.lastIndexOf('.');
	packageName = (i != -1 ? stubClassName.substring(0, i) : "");
	stubClassSimpleName = stubClassName.substring(i + 1);
	skeletonClassSimpleName = skeletonClassName.substring(i + 1);

	remoteMethods = remoteClass.remoteMethods();
	methodFieldNames = nameMethodFields(remoteMethods);
    }

    /**
     * Returns the binary name of the stub class to generate for the
     * remote implementation class.
     **/
    String stubClassName() {
	return stubClassName;
    }

    /**
     * Returns the binary name of the skeleton class to generate for
     * the remote implementation class.
     **/
    String skeletonClassName() {
	return skeletonClassName;
    }

    /**
     * Writes the stub class for the remote class to a stream.
     **/
    void writeStub(IndentingWriter p) throws IOException {

	/*
	 * Write boiler plate comment.
	 */
	p.pln("// Stub class generated by rmic, do not edit.");
	p.pln("// Contents subject to change without notice.");
	p.pln();

	/*
	 * If remote implementation class was in a particular package,
	 * declare the stub class to be in the same package.
	 */
	if (!packageName.equals("")) {
	    p.pln("package " + packageName + ";");
	    p.pln();
	}

	/*
	 * Declare the stub class; implement all remote interfaces.
	 */
	p.plnI("public final class " + stubClassSimpleName);
	p.pln("extends " + REMOTE_STUB);
	ClassDoc[] remoteInterfaces = remoteClass.remoteInterfaces();
	if (remoteInterfaces.length > 0) {
	    p.p("implements ");
	    for (int i = 0; i < remoteInterfaces.length; i++) {
		if (i > 0) {
		    p.p(", ");
		}
		p.p(remoteInterfaces[i].qualifiedName());
	    }
	    p.pln();
	}
	p.pOlnI("{");

	if (version == StubVersion.V1_1 ||
	    version == StubVersion.VCOMPAT)
	{
	    writeOperationsArray(p);
	    p.pln();
	    writeInterfaceHash(p);
	    p.pln();
	}

	if (version == StubVersion.VCOMPAT ||
	    version == StubVersion.V1_2)
	{
	    p.pln("private static final long serialVersionUID = " +
		STUB_SERIAL_VERSION_UID + ";");
	    p.pln();

	    /*
	     * We only need to declare and initialize the static fields of
	     * Method objects for each remote method if there are any remote
	     * methods; otherwise, skip this code entirely, to avoid generating
	     * a try/catch block for a checked exception that cannot occur
	     * (see bugid 4125181).
	     */
	    if (methodFieldNames.length > 0) {
		if (version == StubVersion.VCOMPAT) {
		    p.pln("private static boolean useNewInvoke;");
		}
		writeMethodFieldDeclarations(p);
		p.pln();

		/*
		 * Initialize java.lang.reflect.Method fields for each remote
		 * method in a static initializer.
		 */
		p.plnI("static {");
		p.plnI("try {");
		if (version == StubVersion.VCOMPAT) {
		    /*
		     * Fat stubs must determine whether the API required for
		     * the JDK 1.2 stub protocol is supported in the current
		     * runtime, so that it can use it if supported.  This is
		     * determined by using the Reflection API to test if the
		     * new invoke method on RemoteRef exists, and setting the
		     * static boolean "useNewInvoke" to true if it does, or
		     * to false if a NoSuchMethodException is thrown.
		     */
		    p.plnI(REMOTE_REF + ".class.getMethod(\"invoke\",");
		    p.plnI("new java.lang.Class[] {");
		    p.pln(REMOTE + ".class,");
		    p.pln("java.lang.reflect.Method.class,");
		    p.pln("java.lang.Object[].class,");
		    p.pln("long.class");
		    p.pOln("});");
		    p.pO();
		    p.pln("useNewInvoke = true;");
		}
		writeMethodFieldInitializers(p);
		p.pOlnI("} catch (java.lang.NoSuchMethodException e) {");
		if (version == StubVersion.VCOMPAT) {
		    p.pln("useNewInvoke = false;");
		} else {
		    p.plnI("throw new java.lang.NoSuchMethodError(");
		    p.pln("\"stub class initialization failed\");");
		    p.pO();
		}
		p.pOln("}");		// end try/catch block
		p.pOln("}");		// end static initializer
		p.pln();
	    }
	}

	writeStubConstructors(p);
	p.pln();

	/*
	 * Write each stub method.
	 */
	if (remoteMethods.length > 0) {
	    p.pln("// methods from remote interfaces");
	    for (int i = 0; i < remoteMethods.length; ++i) {
		p.pln();
		writeStubMethod(p, i);
	    }
	}

	p.pOln("}");			// end stub class
    }

    /**
     * Writes the constructors for the stub class.
     **/
    private void writeStubConstructors(IndentingWriter p)
	throws IOException
    {
	p.pln("// constructors");

	/*
	 * Only stubs compatible with the JDK 1.1 stub protocol need
	 * a no-arg constructor; later versions use reflection to find
	 * the constructor that directly takes a RemoteRef argument.
	 */
	if (version == StubVersion.V1_1 ||
	    version == StubVersion.VCOMPAT)
	{
	    p.plnI("public " + stubClassSimpleName + "() {");
	    p.pln("super();");
	    p.pOln("}");
	}

	p.plnI("public " + stubClassSimpleName + "(" + REMOTE_REF + " ref) {");
	p.pln("super(ref);");
	p.pOln("}");
    }

    /**
     * Writes the stub method for the remote method with the given
     * operation number.
     **/
    private void writeStubMethod(IndentingWriter p, int opnum)
	throws IOException
    {
	RemoteClass.Method method = remoteMethods[opnum];
	MethodDoc methodDoc = method.methodDoc();
	String methodName = methodDoc.name();
	Type[] paramTypes = method.parameterTypes();
	String paramNames[] = nameParameters(paramTypes);
	Type returnType = methodDoc.returnType();
	ClassDoc[] exceptions = method.exceptionTypes();

	/*
	 * Declare stub method; throw exceptions declared in remote
	 * interface(s).
	 */
	p.pln("// implementation of " +
	      Util.getFriendlyUnqualifiedSignature(methodDoc));
	p.p("public " + returnType.toString() + " " + methodName + "(");
	for (int i = 0; i < paramTypes.length; i++) {
	    if (i > 0) {
		p.p(", ");
	    }
	    p.p(paramTypes[i].toString() + " " + paramNames[i]);
	}
	p.plnI(")");
	if (exceptions.length > 0) {
	    p.p("throws ");
	    for (int i = 0; i < exceptions.length; i++) {
		if (i > 0) {
		    p.p(", ");
		}
		p.p(exceptions[i].qualifiedName());
	    }
	    p.pln();
	}
	p.pOlnI("{");

	/*
	 * The RemoteRef.invoke methods throw Exception, but unless
	 * this stub method throws Exception as well, we must catch
	 * Exceptions thrown from the invocation.  So we must catch
	 * Exception and rethrow something we can throw:
	 * UnexpectedException, which is a subclass of
	 * RemoteException.  But for any subclasses of Exception that
	 * we can throw, like RemoteException, RuntimeException, and
	 * any of the exceptions declared by this stub method, we want
	 * them to pass through unmodified, so first we must catch any
	 * such exceptions and rethrow them directly.
	 *
	 * We have to be careful generating the rethrowing catch
	 * blocks here, because javac will flag an error if there are
	 * any unreachable catch blocks, i.e. if the catch of an
	 * exception class follows a previous catch of it or of one of
	 * its superclasses.  The following method invocation takes
	 * care of these details.
	 */
	List<ClassDoc> catchList = computeUniqueCatchList(exceptions);

	/*
	 * If we need to catch any particular exceptions (i.e. this method
	 * does not declare java.lang.Exception), put the entire stub
	 * method in a try block.
	 */
	if (catchList.size() > 0) {
	    p.plnI("try {");
	}

	if (version == StubVersion.VCOMPAT) {
	    p.plnI("if (useNewInvoke) {");
	}
	if (version == StubVersion.VCOMPAT ||
	    version == StubVersion.V1_2)
	{
	    if (!Util.isVoid(returnType)) {
		p.p("Object $result = ");		// REMIND: why $?
	    }
	    p.p("ref.invoke(this, " + methodFieldNames[opnum] + ", ");
	    if (paramTypes.length > 0) {
		p.p("new java.lang.Object[] {");
		for (int i = 0; i < paramTypes.length; i++) {
		    if (i > 0)
			p.p(", ");
		    p.p(wrapArgumentCode(paramTypes[i], paramNames[i]));
		}
		p.p("}");
	    } else {
		p.p("null");
	    }
	    p.pln(", " + method.methodHash() + "L);");
	    if (!Util.isVoid(returnType)) {
		p.pln("return " +
		    unwrapArgumentCode(returnType, "$result") + ";");
	    }
	}
	if (version == StubVersion.VCOMPAT) {
	    p.pOlnI("} else {");
	}
	if (version == StubVersion.V1_1 ||
	    version == StubVersion.VCOMPAT)
	{
	    p.pln(REMOTE_CALL + " call = ref.newCall((" + REMOTE_OBJECT +
		") this, operations, " + opnum + ", interfaceHash);");

	    if (paramTypes.length > 0) {
		p.plnI("try {");
		p.pln("java.io.ObjectOutput out = call.getOutputStream();");
		writeMarshalArguments(p, "out", paramTypes, paramNames);
		p.pOlnI("} catch (java.io.IOException e) {");
		p.pln("throw new " + MARSHAL_EXCEPTION +
		    "(\"error marshalling arguments\", e);");
		p.pOln("}");
	    }

	    p.pln("ref.invoke(call);");

	    if (Util.isVoid(returnType)) {
		p.pln("ref.done(call);");
	    } else {
		p.pln(returnType.toString() + " $result;");
							// REMIND: why $?
		p.plnI("try {");
		p.pln("java.io.ObjectInput in = call.getInputStream();");
		boolean objectRead =
		    writeUnmarshalArgument(p, "in", returnType, "$result");
		p.pln(";");
		p.pOlnI("} catch (java.io.IOException e) {");
		p.pln("throw new " + UNMARSHAL_EXCEPTION +
		    "(\"error unmarshalling return\", e);");
		/*
		 * If any only if readObject has been invoked, we must catch
		 * ClassNotFoundException as well as IOException.
		 */
		if (objectRead) {
		    p.pOlnI("} catch (java.lang.ClassNotFoundException e) {");
		    p.pln("throw new " + UNMARSHAL_EXCEPTION +
			"(\"error unmarshalling return\", e);");
		}
		p.pOlnI("} finally {");
		p.pln("ref.done(call);");
		p.pOln("}");
		p.pln("return $result;");
	    }
	}
	if (version == StubVersion.VCOMPAT) {
	    p.pOln("}");		// end if/else (useNewInvoke) block
	}

	/*
	 * If we need to catch any particular exceptions, finally write
	 * the catch blocks for them, rethrow any other Exceptions with an
	 * UnexpectedException, and end the try block.
	 */
	if (catchList.size() > 0) {
	    for (ClassDoc catchClass : catchList) {
		p.pOlnI("} catch (" + catchClass.qualifiedName() + " e) {");
		p.pln("throw e;");
	    }
	    p.pOlnI("} catch (java.lang.Exception e) {");
	    p.pln("throw new " + UNEXPECTED_EXCEPTION +
		"(\"undeclared checked exception\", e);");
	    p.pOln("}");		// end try/catch block
	}

	p.pOln("}");			// end stub method
    }

    /**
     * Computes the exceptions that need to be caught and rethrown in
     * a stub method before wrapping Exceptions in
     * UnexpectedExceptions, given the exceptions declared in the
     * throws clause of the method.  Returns a list containing the
     * exception to catch.  Each exception is guaranteed to be unique,
     * i.e. not a subclass of any of the other exceptions in the list,
     * so the catch blocks for these exceptions may be generated in
     * any order relative to each other.
     *
     * RemoteException and RuntimeException are each automatically
     * placed in the returned list (unless any of their superclasses
     * are already present), since those exceptions should always be
     * directly rethrown by a stub method.
     *
     * The returned list will be empty if java.lang.Exception or one
     * of its superclasses is in the throws clause of the method,
     * indicating that no exceptions need to be caught.
     **/
    private List<ClassDoc> computeUniqueCatchList(ClassDoc[] exceptions) {
	List<ClassDoc> uniqueList = new ArrayList<ClassDoc>();

	uniqueList.add(env.docRuntimeException());
	uniqueList.add(env.docRemoteException()); // always catch/rethrow these

	/* For each exception declared by the stub method's throws clause: */
    nextException:
	for (ClassDoc ex : exceptions) {
	    if (env.docException().subclassOf(ex)) {
		/*
		 * If java.lang.Exception (or a superclass) was declared
		 * in the throws clause of this stub method, then we don't
		 * have to bother catching anything; clear the list and
		 * return.
		 */
		uniqueList.clear();
		break;
	    } else if (!ex.subclassOf(env.docException())) {
		/*
		 * Ignore other Throwables that do not extend Exception,
		 * because they cannot be thrown by the invoke methods.
		 */
		continue;
	    }
	    /*
	     * Compare this exception against the current list of
	     * exceptions that need to be caught:
	     */
	    for (Iterator<ClassDoc> i = uniqueList.iterator(); i.hasNext();) {
		ClassDoc ex2 = i.next();
		if (ex.subclassOf(ex2)) {
		    /*
		     * If a superclass of this exception is already on
		     * the list to catch, then ignore this one and continue;
		     */
		    continue nextException;
		} else if (ex2.subclassOf(ex)) {
		    /*
		     * If a subclass of this exception is on the list
		     * to catch, then remove it;
		     */
		    i.remove();
		}
	    }
	    /* This exception is unique: add it to the list to catch. */
	    uniqueList.add(ex);
	}
	return uniqueList;
    }

    /**
     * Writes the skeleton for the remote class to a stream.
     **/
    void writeSkeleton(IndentingWriter p) throws IOException {
	if (version == StubVersion.V1_2) {
	    throw new AssertionError(
		"should not generate skeleton for version " + version);
	}

	/*
	 * Write boiler plate comment.
	 */
	p.pln("// Skeleton class generated by rmic, do not edit.");
	p.pln("// Contents subject to change without notice.");
	p.pln();

	/*
	 * If remote implementation class was in a particular package,
	 * declare the skeleton class to be in the same package.
	 */
	if (!packageName.equals("")) {
	    p.pln("package " + packageName + ";");
	    p.pln();
	}

	/*
	 * Declare the skeleton class.
	 */
	p.plnI("public final class " + skeletonClassSimpleName);
	p.pln("implements " + SKELETON);
	p.pOlnI("{");

	writeOperationsArray(p);
	p.pln();

	writeInterfaceHash(p);
	p.pln();

	/*
	 * Define the getOperations() method.
	 */
	p.plnI("public " + OPERATION + "[] getOperations() {");
	p.pln("return (" + OPERATION + "[]) operations.clone();");
	p.pOln("}");
	p.pln();

	/*
	 * Define the dispatch() method.
	 */
	p.plnI("public void dispatch(" + REMOTE + " obj, " +
	    REMOTE_CALL + " call, int opnum, long hash)");
	p.pln("throws java.lang.Exception");
	p.pOlnI("{");

	if (version == StubVersion.VCOMPAT) {
	    p.plnI("if (opnum < 0) {");
	    if (remoteMethods.length > 0) {
		for (int opnum = 0; opnum < remoteMethods.length; opnum++) {
		    if (opnum > 0)
			p.pO("} else ");
		    p.plnI("if (hash == " +
			remoteMethods[opnum].methodHash() + "L) {");
		    p.pln("opnum = " + opnum + ";");
		}
		p.pOlnI("} else {");
	    }
	    /*
	     * Skeleton throws UnmarshalException if it does not recognize
	     * the method hash; this is what UnicastServerRef.dispatch()
	     * would do.
	     */
	    p.pln("throw new " +
		UNMARSHAL_EXCEPTION + "(\"invalid method hash\");");
	    if (remoteMethods.length > 0) {
		p.pOln("}");
	    }
	    /*
	     * Ignore the validation of the interface hash if the
	     * operation number was negative, since it is really a
	     * method hash instead.
	     */
	    p.pOlnI("} else {");
	}

	p.plnI("if (hash != interfaceHash)");
	p.pln("throw new " +
	    SKELETON_MISMATCH_EXCEPTION + "(\"interface hash mismatch\");");
	p.pO();

	if (version == StubVersion.VCOMPAT) {
	    p.pOln("}");		// end if/else (opnum < 0) block
	}
	p.pln();

	/*
	 * Cast remote object reference to the remote implementation
	 * class, if it's not private.  We don't use the binary name
	 * of the class like previous implementations did because that
	 * would not compile with javac (since 1.4.1).  If the remote
	 * implementation class is private, then we can't cast to it
	 * like previous implementations did because that also would
	 * not compile with javac-- so instead, we'll have to try to
	 * cast to the remote interface for each remote method.
	 */
	if (!remoteClass.classDoc().isPrivate()) {
	    p.pln(remoteClass.classDoc().qualifiedName() + " server = (" +
		  remoteClass.classDoc().qualifiedName() + ") obj;");
	}

	/*
	 * Process call according to the operation number.
	 */
	p.plnI("switch (opnum) {");
	for (int opnum = 0; opnum < remoteMethods.length; opnum++) {
	    writeSkeletonDispatchCase(p, opnum);
	}
	p.pOlnI("default:");
	/*
	 * Skeleton throws UnmarshalException if it does not recognize
	 * the operation number; this is consistent with the case of an
	 * unrecognized method hash.
	 */
	p.pln("throw new " + UNMARSHAL_EXCEPTION +
	    "(\"invalid method number\");");
	p.pOln("}");			// end switch statement
	
	p.pOln("}");			// end dispatch() method

	p.pOln("}");			// end skeleton class
    }

    /**
     * Writes the case block for the skeleton's dispatch method for
     * the remote method with the given "opnum".
     **/
    private void writeSkeletonDispatchCase(IndentingWriter p, int opnum)
	throws IOException
    {
	RemoteClass.Method method = remoteMethods[opnum];
	MethodDoc methodDoc = method.methodDoc();
	String methodName = methodDoc.name();
	Type paramTypes[] = method.parameterTypes();
	String paramNames[] = nameParameters(paramTypes);
	Type returnType = methodDoc.returnType();

	p.pOlnI("case " + opnum + ": // " +
	    Util.getFriendlyUnqualifiedSignature(methodDoc));
	/*
	 * Use nested block statement inside case to provide an independent
	 * namespace for local variables used to unmarshal parameters for
	 * this remote method.
	 */
	p.pOlnI("{");

	if (paramTypes.length > 0) {
	    /*
	     * Declare local variables to hold arguments.
	     */
	    for (int i = 0; i < paramTypes.length; i++) {
		p.pln(paramTypes[i].toString() + " " + paramNames[i] + ";");
	    }

	    /*
	     * Unmarshal arguments from call stream.
	     */
	    p.plnI("try {");
	    p.pln("java.io.ObjectInput in = call.getInputStream();");
	    boolean objectsRead = writeUnmarshalArguments(p, "in",
		paramTypes, paramNames);
	    p.pOlnI("} catch (java.io.IOException e) {");
	    p.pln("throw new " + UNMARSHAL_EXCEPTION +
		"(\"error unmarshalling arguments\", e);");
	    /*
	     * If any only if readObject has been invoked, we must catch
	     * ClassNotFoundException as well as IOException.
	     */
	    if (objectsRead) {
		p.pOlnI("} catch (java.lang.ClassNotFoundException e) {");
		p.pln("throw new " + UNMARSHAL_EXCEPTION +
		    "(\"error unmarshalling arguments\", e);");
	    }
	    p.pOlnI("} finally {");
	    p.pln("call.releaseInputStream();");
	    p.pOln("}");
	} else {
	    p.pln("call.releaseInputStream();");
	}

	if (!Util.isVoid(returnType)) {
	    /*
	     * Declare variable to hold return type, if not void.
	     */
	    p.p(returnType.toString() + " $result = ");
							// REMIND: why $?
	}

	/*
	 * Invoke the method on the server object.  If the remote
	 * implementation class is private, then we don't have a
	 * reference cast to it, and so we try to cast to the remote
	 * object reference to the method's declaring interface here.
	 */
	String target = remoteClass.classDoc().isPrivate() ?
	    "((" + methodDoc.containingClass().qualifiedName() + ") obj)" :
	    "server";
	p.p(target + "." + methodName + "(");
	for (int i = 0; i < paramNames.length; i++) {
	    if (i > 0)
		p.p(", ");
	    p.p(paramNames[i]);
	}
	p.pln(");");

	/*
	 * Always invoke getResultStream(true) on the call object to send
	 * the indication of a successful invocation to the caller.  If
	 * the return type is not void, keep the result stream and marshal
	 * the return value.
	 */
	p.plnI("try {");
	if (!Util.isVoid(returnType)) {
	    p.p("java.io.ObjectOutput out = ");
	}
	p.pln("call.getResultStream(true);");
	if (!Util.isVoid(returnType)) {
	    writeMarshalArgument(p, "out", returnType, "$result");
	    p.pln(";");
	}
	p.pOlnI("} catch (java.io.IOException e) {");
	p.pln("throw new " +
	    MARSHAL_EXCEPTION + "(\"error marshalling return\", e);");
	p.pOln("}");
	
	p.pln("break;");		// break from switch statement

	p.pOlnI("}");			// end nested block statement
	p.pln();
    }

    /**
     * Writes declaration and initializer for "operations" static array.
     **/
    private void writeOperationsArray(IndentingWriter p)
	throws IOException
    {
	p.plnI("private static final " + OPERATION + "[] operations = {");
	for (int i = 0; i < remoteMethods.length; i++) {
	    if (i > 0)
		p.pln(",");
	    p.p("new " + OPERATION + "(\"" +
		remoteMethods[i].operationString() + "\")");
	}
	p.pln();
	p.pOln("};");
    }

    /**
     * Writes declaration and initializer for "interfaceHash" static field.
     **/
    private void writeInterfaceHash(IndentingWriter p)
	throws IOException
    {
	p.pln("private static final long interfaceHash = " +
	    remoteClass.interfaceHash() + "L;");
    }

    /**
     * Writes declaration for java.lang.reflect.Method static fields
     * corresponding to each remote method in a stub.
     **/
    private void writeMethodFieldDeclarations(IndentingWriter p)
	throws IOException
    {
	for (String name : methodFieldNames) {
	    p.pln("private static java.lang.reflect.Method " + name + ";");
	}
    }

    /**
     * Writes code to initialize the static fields for each method
     * using the Java Reflection API.
     **/
    private void writeMethodFieldInitializers(IndentingWriter p)
	throws IOException
    {
	for (int i = 0; i < methodFieldNames.length; i++) {
	    p.p(methodFieldNames[i] + " = ");
	    /*
	     * Look up the Method object in the somewhat arbitrary
	     * interface that we find in the Method object.
	     */
	    RemoteClass.Method method = remoteMethods[i];
	    MethodDoc methodDoc = method.methodDoc();
	    String methodName = methodDoc.name();
	    Type paramTypes[] = method.parameterTypes();

	    p.p(methodDoc.containingClass().qualifiedName() + ".class.getMethod(\"" +
		methodName + "\", new java.lang.Class[] {");
	    for (int j = 0; j < paramTypes.length; j++) {
		if (j > 0)
		    p.p(", ");
		p.p(paramTypes[j].toString() + ".class");
	    }
	    p.pln("});");
	}
    }


    /*
     * Following are a series of static utility methods useful during
     * the code generation process:
     */

    /**
     * Generates an array of names for fields correspondins to the
     * given array of remote methods.  Each name in the returned array
     * is guaranteed to be unique.
     *
     * The name of a method is included in its corresponding field
     * name to enhance readability of the generated code.
     **/
    private static String[] nameMethodFields(RemoteClass.Method[] methods) {
	String[] names = new String[methods.length];
	for (int i = 0; i < names.length; i++) {
	    names[i] = "$method_" + methods[i].methodDoc().name() + "_" + i;
	}
	return names;
    }

    /**
     * Generates an array of names for parameters corresponding to the
     * given array of types for the parameters.  Each name in the
     * returned array is guaranteed to be unique.
     *
     * A representation of the type of a parameter is included in its
     * corresponding parameter name to enhance the readability of the
     * generated code.
     **/
    private static String[] nameParameters(Type[] types) {
	String[] names = new String[types.length];
	for (int i = 0; i < names.length; i++) {
	    names[i] = "$param_" +
		generateNameFromType(types[i]) + "_" + (i + 1);
	}
	return names;
    }

    /**
     * Generates a readable string representing the given type
     * suitable for embedding within a Java identifier.
     **/
    private static String generateNameFromType(Type type) {
	String name = type.typeName().replace('.', '$');
	int dimensions = type.dimension().length() / 2;
	for (int i = 0; i < dimensions; i++) {
	    name = "arrayOf_" + name;
	}
	return name;
    }

    /**
     * Writes a snippet of Java code to marshal a value named "name"
     * of type "type" to the java.io.ObjectOutput stream named
     * "stream".
     *
     * Primitive types are marshalled with their corresponding methods
     * in the java.io.DataOutput interface, and objects (including
     * arrays) are marshalled using the writeObject method.
     **/
    private static void writeMarshalArgument(IndentingWriter p,
					     String streamName,
					     Type type, String name)
	throws IOException
    {
	if (type.dimension().length() > 0 || type.asClassDoc() != null) {
	    p.p(streamName + ".writeObject(" + name + ")");
	} else if (type.typeName().equals("boolean")) {
	    p.p(streamName + ".writeBoolean(" + name + ")");
	} else if (type.typeName().equals("byte")) {
	    p.p(streamName + ".writeByte(" + name + ")");
	} else if (type.typeName().equals("char")) {
	    p.p(streamName + ".writeChar(" + name + ")");
	} else if (type.typeName().equals("short")) {
	    p.p(streamName + ".writeShort(" + name + ")");
	} else if (type.typeName().equals("int")) {
	    p.p(streamName + ".writeInt(" + name + ")");
	} else if (type.typeName().equals("long")) {
	    p.p(streamName + ".writeLong(" + name + ")");
	} else if (type.typeName().equals("float")) {
	    p.p(streamName + ".writeFloat(" + name + ")");
	} else if (type.typeName().equals("double")) {
	    p.p(streamName + ".writeDouble(" + name + ")");
	} else {
	    throw new AssertionError(type);
	}
    }

    /**
     * Writes Java statements to marshal a series of values in order
     * as named in the "names" array, with types as specified in the
     * "types" array, to the java.io.ObjectOutput stream named
     * "stream".
     **/
    private static void writeMarshalArguments(IndentingWriter p,
					      String streamName,
					      Type[] types, String[] names)
	throws IOException
    {
	assert types.length == names.length;

	for (int i = 0; i < types.length; i++) {
	    writeMarshalArgument(p, streamName, types[i], names[i]);
	    p.pln(";");
	}
    }

    /**
     * Writes a snippet of Java code to unmarshal a value of type
     * "type" from the java.io.ObjectInput stream named "stream" into
     * a variable named "name" (if "name" is null, the value is
     * unmarshalled and discarded).
     *
     * Primitive types are unmarshalled with their corresponding
     * methods in the java.io.DataInput interface, and objects
     * (including arrays) are unmarshalled using the readObject
     * method.
     *
     * Returns true if code to invoke readObject was written, and
     * false otherwise.
     **/
    private static boolean writeUnmarshalArgument(IndentingWriter p,
						  String streamName,
						  Type type, String name)
	throws IOException
    {
	boolean readObject = false;

	if (name != null) {
	    p.p(name + " = ");
	}

	if (type.dimension().length() > 0 || type.asClassDoc() != null) {
	    p.p("(" + type.toString() + ") " + streamName + ".readObject()");
	    readObject = true;
	} else if (type.typeName().equals("boolean")) {
	    p.p(streamName + ".readBoolean()");
	} else if (type.typeName().equals("byte")) {
	    p.p(streamName + ".readByte()");
	} else if (type.typeName().equals("char")) {
	    p.p(streamName + ".readChar()");
	} else if (type.typeName().equals("short")) {
	    p.p(streamName + ".readShort()");
	} else if (type.typeName().equals("int")) {
	    p.p(streamName + ".readInt()");
	} else if (type.typeName().equals("long")) {
	    p.p(streamName + ".readLong()");
	} else if (type.typeName().equals("float")) {
	    p.p(streamName + ".readFloat()");
	} else if (type.typeName().equals("double")) {
	    p.p(streamName + ".readDouble()");
	} else {
	    throw new AssertionError(type);
	}

	return readObject;
    }

    /**
     * Writes Java statements to unmarshal a series of values in order
     * of types as in the "types" array from the java.io.ObjectInput
     * stream named "stream" into variables as named in "names" (for
     * any element of "names" that is null, the corresponding value is
     * unmarshalled and discarded).
     **/
    private static boolean writeUnmarshalArguments(IndentingWriter p,
						   String streamName,
						   Type[] types,
						   String[] names)
	throws IOException
    {
	assert types.length == names.length;

	boolean readObject = false;
	for (int i = 0; i < types.length; i++) {
	    if (writeUnmarshalArgument(p, streamName, types[i], names[i])) {
		readObject = true;
	    }
	    p.pln(";");
	}
	return readObject;
    }

    /**
     * Returns a snippet of Java code to wrap a value named "name" of
     * type "type" into an object as appropriate for use by the Java
     * Reflection API.
     *
     * For primitive types, an appropriate wrapper class is
     * instantiated with the primitive value.  For object types
     * (including arrays), no wrapping is necessary, so the value is
     * named directly.
     **/
    private static String wrapArgumentCode(Type type, String name) {
	if (type.dimension().length() > 0 || type.asClassDoc() != null) {
	    return name;
	} else if (type.typeName().equals("boolean")) {
	    return ("(" + name +
		    " ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE)");
	} else if (type.typeName().equals("byte")) {
	    return "new java.lang.Byte(" + name + ")";
	} else if (type.typeName().equals("char")) {
	    return "new java.lang.Character(" + name + ")";
	} else if (type.typeName().equals("short")) {
	    return "new java.lang.Short(" + name + ")";
	} else if (type.typeName().equals("int")) {
	    return "new java.lang.Integer(" + name + ")";
	} else if (type.typeName().equals("long")) {
	    return "new java.lang.Long(" + name + ")";
	} else if (type.typeName().equals("float")) {
	    return "new java.lang.Float(" + name + ")";
	} else if (type.typeName().equals("double")) {
	    return "new java.lang.Double(" + name + ")";
	} else {
	    throw new AssertionError(type);
	}
    }

    /**
     * Returns a snippet of Java code to unwrap a value named "name"
     * into a value of type "type", as appropriate for the Java
     * Reflection API.
     *
     * For primitive types, the value is assumed to be of the
     * corresponding wrapper class, and a method is called on the
     * wrapper to retrieve the primitive value.  For object types
     * (include arrays), no unwrapping is necessary; the value is
     * simply cast to the expected real object type.
     **/
    private static String unwrapArgumentCode(Type type, String name) {
	if (type.dimension().length() > 0 || type.asClassDoc() != null) {
	    return "((" + type.toString() + ") " + name + ")";
	} else if (type.typeName().equals("boolean")) {
	    return "((java.lang.Boolean) " + name + ").booleanValue()";
	} else if (type.typeName().equals("byte")) {
	    return "((java.lang.Byte) " + name + ").byteValue()";
	} else if (type.typeName().equals("char")) {
	    return "((java.lang.Character) " + name + ").charValue()";
	} else if (type.typeName().equals("short")) {
	    return "((java.lang.Short) " + name + ").shortValue()";
	} else if (type.typeName().equals("int")) {
	    return "((java.lang.Integer) " + name + ").intValue()";
	} else if (type.typeName().equals("long")) {
	    return "((java.lang.Long) " + name + ").longValue()";
	} else if (type.typeName().equals("float")) {
	    return "((java.lang.Float) " + name + ").floatValue()";
	} else if (type.typeName().equals("double")) {
	    return "((java.lang.Double) " + name + ").doubleValue()";
	} else {
	    throw new AssertionError(type);
	}
    }
}
