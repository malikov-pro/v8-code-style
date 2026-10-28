/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.UnreachableCodeCheck;

/**
 * Module body scope of {@link UnreachableCodeCheck}: object module body is legal
 * in EDT (unlike common modules), so gaps after Goto must be reported there.
 */
public class UnreachableCodeModuleBodyCheckTest extends AbstractSingleModuleTestBase
{
    public UnreachableCodeModuleBodyCheckTest()
    {
        super(UnreachableCodeCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "CatalogModules"; //$NON-NLS-1$
    }

    @Override
    protected String getModuleFileName()
    {
        return "/src/Catalogs/TestCatalog/ObjectModule.bsl"; //$NON-NLS-1$
    }

    @Override
    protected com._1c.g5.v8.dt.bsl.model.Module getModule()
    {
        IBmObject mdObject = getTopObjectByFqn("Catalog.TestCatalog", getProject()); //$NON-NLS-1$
        org.junit.Assert.assertTrue(mdObject instanceof Catalog);
        Module module = ((Catalog)mdObject).getObjectModule();
        org.junit.Assert.assertNotNull(module);
        return module;
    }

    @Test
    public void testModuleBodyScope() throws Exception
    {
        var module = updateAndGetModule(FOLDER_RESOURCE + "unreachable-code-module-body.bsl"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        // module body code must follow the methods (EDT grammar); the body here is
        // clean, the method gap after Goto (line 3) is reported
        assertEquals(List.of(3), getModuleMarkers().stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
    }
}
