/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package verserv.provider;

import java.lang.ModuleInfo.*;
import java.module.annotation.*;

@Version("1.0")
@ServiceProviders({
    @ServiceProvider(service="verserv.service.FooService",
        providerClass="verserv.provider.FooService2Provider"),
    @ServiceProvider(service="verserv.service.BarService",
        providerClass="verserv.provider.BarServiceDefaultProvider")
})
@Services("BarProvider")
// It is important that this be on a single line; see ServiceTest.redefineAnnotations
@ImportModules({ @ImportModule(name="java.se.core"), @ImportModule(name="verserv.service", version="[1.0, 2.0)") })
class module_info {
    // Export service type
     exports verserv$provider$BarService;

    // Export service provider type
    exports verserv$provider$FooService2Provider;

    // Note that the default service provider is *not* exported.
}
