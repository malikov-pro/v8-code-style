/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic CreateQueryInCycle
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.ForEachStatement;
import com._1c.g5.v8.dt.bsl.model.ForToStatement;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.WhileStatement;
import com._1c.g5.v8.dt.bsl.model.util.BslUtil;
import com._1c.g5.v8.dt.bsl.resource.TypesComputer;
import com._1c.g5.v8.dt.mcore.ContextDef;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.util.McoreUtil;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/**
 * Проверка: запрос или построитель (запроса/отчёта) создаётся или выполняется
 * в цикле. Каждый проход цикла отправляет новый запрос к СУБД — получайте все
 * данные одним запросом с условием {@code В (&Список)}.
 * <p>
 * Перенос диагностики BSL Language Server CreateQueryInCycle
 * (серьёзность MAJOR, тип PERFORMANCE). Замещает проверку {@code query-in-loop}.
 * Как и в LS:
 * <ul>
 * <li>замечание ставится на вызов метода выполнения ({@code Выполнить},
 * {@code ВыполнитьПакет} и другие методы {@code Выполнить*}) объекта,
 * тип которого — {@code Запрос}, {@code ПостроительЗапроса} или
 * {@code ПостроительОтчета};</li>
 * <li>объект мог быть создан до цикла и переиспользуется в нём либо создан
 * прямо в цикле — оба случая диагностируются;</li>
 * <li>проверяется тело цикла; предикат цикла «Пока» вычисляется на каждой
 * итерации и проверяется всегда;</li>
 * <li>выражения заголовка «Для Каждого ... Из» и «Для ... По» вычисляются один
 * раз при входе в цикл и проверяются только когда сам цикл вложен в другой
 * цикл;</li>
 * <li>бесконечный цикл «Пока Истина» не является исключением.</li>
 * </ul>
 * Quick fix не предусмотрен: модификация запроса (условие {@code В (&Список)})
 * требует решения пользователя.
 *
 * @author malikov-pro
 */
