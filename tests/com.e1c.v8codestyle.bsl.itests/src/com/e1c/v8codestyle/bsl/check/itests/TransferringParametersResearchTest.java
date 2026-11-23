/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureEntry;
import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Pragma;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.SourceObjectLinkProvider;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.resource.DynamicFeatureAccessComputer;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.mcore.DerivedProperty;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.NamedElement;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.util.Environments;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Research-only test: is a cross-module {@link Invocation} resolved in the EDT BSL model, is the server context of the
 * target {@link Method} visible, and can a not-assigned by-reference {@link FormalParam} be detected reliably?
 * <p>
 * Fixture: {@code workspace/TransferringParametersResearch} — a client common module calls a server common module
 * method. The server method has a by-reference parameter which is only read, a by-reference parameter which is
 * assigned, and a by-value parameter.
 * <p>
 * The test only prints facts; the assertions are limited to fixture validity (both modules parse, the invocation
 * exists). Resolution failures are printed, not asserted.
 * <p>
 * Known caveat: a module read outside a validation (check) context is not linked at the AST feature level — every
 * {@link StaticFeatureAccess} reports empty {@code getFeatureEntries()}, including references to local variables.
 * Therefore the {@code FeatureEntry} verdict printed here is inconclusive by construction; the name-based lookup and
 * the structural (SimpleStatement) traversal are the parts that do work in a plain itest.
 *
 * @author malikov-pro
 */
