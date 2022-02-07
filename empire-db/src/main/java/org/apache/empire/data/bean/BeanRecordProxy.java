/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.data.bean;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.Entity;
import org.apache.empire.data.Record;
import org.apache.empire.exceptions.BeanPropertyGetException;
import org.apache.empire.exceptions.BeanPropertySetException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BeanRecordProxy
 * This class defines proxy that allows any POJO to behave like a record object.
 *  
 * @param <T> the type of the class proxied by this {@code BeanRecordProxy}
 * 
 * @author Rainer
 */
public class BeanRecordProxy<T> implements Record
{
    protected static final Logger log = LoggerFactory.getLogger(BeanRecordProxy.class);
    
    protected final Entity entity;
    protected final List<Column> columns;
    protected final Column[] keyColumns;

    protected T data;
    protected boolean[] modified;

    public BeanRecordProxy(T data, List<Column> columns, Column[] keyColumns, Entity entity)
    {
        this.data = data;
        this.columns = columns;
        this.keyColumns = keyColumns;
        this.entity = entity;
    }

    public BeanRecordProxy(List<Column> columns, Column[] keyColumns, Entity entity)
    {
        this(null, columns, keyColumns, entity);
    }

    public BeanRecordProxy(T data, BeanClass beanClass)
    {
        this(data, 
             ObjectUtils.convert(Column.class, beanClass.getProperties()), 
             beanClass.getKeyColumns(),
             beanClass);
    }

    public BeanRecordProxy(BeanClass beanClass)
    {
        this(null, beanClass);
    }
    
    public T getBean()
    {
        return data;
    }

    public void setBean(T data)
    {
        this.data = data;
    }

    @Override
    public Column getColumn(int index)
    {
        return columns.get(index);
    }

    @Override
    public ColumnExpr getColumnExpr(int index)
    {
        return columns.get(index);
    }

    @Override
    public Column[] getKeyColumns()
    {
        return keyColumns;
    }

    /**
     * Returns the array of primary key columns.
     * @return the array of primary key columns
     */
    @Override
    public Object[] getKey()
    {
        if (keyColumns==null)
            return null;
        // Get key values
        Object[] key = new Object[keyColumns.length];
        for (int i=0; i<keyColumns.length; i++)
            key[i] = this.getValue(keyColumns[i]);
        // the key
        return key;
    }

    @Override
    public int getFieldCount()
    {
        return columns.size();
    }

    @Override
    public int getFieldIndex(ColumnExpr column)
    {
        for (int i=0; i<columns.size(); i++)
        {
            if (columns.get(i).equals(column))
                return i;
        }
        return -1;
    }

    @Override
    public int getFieldIndex(String columnName)
    {
        for (int i=0; i<columns.size(); i++)
        {
            if (columns.get(i).getName().equals(columnName))
                return i;
        }
        return -1;
    }

    @Override
    public Options getFieldOptions(Column column)
    {
        return column.getOptions();
    }

    @Override
    public boolean isFieldVisible(Column column)
    {
        return true;
    }

    @Override
    public boolean isFieldReadOnly(Column column)
    {
    	if (isNew()==false && ObjectUtils.contains(keyColumns, column))
    		return true;
    	if (column.isAutoGenerated())
    		return true;
        return column.isReadOnly();
    }

    @Override
    public boolean isFieldRequired(Column column)
    {
        return column.isRequired();
    }

    @Override
    public boolean isModified()
    {
        return (modified!=null);
    }

