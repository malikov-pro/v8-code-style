/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Path;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.bsl.check.TransferringParametersBetweenClientAndServerCheck;

/**
 * Tests for {@link TransferringParametersBetweenClientAndServerCheck}.
 * <p>
 * Fixture project {@code TransferringParametersResearch}: a form client method calls the server common module method
 * with three parameters — a by-reference parameter that is never assigned (reported), a by-reference parameter that is
 * assigned (not reported) and a by-value parameter (not reported) — plus a local client-to-client call (not reported).
 *
 * @author malikov-pro
 */
public class TransferringParametersBetweenClientAndServerCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{
    private static final String CHECK_ID = "transferring-parameters-between-client-and-server"; //$NON-NLS-1$

    private static final String PROJECT_NAME = "TransferringParametersResearch"; //$NON-NLS-1$

    private static final String FORM_MODULE_FILE_NAME = "/src/CommonForms/ClientForm/Module.bsl"; //$NON-NLS-1$

    private static final String CLIENT_MODULE_FILE_NAME = "/src/CommonModules/ClientModule/Module.bsl"; //$NON-NLS-1$

    private static final String SERVER_MODULE_FILE_NAME = "/src/CommonModules/ServerModule/Module.bsl"; //$NON-NLS-1$

    private static final String SERVER_CALLER_MODULE_FILE_NAME = "/src/CommonModules/ServerCallerModule/Module.bsl"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testCrossModuleServerCallParameters()
    {
        assertNoSyntaxErrors(FORM_MODULE_FILE_NAME, getFormModule());
        assertNoSyntaxErrors(CLIENT_MODULE_FILE_NAME, getCommonModule(CLIENT_MODULE_FILE_NAME));
        assertNoSyntaxErrors(SERVER_MODULE_FILE_NAME, getCommonModule(SERVER_MODULE_FILE_NAME));
        assertNoSyntaxErrors(SERVER_CALLER_MODULE_FILE_NAME, getCommonModule(SERVER_CALLER_MODULE_FILE_NAME));

        // A client method calls the server common module: only the never-assigned by-reference parameter is reported.
        List<Marker> formMarkers = getMarkers(FORM_MODULE_FILE_NAME);
        assertEquals(dump(formMarkers), 1, formMarkers.size());
        assertEquals(Integer.valueOf(6), formMarkers.get(0).getExtraInfo().get(StandardExtraInfo.TEXT_LINE));
        assertEquals("Значение1", markedText(FORM_MODULE_FILE_NAME, formMarkers.get(0))); //$NON-NLS-1$
        String message = formMarkers.get(0).getMessage();
        assertTrue(message, message.contains("ПараметрНеизменяемый")); //$NON-NLS-1$
        assertTrue(message, message.contains("СервернаяПроцедура")); //$NON-NLS-1$

        // The client common module has the same call shape and is reported the same way.
        assertEquals(dump(getMarkers(CLIENT_MODULE_FILE_NAME)), 1, getMarkers(CLIENT_MODULE_FILE_NAME).size());

        // No client caller: the server module and the server caller are not reported.
        assertTrue(dump(getMarkers(SERVER_MODULE_FILE_NAME)), getMarkers(SERVER_MODULE_FILE_NAME).isEmpty());
        assertTrue(dump(getMarkers(SERVER_CALLER_MODULE_FILE_NAME)),
            getMarkers(SERVER_CALLER_MODULE_FILE_NAME).isEmpty());
    }

    private List<Marker> getMarkers(String moduleFileName)
    {
        String moduleId = Path.ROOT.append(getTestConfigurationName()).append(moduleFileName).toString();
        List<Marker> markers = List.of(markerManager.getMarkers(getProject().getWorkspaceProject(), moduleId));
        return markers.stream().filter(marker -> CHECK_ID.equals(getCheckIdFromMarker(marker, getProject())))
            .collect(Collectors.toList());
    }

    private String markedText(String moduleFileName, Marker marker)
    {
        Module module = moduleFileName.startsWith("/src/CommonForms/") //$NON-NLS-1$
            ? getFormModule()
            : getCommonModule(moduleFileName);
        Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
        Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
        assertTrue(moduleFileName + " has no offset/length", offset != null && length != null); //$NON-NLS-1$
        return NodeModelUtils.findActualNodeFor(module).getRootNode().getText().substring(offset, offset + length);
    }

    private Module getCommonModule(String moduleFileName)
    {
        String name = moduleFileName.substring(moduleFileName.indexOf("CommonModules/") + 14, //$NON-NLS-1$
            moduleFileName.lastIndexOf('/'));
        return ((CommonModule)getTopObjectByFqn("CommonModule." + name, getProject())).getModule(); //$NON-NLS-1$
    }

    private Module getFormModule()
    {
        return ((Form)getTopObjectByFqn("CommonForm.ClientForm.Form", getProject())).getModule(); //$NON-NLS-1$
    }

    private static void assertNoSyntaxErrors(String moduleFileName, Module module)
    {
        var node = NodeModelUtils.findActualNodeFor(module);
        assertNotNull(moduleFileName + ": no node model", node); //$NON-NLS-1$
        for (var leaf : node.getLeafNodes())
        {
            assertNull(moduleFileName + ": syntax error at line " + leaf.getStartLine() + ": " //$NON-NLS-1$ //$NON-NLS-2$
                + leaf.getSyntaxErrorMessage(), leaf.getSyntaxErrorMessage());
        }
    }

    private static String dump(List<Marker> markers)
    {
        return markers.stream()
            .map(marker -> marker.getCheckId() + " " + marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE) + " " //$NON-NLS-1$ //$NON-NLS-2$
                + marker.getMessage())
            .collect(Collectors.joining("; ")); //$NON-NLS-1$
    }
}
