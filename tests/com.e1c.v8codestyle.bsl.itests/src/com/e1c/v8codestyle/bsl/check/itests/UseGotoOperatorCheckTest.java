package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.UseGotoOperatorCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Tests for {@link UseGotoOperatorCheck} check.
 *
 * @author Vadim Goncharov
 */
public class UseGotoOperatorCheckTest
    extends AbstractSingleModuleTestBase
{

    public UseGotoOperatorCheckTest()
    {
        super(UseGotoOperatorCheck.class);
    }

    /**
     * Test the module use goto operator and labeled statement.
     *
     * @throws Exception the exception
     */
    @Test
    public void testUseGotoOperator() throws Exception
    {

        updateModule(FOLDER_RESOURCE + "use-goto-operator.bsl");

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
        List<Integer> errorLines = markers.stream()
            .map(marker -> marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .map(Integer.class::cast)
            .sorted()
            .collect(Collectors.toList());
        assertEquals(List.of(3, 5), errorLines);

    }

    @Test
    public void testDefaultSeverityIsBlocker()
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertEquals(IssueSeverity.BLOCKER, settings.getSeverity());
    }

    @Test
    public void testIncrementalGotoRemovalAndRestoration() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "use-goto-policy-bad.bsl"); //$NON-NLS-1$
        assertMarkerCount(2);
        updateModule(FOLDER_RESOURCE + "use-goto-policy-clean.bsl"); //$NON-NLS-1$
        assertMarkerCount(0);
        updateModule(FOLDER_RESOURCE + "use-goto-policy-bad.bsl"); //$NON-NLS-1$
        assertMarkerCount(2);
    }

    private void assertMarkerCount(int expected)
    {
        for (var leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().size());
    }

    @Test
    public void testExplicitSeveritySurvivesSourceChanges() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        IssueSeverity original = settings.getSeverity();
        try
        {
            settings.setSeverity(IssueSeverity.MINOR);
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            updateModule(FOLDER_RESOURCE + "use-goto-policy-bad.bsl"); //$NON-NLS-1$
            assertMarkerCount(2);
            assertEquals(IssueSeverity.MINOR, checkRepository.getSettings(
                new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID), getProject().getWorkspaceProject()).getSeverity());
            updateModule(FOLDER_RESOURCE + "use-goto-policy-clean.bsl"); //$NON-NLS-1$
            assertMarkerCount(0);
            updateModule(FOLDER_RESOURCE + "use-goto-policy-bad.bsl"); //$NON-NLS-1$
            assertMarkerCount(2);
            assertEquals(IssueSeverity.MINOR, checkRepository.getSettings(
                new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID), getProject().getWorkspaceProject()).getSeverity());
        }
        finally
        {
            settings.setSeverity(original);
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }

}
