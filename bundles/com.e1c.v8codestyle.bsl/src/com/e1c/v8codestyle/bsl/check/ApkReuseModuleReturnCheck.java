/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: malikov-pro
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check 00350 (reuse module return value)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BooleanLiteral;
import com._1c.g5.v8.dt.bsl.model.DateLiteral;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.NumberLiteral;
import com._1c.g5.v8.dt.bsl.model.Procedure;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com._1c.g5.v8.dt.bsl.model.UnaryExpression;
import com._1c.g5.v8.dt.bsl.model.UnaryOperation;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.ReturnValuesReuse;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка общих модулей с повторным использованием возвращаемых значений:
 * <ul>
 * <li>экспортная процедура бессмысленна — процедуры не возвращают значения;</li>
 * <li>функция, состоящая из единственного оператора «Возврат &lt;константа&gt;»,
 * где константа — строка, число, булево, дата или обращение к предопределённому
 * элементу (Справочники.X.Y и т.п.), кэширует данные, вычисление которых
 * быстрее, чем получение из кэша.</li>
 * </ul>
 * <p>
 * Перенос проверки АПК_00350 «Проверка возвращаемого значения для модулей
 * с повторным использованием». Упрощения относительно алгоритма АПК:
 * возвращаемое выражение со скобкой в исходном тексте не восстанавливается
 * по AST-модели (скобки — скрытые токены), поэтому проверяется первый
 * значащий токен после ключевого слова «Возврат»; знак числа учитывается
 * только как унарный +/- перед литералом. Дата-литерал не проверяется на
 * «только цифры» — это гарантирует синтаксический анализатор.
 *
 * @author malikov-pro
 */
public class ApkReuseModuleReturnCheck
    extends BasicCheck
{

    /** Идентификатор проверки (код АПК + краткий слаг). */
    public static final String CHECK_ID = "apk-00350-reuse-module-return"; //$NON-NLS-1$

    private static final Set<String> PREDEFINED_PROVIDERS =
        Set.of("справочники", //$NON-NLS-1$
            "планывидоврасчета", //$NON-NLS-1$
            "планывидовхарактеристик", //$NON-NLS-1$
            "планысчетов", //$NON-NLS-1$
            "catalogs", //$NON-NLS-1$
            "chartsofcalculationtypes", //$NON-NLS-1$
            "chartsofcharacteristictypes", //$NON-NLS-1$
            "chartsofaccounts"); //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkReuseModuleReturnCheck()
    {
        super();
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ApkReuseModuleReturnCheck_title)
            .description(Messages.ApkReuseModuleReturnCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof Method method))
        {
            return;
        }

        Module module = EcoreUtil2.getContainerOfType(method, Module.class);
        if (module == null || module.getModuleType() != ModuleType.COMMON_MODULE)
        {
            return;
        }
        EObject owner = module.getOwner();
        if (!(owner instanceof CommonModule commonModule))
        {
            // To support partially-inconsistent models
            return;
        }
        ReturnValuesReuse reuse = commonModule.getReturnValuesReuse();
        if (reuse == null || reuse == ReturnValuesReuse.DONT_USE)
        {
            return;
        }

        if (method instanceof Procedure procedure)
        {
            if (procedure.isExport())
            {
                resultAceptor.addIssue(Messages.ApkReuseModuleReturnCheck_Export_procedure_is_meaningless, method);
            }
            return;
        }

        if (!(method instanceof Function))
        {
            return;
        }
        checkFunction((Function)method, resultAceptor);
    }

    private void checkFunction(Function function, ResultAcceptor resultAceptor)
    {
        List<Statement> statements = function.getStatements();
        if (statements.size() != 1 || !(statements.get(0) instanceof ReturnStatement returnStatement))
        {
            return;
        }

        if (isParenthesizedExpression(returnStatement))
        {
            return;
        }

        Expression expression = returnStatement.getExpression();
        if (expression instanceof UnaryExpression unary
            && (unary.getOperation() == UnaryOperation.MINUS || unary.getOperation() == UnaryOperation.PLUS)
            && unary.getOperand() instanceof NumberLiteral)
        {
            // Знак числа («Возврат -1») алгоритмом АПК считается частью константы
            expression = unary.getOperand();
        }

        if (isConstantExpression(expression))
        {
            resultAceptor.addIssue(Messages.ApkReuseModuleReturnCheck_Function_returns_constant, returnStatement);
        }
    }

    /**
     * Константа примитивного типа либо предопределённый элемент:
     * строка (без экранированных кавычек), число, булево, дата,
     * «Справочники.X.Y» и аналогичные обращения к менеджерам
     * предопределённых элементов.
     */
    private boolean isConstantExpression(Expression expression)
    {
        if (expression instanceof StringLiteral stringLiteral)
        {
            // Строка с экранированными кавычками алгоритмом АПК не считается простой константой.
            // Строки литерала хранятся как сырой токен — с обрамляющими кавычками,
            // поэтому срезаем края всего литерала перед поиском кавычек внутри
            List<String> lines = stringLiteral.getLines();
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < lines.size(); i++)
            {
                String line = lines.get(i);
                if (i == 0 && line.startsWith("\"")) //$NON-NLS-1$
                {
                    line = line.substring(1);
                }
                if (i == lines.size() - 1 && line.endsWith("\"") && !line.isEmpty()) //$NON-NLS-1$
                {
                    line = line.substring(0, line.length() - 1);
                }
                content.append(line);
            }
            return content.indexOf("\"") < 0; //$NON-NLS-1$
        }
        if (expression instanceof NumberLiteral || expression instanceof BooleanLiteral
            || expression instanceof DateLiteral)
        {
            return true;
        }
        if (expression instanceof DynamicFeatureAccess top
            && top.getSource() instanceof DynamicFeatureAccess mid
            && mid.getSource() instanceof StaticFeatureAccess root)
        {
            String name = root.getName();
            return name != null && PREDEFINED_PROVIDERS.contains(name.toLowerCase(Locale.ROOT));
        }
        return false;
    }

    /**
     * Скобки — скрытые токены и не попадают в AST: проверяем первый
     * значащий лист после ключевого слова «Возврат».
     */
    private boolean isParenthesizedExpression(ReturnStatement returnStatement)
    {
        ICompositeNode node = NodeModelUtils.findActualNodeFor(returnStatement);
        if (node == null)
        {
            return false;
        }
        boolean keywordSeen = false;
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (leaf.isHidden())
            {
                continue;
            }
            if (!keywordSeen)
            {
                keywordSeen = true;
                continue;
            }
            return "(".equals(leaf.getText()); //$NON-NLS-1$
        }
        return false;
    }
}
