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
package org.apache.empire.jsf2.components;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.NamingContainer;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.controls.InputControlManager;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.apache.empire.jsf2.utils.TagEncodingHelperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabViewTag extends UIOutput implements NamingContainer
{
    // Logger
    // private static final Logger log = LoggerFactory.getLogger(MenuTag.class);
    private static final Logger       log                    = LoggerFactory.getLogger(TabViewTag.class);

    protected final String            TAB_STYLE_CLASS        = "eTabView";

    protected final String            TAB_ACTIVE_INDEX       = "activeIndex";

    protected final String            TABLINK_ID_PREFIX      = "tabLink";

    protected final String            TAB_RENDERED_ATTRIBUTE = "visible";

    protected final TagEncodingHelper helper                 = TagEncodingHelperFactory.create(this, this.TAB_STYLE_CLASS);

    public static class TabPageActionListener implements ActionListener, StateHolder
    {

        // private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
        // private static final Object[] EMPTY_PARAMS = new Object[0];

        private String  clientId;
        private boolean isTransient = false;

        /** Creates a new instance of MethodExpressionActionListener */
        public TabPageActionListener()
        {
            // constructor for state-saving 
        }

        public TabPageActionListener(String clientId)
        {
            // constructor for state-saving
            this.clientId = clientId;
        }

        @Override
        public void processAction(ActionEvent actionEvent)
            throws AbortProcessingException
        {
            // UIComponent findBase = ComponentUtils.findComponent(null, clientId, separatorChar);
            FacesContext fc = FacesContext.getCurrentInstance();
            UIComponent tabView = FacesUtils.getWebApplication().findComponent(fc, this.clientId, null);
            if (!(tabView instanceof TabViewTag))
            {
                throw new UnexpectedReturnValueException(tabView, "findComponent");
            }
            // Invoke
            TabViewTag tvt = (TabViewTag) tabView;
            tvt.setActiveTab(actionEvent);
        }

        @Override
        public void restoreState(FacesContext context, Object state)
        {
            // clientId = (String) ((Object[]) state)[0];
            this.clientId = (String) state;
        }

        @Override
        public Object saveState(FacesContext context)
        {
            // return new Object[] { clientId };
            return this.clientId;
        }

        @Override
        public void setTransient(boolean newTransientValue)
        {
            this.isTransient = newTransientValue;
        }

        @Override
        public boolean isTransient()
        {
            return this.isTransient;
        }
    }

    public TabViewTag()
    {
        log.trace("TabViewTag created");
    }

    @Override
    public String getFamily()
    {
        return UINamingContainer.COMPONENT_FAMILY;
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // call base
        super.encodeBegin(context);

        // registerTabViewBean
        // context.getExternalContext().getRequestMap().put("tabView", this);

        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(InputControl.HTML_TAG_DIV, this);
        writer.writeAttribute(InputControl.HTML_ATTR_ID, getClientId(), null);
        writer.writeAttribute(InputControl.HTML_ATTR_CLASS, this.helper.getTagStyleClass(), null);
        this.helper.writeAttribute(writer, InputControl.HTML_ATTR_STYLE, this.helper.getTagAttributeString("style"));

        // The Tabs
        if (ObjectUtils.getBoolean(this.helper.getTagAttributeValue("hideTabBar")))
        {   // hide bar
            encodeTabs(context, null);
        }
        else
        {   // show bar
            writer.startElement(InputControl.HTML_TAG_TABLE, this);
            writer.writeAttribute(InputControl.HTML_ATTR_CLASS, "eTabBar", null);
            writer.startElement(InputControl.HTML_TAG_TR, this);
            encodeTabs(context, writer);
            writer.startElement(InputControl.HTML_TAG_TD, this);
            writer.writeAttribute(InputControl.HTML_ATTR_CLASS, "eTabBarEmpty", null);
            writer.endElement(InputControl.HTML_TAG_TD);
            writer.endElement(InputControl.HTML_TAG_TR);
            writer.endElement(InputControl.HTML_TAG_TABLE);
        }
        
        // The Pages
        writer.startElement(InputControl.HTML_TAG_TABLE, this);
        writer.writeAttribute(InputControl.HTML_ATTR_CLASS, "eTabPanel", null);
        String minHeight = this.helper.getTagAttributeString("minHeight");
        if (StringUtils.isNotEmpty(minHeight))
        {
            writer.writeAttribute(InputControl.HTML_ATTR_STYLE, "min-height:" + minHeight, null);
        }
    }

    @Override
    public boolean getRendersChildren()
    {
        return super.getRendersChildren();
    }

    @Override
    public void encodeChildren(FacesContext context)
        throws IOException
    {
        super.encodeChildren(context);
    }

    @Override
    public void encodeEnd(FacesContext context)
        throws IOException
    {
        // call base
        super.encodeEnd(context);
        // close
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement(InputControl.HTML_TAG_TABLE);
        writer.endElement(InputControl.HTML_TAG_DIV);
    }

    @Override
    public void decode(FacesContext context)
    {
        for (UIComponent c : getChildren())
        {
            c.decode(context);
        }
        super.decode(context);
    }

    /*
    @Override
    public void processDecodes(FacesContext context)
    {
        super.processDecodes(context);
    }
    */

    protected void encodeTabs(FacesContext context, ResponseWriter writer)
        throws IOException
    {
        Iterator<UIComponent> ci = getFacetsAndChildren();
        if (ci.hasNext() == false)
        {
            log.warn("Invalid TabPage definition!");
            return;
        }
        UIComponent panel = ci.next();
        int index = 0;
        int activeIndex = getActivePageIndex();
        // Patch for MOJARRA: Remove HtmlCommandLinks
        List<UIComponent> chk = panel.getChildren();
        for (int i = chk.size() - 1; i >= 0; i--)
        {
            if ((chk.get(i) instanceof HtmlCommandLink))
                chk.remove(i);
        }
        // Create Page Links
        for (UIComponent c : panel.getChildren())
        { // Find Tab pages
            if (!(c instanceof TabPageTag))
            {
                continue;
            }
            // found
            boolean active = (index == activeIndex);
            TabPageTag page = (TabPageTag) c;

            // render tab-link? default is true
            boolean rendered = ObjectUtils.getBoolean(ObjectUtils.coalesce(page.getAttributes().get(this.TAB_RENDERED_ATTRIBUTE), true));
            if (!rendered)
            {
                // dont render content
                page.setRendered(false);
                continue;
            }
            if (writer!=null)
            {   // encode Tab
                boolean disabled = ObjectUtils.getBoolean(TagEncodingHelper.getTagAttributeValue(page, "disabled"));
                writer.startElement(InputControl.HTML_TAG_TD, this);
                // tab label
                String styleClass = "eTabLabel";
                if (active)
                {
                    styleClass += " eTabActive";
                }
                else if (disabled)
                {
                    styleClass += " eTabDisabled";
                }
                writer.writeAttribute(InputControl.HTML_ATTR_CLASS, styleClass, null);
                // encode Link
                encodeTabLink(context, writer, index, page, (active || disabled));
                writer.endElement(InputControl.HTML_TAG_TD);
            }
            // set rendered
            page.setRendered(active);
            // next
            index++;
        }
    }

    protected void encodeTabLink(FacesContext context, ResponseWriter writer, int index, TabPageTag page, boolean disabled)
        throws IOException
    {
        // Add component
        HtmlCommandLink link = null;
        List<UIComponent> tabLinks = getChildren();
        if (tabLinks.size() > index)
        {
            UIComponent c = tabLinks.get(index);
            if (c instanceof HtmlCommandLink)
            {
                link = (HtmlCommandLink) c;
            }
            else
            { // Something's wrong here?
                log.error("INFO: Unexpected child node for {}! Child item type is {}.", getClass().getName(), c.getClass().getName());
                // encode anyway
                c.setRendered(true);
                c.encodeAll(context);
                c.setRendered(false); // Don't render twice!
                return;
            }
        }
        if (link == null)
        { // create the tab-Link   
            String linkId = this.TABLINK_ID_PREFIX + String.valueOf(index);
            link = createCommandLink(context, linkId);
            tabLinks.add(index, link);
            // Set TabPageActionListener
            TabPageActionListener tpal = new TabPageActionListener(this.getClientId());
            link.addActionListener(tpal);
        }
        // init linkComponent
        link.setValue(page.getTabLabel());
        link.setDisabled(disabled);
        // Set Style
        String styleClass = "eTabLink";
        link.setStyleClass(styleClass);

        // encode link
        link.setRendered(true);
        link.encodeAll(context);
        link.setRendered(false); // Don't render twice!
    }

    protected HtmlCommandLink createCommandLink(FacesContext context, String linkId)
    {
        // CommandLink link 
        HtmlCommandLink link = InputControlManager.createComponent(context, HtmlCommandLink.class);
        link.setId(linkId);
        return link;
    }

    public int getActivePageIndex()
    {
        Object active = this.helper.getTagAttributeValue(this.TAB_ACTIVE_INDEX);
        return ObjectUtils.getInteger(active);
    }

    public void setActivePageIndex(int activeIndex)
    {
        ValueExpression ve = this.getValueExpression(this.TAB_ACTIVE_INDEX);
        if (ve != null)
        { // set active index
            FacesContext fc = FacesUtils.getContext();
            ve.setValue(fc.getELContext(), activeIndex);
        }
        else
        { // save activeIndex
            getAttributes().put(this.TAB_ACTIVE_INDEX, activeIndex);
        }
    }

    public void setActiveTab(ActionEvent event)
    {
        log.debug("setActiveTab");
        // done
        UIComponent comp = event.getComponent();
        String tabNo = comp.getId().substring(this.TABLINK_ID_PREFIX.length());
        int pageIndex = ObjectUtils.getInteger(tabNo);
        if (pageIndex == getActivePageIndex())
        {   // already set
            log.warn("setActiveTab is called for active page!");
            return;
        }

        // set new Page
        setActivePageIndex(pageIndex);

        // TabChangeListener
        Object tcl = getAttributes().get("tabChangedListener");
        if (tcl != null)
        {
            if (!(tcl instanceof MethodExpression))
            {
                log.error("tabChangedListener is not a valid method expression!");
                return;
            }
            FacesContext fc = FacesUtils.getContext();
            MethodExpression methodExpression = (MethodExpression) tcl;
            methodExpression.invoke(fc.getELContext(), new Object[] { pageIndex });
        }

    }
}
