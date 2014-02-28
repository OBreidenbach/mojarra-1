/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.sun.faces.el.impl;

import java.util.List;
import java.util.Map;

/**
 * <p>This is a test bean with a set of properties
 *
 * @author Nathan Abramson - Art Technology Group
 */

public class Bean1 {

    //-------------------------------------
    // Properties
    //-------------------------------------
    // property boolean1

    boolean mBoolean1;


    public boolean getBoolean1() {
        return mBoolean1;
    }


    public void setBoolean1(boolean pBoolean1) {
        mBoolean1 = pBoolean1;
    }

    //-------------------------------------
    // property byte1

    byte mByte1;


    public byte getByte1() {
        return mByte1;
    }


    public void setByte1(byte pByte1) {
        mByte1 = pByte1;
    }

    //-------------------------------------
    // property char1

    char mChar1;


    public char getChar1() {
        return mChar1;
    }


    public void setChar1(char pChar1) {
        mChar1 = pChar1;
    }

    //-------------------------------------
    // property short1

    short mShort1;


    public short getShort1() {
        return mShort1;
    }


    public void setShort1(short pShort1) {
        mShort1 = pShort1;
    }

    //-------------------------------------
    // property int1

    int mInt1;


    public int getInt1() {
        return mInt1;
    }


    public void setInt1(int pInt1) {
        mInt1 = pInt1;
    }

    //-------------------------------------
    // property long1

    long mLong1;


    public long getLong1() {
        return mLong1;
    }


    public void setLong1(long pLong1) {
        mLong1 = pLong1;
    }

    //-------------------------------------
    // property float1

    float mFloat1;


    public float getFloat1() {
        return mFloat1;
    }


    public void setFloat1(float pFloat1) {
        mFloat1 = pFloat1;
    }

    //-------------------------------------
    // property double1

    double mDouble1;


    public double getDouble1() {
        return mDouble1;
    }


    public void setDouble1(double pDouble1) {
        mDouble1 = pDouble1;
    }

    //-------------------------------------
    // property boolean2

    Boolean mBoolean2;


    public Boolean getBoolean2() {
        return mBoolean2;
    }


    public void setBoolean2(Boolean pBoolean2) {
        mBoolean2 = pBoolean2;
    }

    //-------------------------------------
    // property byte2

    Byte mByte2;


    public Byte getByte2() {
        return mByte2;
    }


    public void setByte2(Byte pByte2) {
        mByte2 = pByte2;
    }

    //-------------------------------------
    // property char2

    Character mChar2;


    public Character getChar2() {
        return mChar2;
    }


    public void setChar2(Character pChar2) {
        mChar2 = pChar2;
    }

    //-------------------------------------
    // property short2

    Short mShort2;


    public Short getShort2() {
        return mShort2;
    }


    public void setShort2(Short pShort2) {
        mShort2 = pShort2;
    }

    //-------------------------------------
    // property int2

    Integer mInt2;


    public Integer getInt2() {
        return mInt2;
    }


    public void setInt2(Integer pInt2) {
        mInt2 = pInt2;
    }

    //-------------------------------------
    // property long2

    Long mLong2;


    public Long getLong2() {
        return mLong2;
    }


    public void setLong2(Long pLong2) {
        mLong2 = pLong2;
    }

    //-------------------------------------
    // property float2

    Float mFloat2;


    public Float getFloat2() {
        return mFloat2;
    }


    public void setFloat2(Float pFloat2) {
        mFloat2 = pFloat2;
    }

    //-------------------------------------
    // property double2

    Double mDouble2;


    public Double getDouble2() {
        return mDouble2;
    }


    public void setDouble2(Double pDouble2) {
        mDouble2 = pDouble2;
    }

    //-------------------------------------
    // property string1

    String mString1;


    public String getString1() {
        return mString1;
    }


    public void setString1(String pString1) {
        mString1 = pString1;
    }

    //-------------------------------------
    // property string2

    String mString2;


    public String getString2() {
        return mString2;
    }


    public void setString2(String pString2) {
        mString2 = pString2;
    }

    //-------------------------------------
    // property bean1

    Bean1 mBean1;


    public Bean1 getBean1() {
        return mBean1;
    }


    public void setBean1(Bean1 pBean1) {
        mBean1 = pBean1;
    }

    //-------------------------------------
    // property bean2

    Bean1 mBean2;


    public Bean1 getBean2() {
        return mBean2;
    }


    public void setBean2(Bean1 pBean2) {
        mBean2 = pBean2;
    }

    //-------------------------------------
    // property noGetter

    String mNoGetter;


    public void setNoGetter(String pNoGetter) {
        mNoGetter = pNoGetter;
    }

    //-------------------------------------
    // property errorInGetter

    String mErrorInGetter;


    public String getErrorInGetter() {
        throw new NullPointerException("Error!");
    }

    //-------------------------------------
    // property stringArray1

    String[] mStringArray1;


    public String[] getStringArray1() {
        return mStringArray1;
    }


    public void setStringArray1(String[] pStringArray1) {
        mStringArray1 = pStringArray1;
    }

    //-------------------------------------
    // property list1

    List mList1;


    public List getList1() {
        return mList1;
    }


    public void setList1(List pList1) {
        mList1 = pList1;
    }

    //-------------------------------------
    // property map1

    Map mMap1;


    public Map getMap1() {
        return mMap1;
    }


    public void setMap1(Map pMap1) {
        mMap1 = pMap1;
    }

    //-------------------------------------
    // property indexed1

    public String getIndexed1(int pIndex) {
        return mStringArray1[pIndex];
    }

    //-------------------------------------
    // Member variables
    //-------------------------------------

    //-------------------------------------
    /**
     * Constructor
     */
    public Bean1() {
    }

    //-------------------------------------

}
