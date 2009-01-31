/*
 * Copyright 2006-2007 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.security.ssl;

import java.io.IOException;
import java.io.PrintStream;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;

/**
 * ClientKeyExchange message for all ECDH based key exchange methods. It
 * contains the client's ephemeral public value.
 *
 * @version %I%, %G%
 * @since   1.6
 * @author  Andreas Sterbenz
 */
final class ECDHClientKeyExchange extends HandshakeMessage {

    int messageType() {
        return ht_client_key_exchange;
    }

    private byte[] encodedPoint;

    byte[] getEncodedPoint() {
	return encodedPoint;
    }

    // Called by the client with its ephemeral public key.
    ECDHClientKeyExchange(PublicKey publicKey) {
	ECPublicKey ecKey = (ECPublicKey)publicKey;
	ECPoint point = ecKey.getW();
	ECParameterSpec params = ecKey.getParams();
	encodedPoint = JsseJce.encodePoint(point, params.getCurve());
    }

    ECDHClientKeyExchange(HandshakeInStream input) throws IOException {
        encodedPoint = input.getBytes8();
    }

    int messageLength() {
	return encodedPoint.length + 1;
    }

    void send(HandshakeOutStream s) throws IOException {
	s.putBytes8(encodedPoint);
    }

    void print(PrintStream s) throws IOException {
        s.println("*** ECDHClientKeyExchange");

	if (debug != null && Debug.isOn("verbose")) {
	    Debug.println(s, "ECDH Public value", encodedPoint);
	}
    }
}
