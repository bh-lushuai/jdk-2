/*
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
 *
 */

/*
 * @(#)GlyphSubstitutionTables.h	1.18 06/12/13
 *
 * (C) Copyright IBM Corp. 1998-2005 - All Rights Reserved
 *
 */

#ifndef __GLYPHSUBSTITUTIONTABLES_H
#define __GLYPHSUBSTITUTIONTABLES_H

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "GlyphLookupTables.h"

class  LEGlyphStorage;
class  LEGlyphFilter;
struct GlyphDefinitionTableHeader;

struct GlyphSubstitutionTableHeader : public GlyphLookupTableHeader
{
    le_int32 process(LEGlyphStorage &glyphStorage,
        le_bool rightToLeft, LETag scriptTag, LETag languageTag,
        const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
        const LEGlyphFilter *filter, const FeatureMap *featureMap, 
        le_int32 featureMapCount, le_bool featureOrder) const;
};

enum GlyphSubstitutionSubtableTypes
{
    gsstSingle          = 1,
    gsstMultiple        = 2,
    gsstAlternate       = 3,
    gsstLigature        = 4,
    gsstContext         = 5,
    gsstChainingContext = 6,
    gsstExtension       = 7,
    gsstReverseChaining = 8
};

typedef LookupSubtable GlyphSubstitutionSubtable;

#endif