    @Override
    public boolean isNew()
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // Record is new until all key fields have been supplied
        if (keyColumns!=null)
        {   // Check all Key Columns
            for (int i=0; i<keyColumns.length; i++)
            {
                Object value = getValue(keyColumns[i]);
                if ((value instanceof Number) && ((Number)value).longValue()==0)
                    return true;
                if (ObjectUtils.isEmpty(value))
                    return true;
            }
        }
        // Not new
        return false;
    }
    
    @Override
    public Entity getEntity()
    {
        return this.entity;
    }

    @Override
    public boolean isValid()
    {
        return (data!=null);
    }

    @Override
    public boolean isReadOnly()
    {
        return (isValid() ? false : true);
    }

    @Override
    public Object getValue(ColumnExpr column)
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // getBeanPropertyValue 
        return getBeanPropertyValue(data, column);
    }

    @Override
    public Object getValue(int index)
    {
        return getValue(getColumn(index));
    }

    @Override
    public boolean isNull(ColumnExpr column)
    {
        return ObjectUtils.isEmpty(getValue(column));
    }

    @Override
    public boolean isNull(int index)
    {
        return isNull(getColumn(index));
    }

    /**
     * Validates a value before it is set in the record.
     */
    @Override
    public Object validateValue(Column column, Object value)
    {
        return column.validateValue(value);
    }

    /**
     * sets the value of a field.
     */
    @Override
    public void setValue(Column column, Object value)
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        // Track modification status
        if (ObjectUtils.compareEqual(getValue(column), value)==false)
        {
            if (modified== null)
                modified = new boolean[columns.size()]; 
            modified[getFieldIndex(column)] = true;
        }
        // validate
        value = validateValue(column, value);
        // Set Value
        setBeanPropertyValue(data, column, value);
    }

    /**
     * sets the value of a field.
     */
    @Override
    public final void setValue(int i, Object value)
    {
        setValue(getColumn(i), value);
    }

    /**
     * Detects whether or not a particular field has been modified.
     */
    @Override
    public boolean wasModified(Column column)
    {
        int index = getFieldIndex(column);
        if (index<0)
            throw new ItemNotFoundException(column.getName());
        // check modified
        return (modified!=null && modified[index]);
    }

    /**
     * clears the modification status of the object and all fields.
     */
    public void clearModified()
    {
        modified = null;
    }

    // --------------- Bean support ------------------

    @Override
    public int setBeanProperties(Object bean)
    {
        return setBeanProperties(bean, null);
    }

    @Override
    public int setBeanProperties(Object bean, Collection<? extends ColumnExpr> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        { // Check Property
            Column column = getColumn(i);
            if (column.isReadOnly())
                continue;
            if (ignoreList != null && ignoreList.contains(column))
                continue; // ignore this property
            // Get Property Name
            setBeanPropertyValue(bean, column, getValue(i));
        }
        return count;
    }

    @Override
    public int setRecordValues(Object bean, Collection<Column> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        { // Check Property
            Column column = getColumn(i);
            if (column.isReadOnly())
                continue;
            if (ignoreList != null && ignoreList.contains(column))
                continue; // ignore this property
            // Get Property Name
            String property = column.getBeanPropertyName();
            Object value = getBeanPropertyValue(bean, property);
            setValue(column, value);
            count++;
        }
        return count;
    }
    
    @Override
    public int setRecordValues(Object bean)
    {
        return setRecordValues(bean, null);
    }

    // --------------- protected ------------------

    protected Object getBeanPropertyValue(Object bean, ColumnExpr column)
    {
        // Check Params
        if (bean==null)
            throw new InvalidArgumentException("bean", bean);
        if (column==null)
            throw new InvalidArgumentException("column", column);
        // getBeanPropertyValue 
        return getBeanPropertyValue(bean, column.getBeanPropertyName()); 
    }

    protected Object getBeanPropertyValue(Object bean, String property)
    {
        // Check Params
        if (bean==null)
            throw new InvalidArgumentException("bean", bean);
        if (property==null)
            throw new InvalidArgumentException("property", property);
        try
        {   // Get Property Value
            PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
            return pub.getSimpleProperty(bean, property);

        } catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        } catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        } catch (NoSuchMethodException e)
        {   log.warn(bean.getClass().getName() + ": no getter available for property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        }
    }

    protected void setBeanPropertyValue(Object bean, Column column, Object value)
    {
        // Check Params
        if (bean==null)
            throw new InvalidArgumentException("bean", bean);
        if (column==null)
            throw new InvalidArgumentException("column", column);
        // Get Property Name
        String property = column.getBeanPropertyName(); 
        try
        {   // Get Property Value
            if (ObjectUtils.isEmpty(value))
                value = null;
            // Set Property Value
            if (value!=null)
            {   // Bean utils will convert if necessary
                BeanUtils.setProperty(bean, property, value);
            }
            else
            {   // Don't convert, just set
                PropertyUtils.setProperty(bean, property, null);
            }
        } catch (IllegalArgumentException e) {
            log.error(bean.getClass().getName() + ": invalid argument for property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        } catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        } catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        } catch (NoSuchMethodException e) {
            log.error(bean.getClass().getName() + ": no setter available for property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }    
    }
    
}
