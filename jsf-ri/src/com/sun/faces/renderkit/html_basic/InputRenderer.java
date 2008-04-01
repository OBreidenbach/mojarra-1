/*
 * $Id: InputRenderer.java,v 1.18 2002/05/31 19:34:14 jvisvanathan Exp $
 */

/*
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// InputRenderer.java

package com.sun.faces.renderkit.html_basic;

import java.util.Iterator;

import javax.faces.component.AttributeDescriptor;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;
import javax.faces.component.UIComponent;
import javax.faces.FacesException;

import org.mozilla.util.Assert;
import org.mozilla.util.Debug;
import org.mozilla.util.Log;
import org.mozilla.util.ParameterCheck;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.ConversionException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.PrintWriter;
import java.io.IOException;

/**
 *
 *  <B>InputRenderer</B> is a class ...
 *
 * <B>Lifetime And Scope</B> <P>
 *
 * @version $Id: InputRenderer.java,v 1.18 2002/05/31 19:34:14 jvisvanathan Exp $
 * 
 * @see	Blah
 * @see	Bloo
 *
 */

public class InputRenderer extends Renderer {
    //
    // Protected Constants
    //

    //
    // Class Variables
    //

    //
    // Instance Variables
    //

    // Attribute Instance Variables


    // Relationship Instance Variables

    //
    // Constructors and Initializers    
    //

    public InputRenderer() {
        super();
    }

    //
    // Class methods
    //

    //
    // General Methods
    //

    //
    // Methods From Renderer
    //
    public AttributeDescriptor getAttributeDescriptor(
        UIComponent component, String name) {
        return null;
    }

    public AttributeDescriptor getAttributeDescriptor(
        String componentType, String name) {
        return null;
    }

    public Iterator getAttributeNames(UIComponent component) {
        return null;
    }

    public Iterator getAttributeNames(String componentType) {
        return null;
    }

    public boolean supportsComponentType(UIComponent c) {
        return false;
    }

    public boolean supportsComponentType(String componentType) {
        return false;
    }

    public void decode(FacesContext context, UIComponent component) {
        Object convertedValue = null;
        Class modelType = null;
        
        ParameterCheck.nonNull(context);
        ParameterCheck.nonNull(component);
        
        // PENDING (visvan) should we call supportsType to double check
        // compoenentType ??
        String compoundId = component.getCompoundId();
        Assert.assert_it(compoundId != null );
        
        String newValue = context.getServletRequest().getParameter(compoundId);
        String modelRef = component.getModel();
        
        // If modelReference String is null or newValue is null, type
        // conversion is not necessary. This is because default type
        // for UITextEntry component is String. Simply set local value.
        if ( newValue == null || modelRef == null ) {
            component.setValue(newValue);
            return;
        }
        
        // if we get here, type conversion is required.
        try {
            modelType = context.getModelType(modelRef);
        } catch (FacesException fe ) {
            // PENDING (visvan) log error
        }    
        Assert.assert_it(modelType != null );
        
        try {
            convertedValue = ConvertUtils.convert(newValue, modelType);
        } catch (ConversionException ce ) {
            //PENDING (visvan) add error message to messageList
        }    
            
        if ( convertedValue == null ) {
            // since conversion failed, don't modify the localValue.
            // set the value temporarily in an attribute so that encode can 
            // use this local state instead of local value.
            // PENDING (visvan) confirm with Craig ??
            component.setAttribute("localState", newValue);
        } else {
            // conversion successful, set converted value as the local value.
            component.setValue(convertedValue);    
        }    
        
    }

    public void encodeBegin(FacesContext context, UIComponent component) {
        String currentValue = null;
        PrintWriter writer = null;
        
        ParameterCheck.nonNull(context);
        ParameterCheck.nonNull(component);
        
        // if localState attribute is set, then conversion failed, so use
        // that to reproduce the incorrect value. Otherwise use the current value
        // stored in component.
        Object localState = component.getAttribute("localState");
        if ( localState != null ) {
            currentValue = (String) localState;
        } else {
            Object currentObj = component.currentValue(context);
            if ( currentObj != null) {
                currentValue = ConvertUtils.convert(currentObj);
            }    
        }    
        try {
            writer = context.getServletResponse().getWriter();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        Assert.assert_it(writer != null );
        writer.print("<INPUT TYPE=\"text\"");
        writer.print(" NAME=\"");
        writer.print(component.getCompoundId());
        writer.print("\"");

        // render default text specified
        if ( currentValue != null ) {
            writer.print(" VALUE=\"");
            writer.print(currentValue);
            writer.print("\"");
        }
        //render size if specified
        String textField_size = (String)component.getAttribute("size");
        if ( textField_size != null ) {
            writer.print(" SIZE=\"");
            writer.print(textField_size);
            writer.print("\"");
        }
        //render maxlength if specified
        String textField_ml = (String)component.getAttribute("maxlength");
        if ( textField_ml != null ) {
            writer.print(" MAXLENGTH=\"");
            writer.print(textField_ml);
            writer.print("\"");
        }
        writer.print(">");
    }

    public void encodeChildren(FacesContext context, UIComponent component) {

    }

    public void encodeEnd(FacesContext context, UIComponent component) {

    }
    
    // The testcase for this class is TestRenderers_1.java 

} // end of class InputRenderer


