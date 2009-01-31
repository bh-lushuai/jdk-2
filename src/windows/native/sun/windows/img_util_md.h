/*
 * Copyright 1996-2001 Sun Microsystems, Inc.  All Rights Reserved.
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

#include "windows.h"

#ifdef __cplusplus
extern "C" {
#include "colordata.h"
}
#else
#include "colordata.h"
#endif

#ifdef __cplusplus
extern "C" {
#endif


typedef unsigned char MaskBits;

extern uns_ordered_dither_array img_oda_alpha;

#define BufComplete(cvdata, dstX1, dstY1, dstX2, dstY2)		\
    (((AwtImage *)cvdata)->BufDone(dstX1, dstY1, dstX2, dstY2))

#define SendRow(cvdata, dstY, dstX1, dstX2)

#define ImgInitMask(cvdata, x1, y1, x2, y2)			\
    (((AwtImage *) cvdata)->GetMaskBuf(TRUE, x1, y1, x2, y2))

#define ScanBytes(cvdata)	(((AwtImage *) cvdata)->GetBufScan())

#define MaskScan(cvdata)					\
    MaskOffset((((AwtImage *) cvdata)->GetWidth() + 31) & (~31))

#define MaskOffset(x)		((x) >> 3)

#define MaskInit(x)		(0x80 >> ((x) & 7))

#define SetOpaqueBit(mask, bit)		((mask) &= ~(bit))
#define SetTransparentBit(mask, bit)	((mask) |= (bit))

#define ColorCubeFSMap(r, g, b)		AwtImage::CubeMap(r, g, b)

#define ColorCubeOrdMapSgn(r, g, b)	AwtImage::CubeMap(r, g, b);

#define GetPixelRGB(pixel, red, green, blue)			\
    do {							\
	RGBQUAD *cp = AwtImage::PixelColor(pixel);		\
	red = cp->rgbRed;					\
	green = cp->rgbGreen;					\
	blue = cp->rgbBlue;					\
    } while (0)

#ifdef DEBUG
#undef img_check
#define img_check(condition)					\
    do {							\
	if (!(condition)) {					\
	    SignalError(0, JAVAPKG "InternalError",		\
			"assertion failed:  " #condition);	\
	    return SCALEFAILURE;				\
	}							\
    } while (0)
#else /* DEBUG */
#define img_check(condition)	do {} while (0)
#endif /* DEBUG */

void color_init();
extern const char *cubemapArray;
#define CUBEMAP(r,g,b) \
    ((dstLockInfo.inv_cmap)[(((r)>>3)<<10) | (((g)>>3)<<5) | ((b)>>3)])

extern void freeICMColorData(ColorData *pData);
extern void initInverseGrayLut(int* prgb, int rgbsize, ColorData* cData);
extern unsigned char* initCubemap(int* cmap, int cmap_len, int cube_dim);
extern void initDitherTables(ColorData* cData);

#define SET_CUBEMAPARRAY \
    if (lockInfo->lockedLut) { \
        lockInfo->inv_cmap = (const char *)cubemapArray; \
    } else { \
        lockInfo->inv_cmap = (const char*)lockInfo->colorData->img_clr_tbl; \
    }


#ifdef __cplusplus
}; /* end of extern "C" */
#endif
