/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.1.2.1</a>, using an XML
 * Schema.
 * $Id$
 */

package org.opennms.netmgt.config.wmi;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

/**
 * WMI object groups
 * 
 * @version $Revision$ $Date$
 */

@SuppressWarnings("all") public class Wpms implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * A WMI Object Group
     *  
     */
    private java.util.List<org.opennms.netmgt.config.wmi.Wpm> _wpmList;


      //----------------/
     //- Constructors -/
    //----------------/

    public Wpms() {
        super();
        this._wpmList = new java.util.ArrayList<org.opennms.netmgt.config.wmi.Wpm>();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * 
     * 
     * @param vWpm
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addWpm(
            final org.opennms.netmgt.config.wmi.Wpm vWpm)
    throws java.lang.IndexOutOfBoundsException {
        this._wpmList.add(vWpm);
    }

    /**
     * 
     * 
     * @param index
     * @param vWpm
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addWpm(
            final int index,
            final org.opennms.netmgt.config.wmi.Wpm vWpm)
    throws java.lang.IndexOutOfBoundsException {
        this._wpmList.add(index, vWpm);
    }

    /**
     * Method enumerateWpm.
     * 
     * @return an Enumeration over all possible elements of this
     * collection
     */
    public java.util.Enumeration<org.opennms.netmgt.config.wmi.Wpm> enumerateWpm(
    ) {
        return java.util.Collections.enumeration(this._wpmList);
    }

    /**
     * Overrides the java.lang.Object.equals method.
     * 
     * @param obj
     * @return true if the objects are equal.
     */
    @Override()
    public boolean equals(
            final java.lang.Object obj) {
        if ( this == obj )
            return true;
        
        if (obj instanceof Wpms) {
        
            Wpms temp = (Wpms)obj;
            if (this._wpmList != null) {
                if (temp._wpmList == null) return false;
                else if (!(this._wpmList.equals(temp._wpmList))) 
                    return false;
            }
            else if (temp._wpmList != null)
                return false;
            return true;
        }
        return false;
    }

    /**
     * Method getWpm.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the org.opennms.netmgt.config.wmi.Wpm
     * at the given index
     */
    public org.opennms.netmgt.config.wmi.Wpm getWpm(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._wpmList.size()) {
            throw new IndexOutOfBoundsException("getWpm: Index value '" + index + "' not in range [0.." + (this._wpmList.size() - 1) + "]");
        }
        
        return (org.opennms.netmgt.config.wmi.Wpm) _wpmList.get(index);
    }

    /**
     * Method getWpm.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public org.opennms.netmgt.config.wmi.Wpm[] getWpm(
    ) {
        org.opennms.netmgt.config.wmi.Wpm[] array = new org.opennms.netmgt.config.wmi.Wpm[0];
        return (org.opennms.netmgt.config.wmi.Wpm[]) this._wpmList.toArray(array);
    }

    /**
     * Method getWpmCollection.Returns a reference to '_wpmList'.
     * No type checking is performed on any modifications to the
     * Vector.
     * 
     * @return a reference to the Vector backing this class
     */
    public java.util.List<org.opennms.netmgt.config.wmi.Wpm> getWpmCollection(
    ) {
        return this._wpmList;
    }

    /**
     * Method getWpmCount.
     * 
     * @return the size of this collection
     */
    public int getWpmCount(
    ) {
        return this._wpmList.size();
    }

    /**
     * Overrides the java.lang.Object.hashCode method.
     * <p>
     * The following steps came from <b>Effective Java Programming
     * Language Guide</b> by Joshua Bloch, Chapter 3
     * 
     * @return a hash code value for the object.
     */
    public int hashCode(
    ) {
        int result = 17;
        
        long tmp;
        if (_wpmList != null) {
           result = 37 * result + _wpmList.hashCode();
        }
        
        return result;
    }

    /**
     * Method isValid.
     * 
     * @return true if this object is valid according to the schema
     */
    public boolean isValid(
    ) {
        try {
            validate();
        } catch (org.exolab.castor.xml.ValidationException vex) {
            return false;
        }
        return true;
    }

    /**
     * Method iterateWpm.
     * 
     * @return an Iterator over all possible elements in this
     * collection
     */
    public java.util.Iterator<org.opennms.netmgt.config.wmi.Wpm> iterateWpm(
    ) {
        return this._wpmList.iterator();
    }

    /**
     * 
     * 
     * @param out
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void marshal(
            final java.io.Writer out)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, out);
    }

    /**
     * 
     * 
     * @param handler
     * @throws java.io.IOException if an IOException occurs during
     * marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     */
    public void marshal(
            final org.xml.sax.ContentHandler handler)
    throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, handler);
    }

    /**
     */
    public void removeAllWpm(
    ) {
        this._wpmList.clear();
    }

    /**
     * Method removeWpm.
     * 
     * @param vWpm
     * @return true if the object was removed from the collection.
     */
    public boolean removeWpm(
            final org.opennms.netmgt.config.wmi.Wpm vWpm) {
        boolean removed = _wpmList.remove(vWpm);
        return removed;
    }

    /**
     * Method removeWpmAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public org.opennms.netmgt.config.wmi.Wpm removeWpmAt(
            final int index) {
        java.lang.Object obj = this._wpmList.remove(index);
        return (org.opennms.netmgt.config.wmi.Wpm) obj;
    }

    /**
     * 
     * 
     * @param index
     * @param vWpm
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setWpm(
            final int index,
            final org.opennms.netmgt.config.wmi.Wpm vWpm)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._wpmList.size()) {
            throw new IndexOutOfBoundsException("setWpm: Index value '" + index + "' not in range [0.." + (this._wpmList.size() - 1) + "]");
        }
        
        this._wpmList.set(index, vWpm);
    }

    /**
     * 
     * 
     * @param vWpmArray
     */
    public void setWpm(
            final org.opennms.netmgt.config.wmi.Wpm[] vWpmArray) {
        //-- copy array
        _wpmList.clear();
        
        for (int i = 0; i < vWpmArray.length; i++) {
                this._wpmList.add(vWpmArray[i]);
        }
    }

    /**
     * Sets the value of '_wpmList' by copying the given Vector.
     * All elements will be checked for type safety.
     * 
     * @param vWpmList the Vector to copy.
     */
    public void setWpm(
            final java.util.List<org.opennms.netmgt.config.wmi.Wpm> vWpmList) {
        // copy vector
        this._wpmList.clear();
        
        this._wpmList.addAll(vWpmList);
    }

    /**
     * Sets the value of '_wpmList' by setting it to the given
     * Vector. No type checking is performed.
     * @deprecated
     * 
     * @param wpmList the Vector to set.
     */
    public void setWpmCollection(
            final java.util.List<org.opennms.netmgt.config.wmi.Wpm> wpmList) {
        this._wpmList = wpmList;
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled org.opennms.netmgt.config.wmi.Wpms
     */
    public static org.opennms.netmgt.config.wmi.Wpms unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (org.opennms.netmgt.config.wmi.Wpms) Unmarshaller.unmarshal(org.opennms.netmgt.config.wmi.Wpms.class, reader);
    }

    /**
     * 
     * 
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void validate(
    )
    throws org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
        validator.validate(this);
    }

}
