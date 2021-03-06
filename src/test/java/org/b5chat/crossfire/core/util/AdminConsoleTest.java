/**
 * $RCSfile$
 * $Revision: 11608 $
 * $Date: 2010-02-07 16:03:12 -0500 (Sun, 07 Feb 2010) $
 *
 * Copyright (C) 2004-2008 B5Chat Community. All rights reserved.
 */

package org.b5chat.crossfire.core.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.lang.reflect.Method;
import junit.framework.TestCase;

import org.b5chat.crossfire.plugin.admin.AdminConsole;
import org.dom4j.Element;

public class AdminConsoleTest extends TestCase {

    public AdminConsoleTest() {

    }

    /**
     * Resets the admin console internal data structures.
     */
    @Override
	public void tearDown() throws Exception {
        Class c = AdminConsole.class;
        Method init = c.getDeclaredMethod("load", (Class[])null);
        init.setAccessible(true);
        init.invoke((Object)null, (Object[])null);
    }

    public void testGetGlobalProps() throws Exception {
        String name = AdminConsole.getAppName();
        String image = AdminConsole.getLogoImage();
        assertEquals("crossfire", name);
        assertEquals("images/header-title.gif", image);
    }

    public void testModifyGlobalProps() throws Exception {
        // Add a new stream to the AdminConsole:
        String filename = TestUtils.prepareFilename(
                "./resources/net/b5chat/admin/AdminConsoleTest.admin-sidebar-01.xml");
        InputStream in = new FileInputStream(filename);
        AdminConsole.addModel("test1", in);
        in.close();
        String name = AdminConsole.getAppName();
        assertEquals("Foo Bar", name);
        String img = AdminConsole.getLogoImage();
        assertEquals("foo.gif", img);
    }

    public void testNewTabs() throws Exception {
        // Add a new stream to the AdminConsole:
        String filename = TestUtils.prepareFilename(
                "./resources/net/b5chat/admin/AdminConsoleTest.admin-sidebar-02.xml");
        InputStream in = new FileInputStream(filename);
        AdminConsole.addModel("test2", in);
        in.close();
        Collection tabs = AdminConsole.getModel().selectNodes("//tab");
        assertNotNull(tabs);
        assertTrue(tabs.size() > 0);
        boolean found = false;
        for (Iterator iter=tabs.iterator(); iter.hasNext(); ) {
            Element tab = (Element)iter.next();
            if ("foobar".equals(tab.attributeValue("id"))) {
                found = true;
                assertEquals("Foo Bar", tab.attributeValue("name"));
                assertEquals("Click to see foo bar", tab.attributeValue("description"));
            }
        }
        if (!found) {
            fail("Expected new item 'foobar' was not found.");
        }
    }

    public void testTabOverwrite() throws Exception {
        // Add a new stream to the AdminConsole:
        String filename = TestUtils.prepareFilename(
                "./resources/net/b5chat/admin/AdminConsoleTest.admin-sidebar-03.xml");
        InputStream in = new FileInputStream(filename);
        AdminConsole.addModel("test3", in);
        in.close();
        boolean found = false;
        for (Iterator tabs=AdminConsole.getModel().selectNodes("//tab").iterator(); tabs.hasNext(); ) {
            Element tab = (Element)tabs.next();
            if ("server".equals(tab.attributeValue("id"))) {
                found = true;
                assertEquals("New Server Title", tab.attributeValue("name"));
                assertEquals("Testing 1 2 3", tab.attributeValue("description"));
            }
        }
        if (!found) {
            fail("Failed to overwrite 'server' tab with new properties.");
        }
    }
}
