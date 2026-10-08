/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_00460
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BslContextDef;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.Procedure;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com._1c.g5.v8.dt.core.platform.IConfigurationAware;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.v8codestyle.bsl.ModuleStructureSection;

/**
 * Common helpers of the АПК_00460 checks: the overridable common module is
 * a common module whose name contains «Переопределяемый» (English
 * «Override»/«Overridable») in any case, as in the APK algorithm.
 *
 * @author malikov-pro
 */
final class OverridableModuleUtil
{

    private static final String RU_NAME_PART_UPPER = "ПЕРЕОПРЕДЕЛЯЕМЫЙ"; //$NON-NLS-1$
    private static final String EN_NAME_PART_UPPER = "OVERRIDABLE"; //$NON-NLS-1$
    private static final String EN_NAME_PART_ALT_UPPER = "OVERRIDE"; //$NON-NLS-1$

    private OverridableModuleUtil()
    {
        // Utility class, no instances
    }

    /**
     * Проверяет, что модуль — переопределяемый общий модуль: общий модуль,
     * в имени которого содержится «Переопределяемый»/«Override» (в любом
     * регистре), как в алгоритме АПК.
     *
     * @param module the module to test, may be {@code null}
     * @return true, if the module is an overridable common module
     */
    static boolean isOverridableCommonModule(Module module)
    {
        if (module == null || module.getModuleType() != ModuleType.COMMON_MODULE)
        {
            return false;
        }
        String name = getOwnerName(module);
        return isOverridableName(name);
    }

    /**
     * Проверяет, что имя содержит «Переопределяемый»/«Override»
     * (в любом регистре).
     */
    static boolean isOverridableName(String name)
    {
        if (name == null)
        {
            return false;
        }
        String upperName = name.toUpperCase(Locale.ROOT);
        return upperName.contains(RU_NAME_PART_UPPER) || upperName.contains(EN_NAME_PART_UPPER)
            || upperName.contains(EN_NAME_PART_ALT_UPPER);
    }

    /**
     * Возвращает имя объекта-владельца модуля (для общего модуля — имя самого
     * модуля); {@code null}, если владелец недоступен.
     */
    static String getOwnerName(Module module)
    {
        EObject owner = module.getOwner();
        if (owner == null)
        {
            return null;
        }
        if (owner.eIsProxy())
        {
            owner = EcoreUtil.resolve(owner, module);
        }
        return owner instanceof MdObject mdObject ? mdObject.getName() : null;
    }

    // ---------------------------------------------------------------------------------
    // Помощники подпунктов 1–4: переопределяемая процедура сводится к вызову оригинала
    // ---------------------------------------------------------------------------------

    /**
     * Проверяет, что метод — кандидат на переопределение: экспортная
     * процедура с именем (функции и неэкспортные методы проверяются
     * другими проверками АПК_00460).
     *
     * @param method the method to test, may be {@code null}
     * @return true, if the method is an export procedure with a name
     */
    static boolean isForwardingProcedureCandidate(Method method)
    {
        return method != null && method instanceof Procedure && method.isExport() && method.getName() != null;
    }

    /**
     * Проверяет, что метод устарел: помечен комментарием
     * «Устарела.»/«Deprecated.» (признак контекста модуля) либо размещён
     * в области «УстаревшиеПроцедурыИФункции»/«Deprecated». Устаревшие
     * процедуры переопределяемого модуля не проверяются, как в АПК.
     *
     * @param module the module containing the method, may be {@code null}
     * @param method the method to test, may be {@code null}
     * @return true, if the method is deprecated
     */
    static boolean isDeprecatedMethod(Module module, Method method)
    {
        if (method == null)
        {
            return false;
        }
        if (isInDeprecatedRegion(method))
        {
            return true;
        }
        if (module == null || !(module.getContextDef() instanceof BslContextDef contextDef))
        {
            return false;
        }
        return contextDef.allMethods()
            .stream()
            .filter(m -> method.getName() != null && method.getName().equals(m.getName()))
            .anyMatch(m -> m instanceof BslContextDefMethod defMethod && defMethod.isDeprecated());
    }