public class CreateQueryInCycleCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "create-query-in-cycle"; //$NON-NLS-1$

    private static final String RU_EXECUTE_METHOD_PREFIX = "Выполнить"; //$NON-NLS-1$

    private static final String EN_EXECUTE_METHOD_PREFIX = "Execute"; //$NON-NLS-1$

    private static final Set<String> QUERY_TYPE_NAMES = Set.of(IEObjectTypeNames.QUERY,
        IEObjectTypeNames.QUERY_BUILDER, IEObjectTypeNames.REPORT_BUILDER);

    private final TypesComputer typesComputer;

    private final IRuntimeVersionSupport versionSupport;

    /**
     * Instantiates a new check.
     *
     * @param versionSupport the runtime version support service, cannot be {@code null}
     * @param typesComputer the types computer service, cannot be {@code null}
     */
    @Inject
    public CreateQueryInCycleCheck(IRuntimeVersionSupport versionSupport, TypesComputer typesComputer)
    {
        super();

        this.versionSupport = versionSupport;
        this.typesComputer = typesComputer;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        //@formatter:off
        builder.title(Messages.CreateQueryInCycle_title)
            .description(Messages.CreateQueryInCycle_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.PERFORMANCE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
        //@formatter:on
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (!(object instanceof Module module))
        {
            return;
        }

        Set<String> executeMethods = queryExecuteMethodNames(module);
        if (executeMethods.isEmpty())
        {
            return;
        }

        Collection<EObject> executeCallsInCycle = findExecuteCallsInCycles(module, executeMethods, monitor);
        for (EObject executeCall : executeCallsInCycle)
        {
            if (monitor.isCanceled())
            {
                return;
            }
            resultAceptor.addIssue(Messages.CreateQueryInCycle_execute_query_in_loop, executeCall,
                FEATURE_ACCESS__NAME);
        }
    }

    private Set<String> queryExecuteMethodNames(EObject object)
    {
        Set<String> executeMethods = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        IEObjectProvider provider = IEObjectProvider.Registry.INSTANCE.get(McorePackage.Literals.TYPE_ITEM,
            versionSupport.getRuntimeVersionOrDefault(object, Version.LATEST));
        EObject proxyType = provider.getProxy(IEObjectTypeNames.QUERY);
        if (!(proxyType instanceof Type))
        {
            return executeMethods;
        }

        Type queryType = (Type)EcoreUtil.resolve(proxyType, object);

        ContextDef contextDef = queryType.getContextDef();
        if (contextDef == null)
        {
            return executeMethods;
        }

        for (com._1c.g5.v8.dt.mcore.Method queryMethod : contextDef.allMethods())
        {
            if (queryMethod.getName().startsWith(EN_EXECUTE_METHOD_PREFIX)
                || queryMethod.getNameRu().startsWith(RU_EXECUTE_METHOD_PREFIX))
            {
                executeMethods.add(queryMethod.getName());
                executeMethods.add(queryMethod.getNameRu());
            }
        }

        return executeMethods;
    }

    private Collection<EObject> findExecuteCallsInCycles(Module module, Set<String> executeMethods,
        IProgressMonitor monitor)
    {
        Collection<EObject> result = new LinkedHashSet<>();

        for (LoopStatement loopStatement : EcoreUtil2.eAllOfType(module, LoopStatement.class))
        {
            if (monitor.isCanceled())
            {
                return result;
            }

            // Тело цикла выполняется на каждой итерации.
            for (Statement statement : loopStatement.getStatements())
            {
                collectExecuteCalls(statement, executeMethods, result);
            }

            // Предикат цикла «Пока» перевычисляется на каждой итерации.
            if (loopStatement instanceof WhileStatement whileStatement)
            {
                collectExecuteCalls(whileStatement.getPredicate(), executeMethods, result);
            }

            // Выражения заголовка «Для Каждого ... Из» и «Для ... = ... По»
            // вычисляются один раз при входе в цикл, поэтому проверяются,
            // только когда сам цикл вложен в другой цикл.
            if (isNestedLoop(loopStatement))
            {
                if (loopStatement instanceof ForEachStatement forEachStatement)
                {
                    collectExecuteCalls(forEachStatement.getCollection(), executeMethods, result);
                }
                else if (loopStatement instanceof ForToStatement forToStatement)
                {
                    collectExecuteCalls(forToStatement.getInitializer(), executeMethods, result);
                    collectExecuteCalls(forToStatement.getBound(), executeMethods, result);
                }
            }
        }

        return result;
    }

    private void collectExecuteCalls(EObject context, Set<String> executeMethods, Collection<EObject> result)
    {
        if (context == null)
        {
            return;
        }

        for (DynamicFeatureAccess executeCall : EcoreUtil2.eAllOfType(context, DynamicFeatureAccess.class))
        {
            String methodName = executeCall.getName();
            if (methodName != null && executeMethods.contains(methodName) && BslUtil.getInvocation(executeCall) != null
                && isQueryRelatedSource(executeCall.getSource()))
            {
                result.add(executeCall);
            }
        }
    }

    private boolean isNestedLoop(LoopStatement loopStatement)
    {
        for (EObject container = loopStatement.eContainer(); container != null; container = container.eContainer())
        {
            if (container instanceof LoopStatement)
            {
                return true;
            }
        }
        return false;
    }

    private boolean isQueryRelatedSource(Expression source)
    {
        if (source == null)
        {
            return false;
        }

        Environmental envs = EcoreUtil2.getContainerOfType(source, Environmental.class);
        if (envs == null)
        {
            return false;
        }

        List<TypeItem> sourceTypes = typesComputer.computeTypes(source, envs.environments());
        if (sourceTypes.isEmpty())
        {
            return false;
        }

        for (TypeItem sourceType : sourceTypes)
        {
            if (QUERY_TYPE_NAMES.contains(McoreUtil.getTypeName(sourceType)))
            {
                return true;
            }
        }

        return false;
    }

}