public class TransferringParametersResearchTest
    extends SingleProjectReadOnlyCheckTestBase
{
    private static final String PROJECT_NAME = "TransferringParametersResearch"; //$NON-NLS-1$

    private static final String CLIENT_MODULE_FQN = "CommonModule.ClientModule"; //$NON-NLS-1$

    private static final String SERVER_MODULE_FQN = "CommonModule.ServerModule"; //$NON-NLS-1$

    private static final String FORM_MODULE_FQN = "CommonForm.ClientForm.Form"; //$NON-NLS-1$

    private static final String SERVER_CALLER_MODULE_FQN = "CommonModule.ServerCallerModule"; //$NON-NLS-1$

    private DynamicFeatureAccessComputer dynamicComputer;

    private List<Module> knownModules = List.of();

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testCrossModuleInvocationResolution()
    {
        Module clientModule = getCommonModule(CLIENT_MODULE_FQN);
        Module serverModule = getCommonModule(SERVER_MODULE_FQN);
        Module formModule = getFormModule(FORM_MODULE_FQN);
        Module serverCallerModule = getCommonModule(SERVER_CALLER_MODULE_FQN);
        knownModules = List.of(clientModule, serverModule, formModule, serverCallerModule);

        assertNoSyntaxErrors(CLIENT_MODULE_FQN, clientModule);
        assertNoSyntaxErrors(SERVER_MODULE_FQN, serverModule);
        assertNoSyntaxErrors(FORM_MODULE_FQN, formModule);
        assertNoSyntaxErrors(SERVER_CALLER_MODULE_FQN, serverCallerModule);

        printModuleFacts("CLIENT", clientModule);
        printModuleFacts("SERVER", serverModule);
        printModuleFacts("FORM", formModule);
        printModuleFacts("SERVER CALLER", serverCallerModule);

        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Transferring parameters research")
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                analyseInvocations(transaction, "CLIENT COMMON MODULE", clientModule); //$NON-NLS-1$
                analyseInvocations(transaction, "FORM MODULE", formModule); //$NON-NLS-1$
                analyseInvocations(transaction, "SERVER COMMON MODULE", serverCallerModule); //$NON-NLS-1$
                return null;
            }
        });

        printMarkers("client common module", clientModule); //$NON-NLS-1$
        printMarkers("form module", formModule); //$NON-NLS-1$
        printMarkers("server common module", serverCallerModule); //$NON-NLS-1$
        print("NOTE: empty FeatureEntry lists below are a read-context limitation, not proof of failed resolution:"); //$NON-NLS-1$
        print("NOTE: all modules report withFeatureEntries=0 even for local variables (see module facts above)."); //$NON-NLS-1$
        print("NOTE: validation-context resolution is covered by MissedRequiredParameterCheck/CrossModuleTest."); //$NON-NLS-1$
    }

    private void analyseInvocations(IBmTransaction transaction, String title, Module callerModule)
    {
        List<Invocation> invocations = EcoreUtil2.getAllContentsOfType(callerModule, Invocation.class);
        print("%s: invocations=%d", title, invocations.size()); //$NON-NLS-1$
        assertTrue("No invocation found in the fixture modules", !invocations.isEmpty()); //$NON-NLS-1$

        for (Invocation invocation : invocations)
        {
            FeatureAccess access = invocation.getMethodAccess();
            Method caller = EcoreUtil2.getContainerOfType(invocation, Method.class);
            print("---- invocation: name=%s accessClass=%s params=%d isServerCallFlag=%s callerMethod=%s", //$NON-NLS-1$
                access == null ? null : access.getName(), className(access), invocation.getParams().size(),
                invocation.isIsServerCall(), caller == null ? null : caller.getName());
            if (access == null)
            {
                continue;
            }

            Environments environments = environmentsOf(invocation);
            print("  caller environments: %s", environments); //$NON-NLS-1$
            print("  methodAccess instanceof StaticFeatureAccess: %s", access instanceof StaticFeatureAccess); //$NON-NLS-1$
            print("  methodAccess instanceof DynamicFeatureAccess: %s", access instanceof DynamicFeatureAccess); //$NON-NLS-1$

            List<FeatureEntry> entries = featureEntries(access, environments);
            print("  feature entries used: %d", entries.size()); //$NON-NLS-1$

            Method target = null;
            int index = 0;
            for (FeatureEntry entry : entries)
            {
                EObject raw = entry.getFeature();
                print("  entry[%d]: entryEnvironments=%s featureClass=%s featureName=%s proxy=%s", index++, //$NON-NLS-1$
                    entry.getEnvironments(), className(raw), name(raw), raw == null ? "n/a" : raw.eIsProxy()); //$NON-NLS-1$
                if (raw instanceof BslContextDefMethod mock)
                {
                    print("    BslContextDefMethod.isServerCall(): %s", mock.isServerCall()); //$NON-NLS-1$
                }
                if (raw != null)
                {
                    print("    raw URI: %s", EcoreUtil.getURI(raw)); //$NON-NLS-1$
                    if (raw instanceof SourceObjectLinkProvider link)
                    {
                        print("    SourceObjectLinkProvider.getSourceUri(): %s", link.getSourceUri()); //$NON-NLS-1$
                    }
                }
                EObject feature = resolveFeature(entry, invocation, transaction);
                if (feature == null)
                {
                    print("    resolved feature: null"); //$NON-NLS-1$
                    continue;
                }
                print("    resolved: class=%s name=%s proxy=%s module=%s", className(feature), name(feature), //$NON-NLS-1$
                    feature.eIsProxy(), moduleOf(feature));
                if (feature instanceof Method method)
                {
                    target = method;
                }
                else if (target == null)
                {
                    target = methodThroughDerivedPropertyTypes(feature, access.getName(), transaction);
                }
            }

            if (target != null)
            {
                printTargetMethod(target, callerModule);
            }
            else
            {
                print("  TARGET method NOT resolved through FeatureEntry/DerivedProperty types"); //$NON-NLS-1$
                String receiverName = access instanceof DynamicFeatureAccess dynamicAccess
                    && dynamicAccess.getSource() instanceof FeatureAccess sourceAccess ? sourceAccess.getName() : null;
                print("  fallback: receiverName=%s, lookup by module name + method name", receiverName); //$NON-NLS-1$
                Method byName = methodByModuleName(receiverName, access.getName());
                if (byName == null)
                {
                    print("  fallback: method '%s' not found in known modules", access.getName()); //$NON-NLS-1$
                }
                else
                {
                    print("  fallback resolved method by module+name"); //$NON-NLS-1$
                    printTargetMethod(byName, callerModule);
                }
            }
        }
    }

    private Method methodByModuleName(String moduleName, String methodName)
    {
        if (moduleName == null || methodName == null)
        {
            return null;
        }
        for (Module module : knownModules)
        {
            if (module.getOwner() instanceof CommonModule commonModule
                && moduleName.equalsIgnoreCase(commonModule.getName()))
            {
                for (Method method : module.allMethods())
                {
                    if (methodName.equalsIgnoreCase(method.getName()))
                    {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    private List<FeatureEntry> featureEntries(FeatureAccess access, Environments environments)
    {
        if (access instanceof StaticFeatureAccess staticAccess)
        {
            return staticAccess.getFeatureEntries();
        }
        if (access instanceof DynamicFeatureAccess dynamicAccess)
        {
            EObject source = dynamicAccess.getSource();
            print("  dynamic source: class=%s name=%s", className(source), //$NON-NLS-1$
                source instanceof FeatureAccess sourceFeatureAccess ? sourceFeatureAccess.getName() : name(source));
            if (source instanceof StaticFeatureAccess sourceAccess)
            {
                print("  dynamic source featureEntries: %d", sourceAccess.getFeatureEntries().size()); //$NON-NLS-1$
            }
            else if (source instanceof DynamicFeatureAccess sourceAccess)
            {
                print("  dynamic source featureEntries: %d", sourceAccess.getFeatureEntries().size()); //$NON-NLS-1$
            }
            List<FeatureEntry> entries = dynamicAccess.getFeatureEntries();
            print("  dynamic access featureEntries set=%s size=%d", dynamicAccess.isSetFeatureEntries(), //$NON-NLS-1$
                entries.size());
            if (entries.isEmpty())
            {
                entries = dynamicComputer().resolveObject(dynamicAccess, environments);
                print("  DynamicFeatureAccessComputer.resolveObject gave %d entries", entries.size()); //$NON-NLS-1$
            }
            return entries;
        }
        return List.of();
    }

    private Method methodThroughDerivedPropertyTypes(EObject feature, String methodName, IBmTransaction transaction)
    {
        if (!(feature instanceof DerivedProperty derivedProperty))
        {
            return null;
        }
        print("    DerivedProperty source=%s types=%d", className(derivedProperty.getSource()), //$NON-NLS-1$
            derivedProperty.getTypes().size());
        for (TypeItem item : derivedProperty.getTypes())
        {
            if (!(item instanceof Type type) || type.getContextDef() == null)
            {
                continue;
            }
            print("    type '%s' contextDef methods=%d", name(type), type.getContextDef().allMethods().size()); //$NON-NLS-1$
            for (com._1c.g5.v8.dt.mcore.Method candidate : type.getContextDef().allMethods())
            {
                if (candidate.getName() == null || !candidate.getName().equalsIgnoreCase(methodName))
                {
                    continue;
                }
                print("      candidate: class=%s isServerCall=%s", className(candidate), //$NON-NLS-1$
                    candidate instanceof BslContextDefMethod mock ? String.valueOf(mock.isServerCall()) : "n/a"); //$NON-NLS-1$
                if (candidate instanceof SourceObjectLinkProvider link && link.getSourceUri() != null)
                {
                    EObject resolved = transaction.getExternalObjectByUri(link.getSourceUri());
                    print("      candidate sourceUri resolved to: %s", className(resolved)); //$NON-NLS-1$
                    if (resolved instanceof Method bslMethod)
                    {
                        return bslMethod;
                    }
                }
                if (candidate instanceof Method bslMethod)
                {
                    return bslMethod;
                }
            }
        }
        return null;
    }

    private void printTargetMethod(Method method, Module callerModule)
    {
        print("  TARGET method '%s': class=%s export=%s pragmas=%d environments=%s module=%s sameModuleAsCaller=%s", //$NON-NLS-1$
            method.getName(), className(method), method.isExport(), method.getPragmas().size(), environments(method),
            moduleOf(method), method.eContainer() == callerModule);
        for (Pragma pragma : method.getPragmas())
        {
            print("    pragma: symbol='%s' value='%s'", pragma.getSymbol(), pragma.getValue()); //$NON-NLS-1$
        }

        int accessCount = 0;
        int accessWithEntries = 0;
        for (StaticFeatureAccess bodyAccess : EcoreUtil2.getAllContentsOfType(method, StaticFeatureAccess.class))
        {
            accessCount++;
            if (!bodyAccess.getFeatureEntries().isEmpty())
            {
                accessWithEntries++;
            }
        }
        print("    body StaticFeatureAccess: total=%d withFeatureEntries=%d", accessCount, accessWithEntries); //$NON-NLS-1$

        for (FormalParam param : method.getFormalParams())
        {
            List<String> identityWrites = new ArrayList<>();
            List<String> nameOccurrences = new ArrayList<>();
            for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(method, SimpleStatement.class))
            {
                if (!(statement.getLeft() instanceof StaticFeatureAccess left) || statement.getRight() == null)
                {
                    continue;
                }
                if (left.getName() != null && param.getName() != null
                    && left.getName().equalsIgnoreCase(param.getName()))
                {
                    nameOccurrences.add(left.getName());
                }
                for (FeatureEntry entry : left.getFeatureEntries())
                {
                    if (entry.getFeature() == param)
                    {
                        identityWrites.add(left.getName());
                    }
                }
            }
            print("    param '%s': byValue(Знач)=%s hasDefault=%s | assignedByIdentity=%d | nameMatches=%d", //$NON-NLS-1$
                param.getName(), param.isByValue(), param.getDefaultValue() != null, identityWrites.size(),
                nameOccurrences.size());
            if (!identityWrites.isEmpty())
            {
                print("      identity writes: %s", identityWrites); //$NON-NLS-1$
            }
        }
    }

    private void printModuleFacts(String title, Module module)
    {
        print("%s module: type=%s owner=%s uri=%s", title, module.getModuleType(), //$NON-NLS-1$
            name(module.getOwner()), module.eResource() == null ? null : module.eResource().getURI());
        print("%s module environments=%s", title, environments(module)); //$NON-NLS-1$
        int accessCount = 0;
        int withEntries = 0;
        for (StaticFeatureAccess access : EcoreUtil2.getAllContentsOfType(module, StaticFeatureAccess.class))
        {
            accessCount++;
            if (!access.getFeatureEntries().isEmpty())
            {
                withEntries++;
            }
        }
        print("%s module: StaticFeatureAccess in body total=%d withFeatureEntries=%d", title, accessCount, //$NON-NLS-1$
            withEntries);
        for (Method method : module.allMethods())
        {
            print("  %s method '%s' export=%s environments=%s pragmas=%d", title, method.getName(), //$NON-NLS-1$
                method.isExport(), environments(method), method.getPragmas().size());
        }
    }

    private void printMarkers(String title, Module module)
    {
        if (module.eResource() == null)
        {
            return;
        }
        String moduleId = module.eResource().getURI().toPlatformString(true);
        var markers = markerManager.getMarkers(getProject().getWorkspaceProject(), moduleId);
        print("markers on %s (%s): %d", title, moduleId, markers == null ? -1 : markers.length); //$NON-NLS-1$
        if (markers != null)
        {
            for (var marker : markers)
            {
                print("  marker: checkId=%s message=%s", marker.getCheckId(), marker.getMessage()); //$NON-NLS-1$
            }
        }
    }

    private EObject resolveFeature(FeatureEntry entry, EObject context, IBmTransaction transaction)
    {
        EObject feature = entry.getFeature();
        if (feature == null)
        {
            return null;
        }
        if (feature.eIsProxy())
        {
            try
            {
                feature = EcoreUtil.resolve(feature, context);
            }
            catch (RuntimeException e)
            {
                print("    EcoreUtil.resolve failed: %s", e); //$NON-NLS-1$
            }
        }
        if (feature instanceof SourceObjectLinkProvider link)
        {
            URI sourceUri = link.getSourceUri();
            if (sourceUri != null)
            {
                EObject fromTransaction = transaction.getExternalObjectByUri(sourceUri);
                print("    transaction.getExternalObjectByUri -> %s", className(fromTransaction)); //$NON-NLS-1$
                if (fromTransaction != null)
                {
                    feature = fromTransaction;
                }
            }
        }
        return feature;
    }

    private DynamicFeatureAccessComputer dynamicComputer()
    {
        if (dynamicComputer == null)
        {
            dynamicComputer = BslPlugin.getDefault().getInjector().getInstance(DynamicFeatureAccessComputer.class);
        }
        return dynamicComputer;
    }

    private Module getCommonModule(String fqn)
    {
        Object object = getTopObjectByFqn(fqn, getProject());
        assertTrue("Not a CommonModule: " + fqn, object instanceof CommonModule); //$NON-NLS-1$
        Module module = ((CommonModule)object).getModule();
        assertNotNull("No module for " + fqn, module); //$NON-NLS-1$
        return module;
    }

    private Module getFormModule(String fqn)
    {
        Object object = getTopObjectByFqn(fqn, getProject());
        assertTrue("Not a Form: " + fqn, object instanceof Form); //$NON-NLS-1$
        Module module = ((Form)object).getModule();
        assertNotNull("No module for " + fqn, module); //$NON-NLS-1$
        return module;
    }

    private static void assertNoSyntaxErrors(String title, Module module)
    {
        var node = NodeModelUtils.findActualNodeFor(module);
        assertNotNull("No node model for " + title, node); //$NON-NLS-1$
        for (var leaf : node.getLeafNodes())
        {
            assertNull(title + ": syntax error at line " + leaf.getStartLine() + ": " + leaf.getSyntaxErrorMessage(), //$NON-NLS-1$ //$NON-NLS-2$
                leaf.getSyntaxErrorMessage());
        }
    }

    private static Environments environmentsOf(EObject object)
    {
        Environmental environmental = EcoreUtil2.getContainerOfType(object, Environmental.class);
        return environmental == null ? Environments.ALL : environmental.environments();
    }

    private static String environments(EObject object)
    {
        if (object instanceof Environmental environmental)
        {
            return String.valueOf(environmental.environments());
        }
        return "n/a"; //$NON-NLS-1$
    }

    private static String moduleOf(EObject object)
    {
        Module module = EcoreUtil2.getContainerOfType(object, Module.class);
        if (module == null)
        {
            return "n/a"; //$NON-NLS-1$
        }
        return name(module.getOwner()) + " (" + module.getModuleType() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String name(EObject object)
    {
        if (object instanceof NamedElement namedElement)
        {
            return namedElement.getName();
        }
        return object == null ? "null" : object.getClass().getSimpleName(); //$NON-NLS-1$
    }

    private static String className(EObject object)
    {
        return object == null ? "null" : object.getClass().getName(); //$NON-NLS-1$
    }

    private static void print(String format, Object... arguments)
    {
        System.out.println("[TPR] " + String.format(format, arguments)); //$NON-NLS-1$
    }
}