    private static boolean isInDeprecatedRegion(Method method)
    {
        RegionPreprocessor region = getFirstParentRegion(method);
        if (region == null || region.getName() == null)
        {
            return false;
        }
        for (String deprecatedName : ModuleStructureSection.DEPRECATED_REGION.getNames())
        {
            if (deprecatedName.equalsIgnoreCase(region.getName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает ближайшую область-родитель метода или {@code null}.
     */
    static RegionPreprocessor getFirstParentRegion(Method method)
    {
        EObject parent = method.eContainer();
        while (parent != null)
        {
            if (parent instanceof RegionPreprocessor region)
            {
                return region;
            }
            parent = parent.eContainer();
        }
        return null;
    }

    /**
     * Возвращает конфигурацию проекта, в котором находится объект, или
     * {@code null}, если проект не конфигурация (например, расширение
     * без доступа к конфигурации).
     *
     * @param projectManager the V8 project manager service, cannot be {@code null}
     * @param context the context object, cannot be {@code null}
     * @return the configuration or {@code null}
     */
    static Configuration getConfiguration(IV8ProjectManager projectManager, EObject context)
    {
        IV8ProjectManager projectManagerToUse = projectManager;
        if (projectManagerToUse == null)
        {
            return null;
        }
        IV8Project project = projectManagerToUse.getProject(context);
        if (project instanceof IConfigurationAware configurationAware)
        {
            return configurationAware.getConfiguration();
        }
        return null;
    }

    /**
     * Ищет общий модуль конфигурации по имени (без учёта регистра), как
     * таблица общих модулей в алгоритме АПК.
     *
     * @param configuration the configuration, may be {@code null}
     * @param moduleName the module name, may be {@code null}
     * @return the common module or {@code null}
     */
    static CommonModule findCommonModule(Configuration configuration, String moduleName)
    {
        if (configuration == null || moduleName == null)
        {
            return null;
        }
        for (CommonModule commonModule : configuration.getCommonModules())
        {
            String name = commonModule.getName();
            if (name != null && name.equalsIgnoreCase(moduleName))
            {
                return commonModule;
            }
        }
        return null;
    }

    /**
     * Возвращает модуль БСП общего модуля (null-безопасно).
     *
     * @param commonModule the common module, may be {@code null}
     * @return the BSL module or {@code null}
     */
    static Module getModuleOf(CommonModule commonModule)
    {
        if (commonModule == null)
        {
            return null;
        }
        Module module = commonModule.getModule();
        if (module == null)
        {
            return null;
        }
        if (module.eIsProxy())
        {
            module = (Module)EcoreUtil.resolve(module, commonModule);
            if (module.eIsProxy())
            {
                return null;
            }
        }
        return module;
    }

    /**
     * Ищет метод с именем (без учёта регистра) в модуле.
     *
     * @param module the module, may be {@code null}
     * @param methodName the method name, may be {@code null}
     * @return the method or {@code null}
     */
    static Method findMethodInModule(Module module, String methodName)
    {
        if (module == null || methodName == null)
        {
            return null;
        }
        for (Method method : module.allMethods())
        {
            String name = method.getName();
            if (name != null && name.equalsIgnoreCase(methodName))
            {
                return method;
            }
        }
        return null;
    }

    /**
     * Проверяет наличие «парного» метода: метода с тем же именем
     * (без учёта регистра) в ДРУГОМ общем модуле конфигурации. Упрощение
     * алгоритма АПК: парный метод ищется глобально по всем общим модулям,
     * фильтр подсистем не воспроизводится.
     *
     * @param configuration the configuration, may be {@code null}
     * @param ownerModule the module being checked, may be {@code null}
     * @param methodName the method name, may be {@code null}
     * @return true, if a same-named method exists in another common module
     */
    static boolean hasPairMethod(Configuration configuration, Module ownerModule, String methodName)
    {
        if (configuration == null || methodName == null)
        {
            return false;
        }
        String ownerName = ownerModule == null ? null : getOwnerName(ownerModule);
        for (CommonModule commonModule : configuration.getCommonModules())
        {
            String name = commonModule.getName();
            if (name != null && name.equalsIgnoreCase(ownerName))
            {
                continue;
            }
            if (findMethodInModule(getModuleOf(commonModule), methodName) != null)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Результат разбора оператора-вызова: вызываемый общий модуль
     * (существующий в конфигурации) и имя вызываемого метода.
     */
    static final class ForwardingCall
    {
        final CommonModule module;
        final String methodName;

        ForwardingCall(CommonModule module, String methodName)
        {
            this.module = module;
            this.methodName = methodName;
        }
    }

    /**
     * Возвращает выражение-вызов из оператора-вызова: в модели BSL
     * вызов без присваивания {@code Модуль.Метод(...);} разбирается
     * в левую часть оператора, правая часть при этом пуста (присваивание
     * {@code Переменная = Вызов(...);} разбирается как
     * «левая = правая»).
     *
     * @param statement the statement, may be {@code null}
     * @return the invocation expression or {@code null} if the statement is not a call statement
     */
    static Invocation getCallExpression(Statement statement)
    {
        if (!(statement instanceof SimpleStatement simpleStatement) || simpleStatement.getRight() != null)
        {
            return null;
        }
        return simpleStatement.getLeft() instanceof Invocation invocation ? invocation : null;
    }

    /**
     * Разбирает оператор-вызов и определяет вызываемый общий модуль
     * и имя вызываемого метода. В модели BSL вызов
     * {@code Идентификатор.Метод(...)} разбирается как
     * {@code Invocation(methodAccess = DynamicFeatureAccess(источник))}.
     * Распознаются формы (как в АПК после замены неявных вызовов):
     * <ul>
     * <li>прямой вызов {@code <Модуль>.<Метод>(...)}, где источник доступа —
     * идентификатор существующего общего модуля конфигурации;</li>
     * <li>неявный вызов {@code ОбщегоНазначения.ОбщийМодуль("Модуль").<Метод>(...)}
     * (англ. {@code Common.CommonModule("Module").<Method>(...)});</li>
     * <li>вызов через переменную {@code <Переменная>.<Метод>(...)}, которой
     * в этом же методе присвоен {@code ОбщегоНазначения.ОбщийМодуль("Модуль")}.</li>
     * </ul>
     *
     * @param invocation the invocation expression of the statement, may be {@code null}
     * @param configuration the configuration, may be {@code null}
     * @param containerMethod the method containing the statement, may be {@code null}
     * @return the forwarding call info or {@code null} if the statement is not a call of a common module method
     */
    static ForwardingCall resolveForwardingCall(Invocation invocation, Configuration configuration,
        Method containerMethod)
    {
        if (invocation == null || invocation.getMethodAccess() == null)
        {
            return null;
        }
        String methodName = invocation.getMethodAccess().getName();
        if (methodName == null)
        {
            return null;
        }
        if (!(invocation.getMethodAccess() instanceof DynamicFeatureAccess dynamicAccess))
        {
            return null;
        }
        Expression receiver = dynamicAccess.getSource();
        // Форма: ОбщегоНазначения.ОбщийМодуль("Модуль").Метод(...)
        if (receiver instanceof Invocation receiverInvocation)
        {
            CommonModule module = resolveCommonModuleGetter(receiverInvocation, configuration);
            return module == null ? null : new ForwardingCall(module, methodName);
        }
        // Формы: Модуль.Метод(...) и Переменная.Метод(...),
        // где Переменная = ОбщегоНазначения.ОбщийМодуль("Модуль")
        if (receiver instanceof StaticFeatureAccess receiverAccess)
        {
            String receiverName = receiverAccess.getName();
            CommonModule module = findCommonModule(configuration, receiverName);
            if (module != null)
            {
                return new ForwardingCall(module, methodName);
            }
            module = resolveVariableModule(containerMethod, receiverName, configuration);
            if (module != null)
            {
                return new ForwardingCall(module, methodName);
            }
        }
        return null;
    }

    /**
     * Распознаёт вызов {@code ОбщегоНазначения.ОбщийМодуль("Модуль")}
     * (англ. {@code Common.CommonModule("Module")}): имена метода и
     * владельца сверяются с константами (как текстовый шаблон в АПК),
     * общий модуль возвращается по строковому литералу, если он существует
     * в конфигурации.
     */
    private static CommonModule resolveCommonModuleGetter(Invocation invocation, Configuration configuration)
    {
        if (invocation == null || !(invocation.getMethodAccess() instanceof DynamicFeatureAccess dynamicAccess)
            || !(dynamicAccess.getSource() instanceof StaticFeatureAccess ownerAccess))
        {
            return null;
        }
        String getterName = dynamicAccess.getName();
        if (!"ОбщийМодуль".equalsIgnoreCase(getterName) && !"CommonModule".equalsIgnoreCase(getterName))
        {
            return null;
        }
        String ownerName = ownerAccess.getName();
        if (!"ОбщегоНазначения".equalsIgnoreCase(ownerName) && !"Common".equalsIgnoreCase(ownerName))
        {
            return null;
        }
        if (invocation.getParams().size() != 1 || !(invocation.getParams().get(0) instanceof StringLiteral literal))
        {
            return null;
        }
        return findCommonModule(configuration, literalText(literal));
    }

    private static String literalText(StringLiteral literal)
    {
        StringBuilder moduleName = new StringBuilder();
        for (String line : literal.getLines())
        {
            moduleName.append(line);
        }
        // Строки литерала включают кавычки — убираем их, как в алгоритме АПК
        return moduleName.toString().replace("\"", "").strip();
    }

    /**
     * Ищет в методе присваивание {@code Переменная = ОбщегоНазначения.ОбщийМодуль("Модуль")}
     * и возвращает общий модуль.
     */
    private static CommonModule resolveVariableModule(Method containerMethod, String variableName,
        Configuration configuration)
    {
        if (containerMethod == null || variableName == null)
        {
            return null;
        }
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(containerMethod, SimpleStatement.class))
        {
            if (statement.getLeft() instanceof StaticFeatureAccess left && left.getName() != null
                && left.getName().equalsIgnoreCase(variableName)
                && statement.getRight() instanceof Invocation initializer)
            {
                CommonModule module = resolveCommonModuleGetter(initializer, configuration);
                if (module != null)
                {
                    return module;
                }
            }
        }
        return null;
    }

    /**
     * Возвращает текст комментария (возможно, многострочного), размещённого
     * непосредственно над объявлением метода; между комментарием и объявлением
     * допускаются пустые строки. {@code null}, если комментария нет.
     */
    static String getPrecedingComment(Module module, Method method)
    {
        INode moduleNode = NodeModelUtils.findActualNodeFor(module);
        INode methodNode = NodeModelUtils.findActualNodeFor(method);
        if (moduleNode == null || methodNode == null)
        {
            return null;
        }
        String moduleText = moduleNode.getText();
        int methodLine = lineByOffset(moduleText, methodNode.getTotalOffset());
        if (methodLine <= 0)
        {
            return null;
        }
        String[] lines = moduleText.split("\n"); //$NON-NLS-1$
        List<String> commentLines = new ArrayList<>();
        for (int i = methodLine - 1; i >= 0; i--)
        {
            String line = lines[i].replace("\r", "").strip(); //$NON-NLS-1$ //$NON-NLS-2$
            if (line.isEmpty())
            {
                continue;
            }
            if (line.startsWith("//")) //$NON-NLS-1$
            {
                commentLines.add(line);
                continue;
            }
            break;
        }
        if (commentLines.isEmpty())
        {
            return null;
        }
        Collections.reverse(commentLines);
        return String.join("\n", commentLines); //$NON-NLS-1$
    }

    private static int lineByOffset(String text, int offset)
    {
        int line = 0;
        int limit = Math.min(offset, text.length());
        for (int i = 0; i < limit; i++)
        {
            if (text.charAt(i) == '\n')
            {
                line++;
            }
        }
        return line;
    }

    /**
     * Нормализует текст комментария, как в алгоритме АПК: удаляются
     * символы «/», пробелы, табуляция и переводы строк.
     */
    static String normalizeComment(String comment)
    {
        if (comment == null)
        {
            return ""; //$NON-NLS-1$
        }
        return comment.replace("/", "") //$NON-NLS-1$ //$NON-NLS-2$
            .replaceAll("\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Проверяет, что у метода парного модуля есть комментарий-отсылка
     * «См. &lt;ИмяПереопределяемогоМодуля&gt;.&lt;ИмяПроцедуры&gt;.»
     * (регистр «См.» и имён значим, как в алгоритме АПК).
     */
    static boolean hasSeeComment(Module pairModule, Method pairMethod, String overridableModuleName,
        String methodName)
    {
        if (overridableModuleName == null || methodName == null)
        {
            return false;
        }
        String comment = getPrecedingComment(pairModule, pairMethod);
        if (comment == null)
        {
            return false;
        }
        String template = "См." + overridableModuleName + "." + methodName; //$NON-NLS-1$ //$NON-NLS-2$
        return normalizeComment(comment).startsWith(template);
    }

    /**
     * Находит диапазоны смещений кода между комментариями
     * «// _Демо начало примера» и «// _Демо конец примера» — такой код
     * не проверяется (русские и английские варианты, без учёта регистра).
     */
    static List<int[]> findDemoExampleRanges(Module module)
    {
        INode moduleNode = NodeModelUtils.findActualNodeFor(module);
        if (moduleNode == null)
        {
            return List.of();
        }
        List<int[]> ranges = new ArrayList<>();
        int rangeStart = -1;
        for (ILeafNode leaf : moduleNode.getLeafNodes())
        {
            String stripped = leaf.getText().strip();
            if (!stripped.startsWith("//")) //$NON-NLS-1$
            {
                continue;
            }
            String upper = stripped.toUpperCase(Locale.ROOT).replace('Ё', 'Е');
            boolean isBegin = (upper.contains("_ДЕМО") && upper.contains("НАЧАЛО")) //$NON-NLS-1$ //$NON-NLS-2$
                || (upper.contains("_DEMO") && upper.contains("START")); //$NON-NLS-1$ //$NON-NLS-2$
            boolean isEnd = (upper.contains("_ДЕМО") && upper.contains("КОНЕЦ")) //$NON-NLS-1$ //$NON-NLS-2$
                || (upper.contains("_DEMO") && upper.contains("END")); //$NON-NLS-1$ //$NON-NLS-2$
            if (isBegin)
            {
                if (rangeStart < 0)
                {
                    rangeStart = leaf.getTotalEndOffset();
                }
            }
            else if (isEnd && rangeStart >= 0)
            {
                ranges.add(new int[] { rangeStart, leaf.getTotalOffset() });
                rangeStart = -1;
            }
        }
        return ranges;
    }

    /**
     * Проверяет, что объект находится внутри диапазона демо-примера.
     * Узел оператора включает ведущие скрытые лексемы (комментарии,
     * переводы строк), поэтому берётся первый содержательный лист.
     */
    static boolean isInDemoExample(EObject object, List<int[]> demoRanges)
    {
        if (demoRanges.isEmpty())
        {
            return false;
        }
        INode node = NodeModelUtils.findActualNodeFor(object);
        if (node == null)
        {
            return false;
        }
        int offset = getFirstContentOffset(node);
        for (int[] range : demoRanges)
        {
            if (offset >= range[0] && offset < range[1])
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает смещение первого содержательного листа узла — после
     * ведущих скрытых лексем (whitespace, комментарии).
     */
    private static int getFirstContentOffset(INode node)
    {
        Iterable<ILeafNode> leaves = node instanceof ICompositeNode composite
            ? composite.getLeafNodes()
            : List.of((ILeafNode)node);
        for (ILeafNode leaf : leaves)
        {
            String leafText = leaf.getText().strip();
            if (leafText.isEmpty() || leafText.startsWith("//")) //$NON-NLS-1$
            {
                continue;
            }
            return leaf.getTotalOffset();
        }
        return node.getTotalOffset();
    }
}
