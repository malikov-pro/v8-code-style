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
 *     malikov-pro - port of the BSL Language Server diagnostic UsingSynchronousCalls
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.common.Symbols;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.Pragma;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.lcore.util.CaseInsensitiveString;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.SynchronousPlatformExtensionAndAddInCallUseMode;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/**
 * Проверка: использование синхронных вызовов в модулях, работающих на клиенте,
 * когда конфигурация ограничивает синхронные вызовы. Синхронные методы глобального
 * контекста (Вопрос, ОткрытьФормуМодально, Предупреждение, ПоместитьФайл,
 * НайтиФайлы, ЗапуститьПриложение и др.) не работают в веб-клиенте — используйте
 * асинхронные аналоги (ПоказатьВопрос, ОткрытьФорму, НачатьПомещениеФайла и т.д.).
 * <p>
 * Перенос диагностики BSL Language Server UsingSynchronousCalls
 * (тип CODE_SMELL, серьёзность MAJOR). Как и в LS:
 * <ul>
 * <li>не диагностируется, если в конфигурации разрешено использование
 * синхронных вызовов (режим USE);</li>
 * <li>проверяются только клиентские модули: модуль управляемого приложения,
 * обычного приложения, модуль команды, модуль формы и клиентские общие модули —
 * прочие модули строго серверные;</li>
 * <li>вызовы в методах с директивами &amp;НаСервере и &amp;НаСервереБезКонтекста
 * пропускаются.</li>
 * </ul>
 * Quick fix не предусмотрен: асинхронные аналоги меняют структуру кода
 * (обработчик оповещения).
 *
 * @author malikov-pro
 */
public class UsingSynchronousCallsCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "using-synchronous-calls"; //$NON-NLS-1$

    private static final Set<CaseInsensitiveString> SERVER_COMPILER_DIRECTIVES = Set.of(
        new CaseInsensitiveString(Symbols.AT_SERVER_RUS),
        new CaseInsensitiveString(Symbols.AT_SERVER_INTNL),
        new CaseInsensitiveString(Symbols.AT_SERVER_NO_CONTEXT_RUS),
        new CaseInsensitiveString(Symbols.AT_SERVER_NO_CONTEXT_INTNL));

    private static final Map<String, String> PAIR_METHODS = Map.ofEntries(
        Map.entry("вопрос", "ПоказатьВопрос"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("doquerybox", "ShowQueryBox"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("открытьформумодально", "ОткрытьФорму"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("openformmodal", "OpenForm"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("открытьзначение", "ПоказатьЗначение"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("openvalue", "ShowValue"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("предупреждение", "ПоказатьПредупреждение"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("domessagebox", "ShowMessageBox"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("ввестидату", "ПоказатьВводДаты"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("inputdate", "ShowInputDate"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("ввестизначение", "ПоказатьВводЗначения"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("inputvalue", "ShowInputValue"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("ввестистроку", "ПоказатьВводСтроки"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("inputstring", "ShowInputString"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("ввестичисло", "ПоказатьВводЧисла"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("inputnumber", "ShowInputNumber"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("установитьвнешнююкомпоненту", "НачатьУстановкуВнешнейКомпоненты"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("installaddin", "BeginInstallAddIn"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("установитьрасширениеработысфайлами", "НачатьУстановкуРасширенияРаботыСФайлами"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("installfilesystemextension", "BeginInstallFileSystemExtension"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("установитьрасширениеработыскриптографией", "НачатьУстановкуРасширенияРаботыСКриптографией"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("installcryptoextension", "BeginInstallCryptoExtension"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("подключитьрасширениеработыскриптографией", "НачатьПодключениеРасширенияРаботыСКриптографией"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("attachcryptoextension", "BeginAttachingCryptoExtension"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("подключитьрасширениеработысфайлами", "НачатьПодключениеРасширенияРаботыСФайлами"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("attachfilesystemextension", "BeginAttachingFileSystemExtension"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("поместитьфайл", "НачатьПомещениеФайла"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("putfile", "BeginPutFile"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("копироватьфайл", "НачатьКопированиеФайла"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("filecopy", "BeginCopyingFile"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("переместитьфайл", "НачатьПеремещениеФайла"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("movefile", "BeginMovingFile"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("найтифайлы", "НачатьПоискФайлов"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("findfiles", "BeginFindingFiles"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("удалитьфайлы", "НачатьУдалениеФайлов"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("deletefiles", "BeginDeletingFiles"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("создатькаталог", "НачатьСозданиеКаталога"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("createdirectory", "BeginCreatingDirectory"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("каталогвременныхфайлов", "НачатьПолучениеКаталогаВременныхФайлов"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("tempfilesdir", "BeginGettingTempFilesDir"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("каталогдокументов", "НачатьПолучениеКаталогаДокументов"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("documentsdir", "BeginGettingDocumentsDir"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("рабочийкаталогданныхпользователя", "НачатьПолучениеРабочегоКаталогаДанныхПользователя"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("userdataworkdir", "BeginGettingUserDataWorkDir"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("получитьфайлы", "НачатьПолучениеФайлов"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("getfiles", "BeginGettingFiles"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("поместитьфайлы", "НачатьПомещениеФайлов"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("putfiles", "BeginPuttingFiles"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("запроситьразрешениепользователя", "НачатьЗапросРазрешенияПользователя"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("requestuserpermission", "BeginRequestingUserPermission"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("запуститьприложение", "НачатьЗапускПриложения"), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("runapp", "BeginRunningApplication")); //$NON-NLS-1$ //$NON-NLS-2$

    private final IConfigurationProvider configurationProvider;

    /**
     * Instantiates a new check.
     *
     * @param configurationProvider the configuration provider service, cannot be {@code null}.
     */
    @Inject
    public UsingSynchronousCallsCheck(IConfigurationProvider configurationProvider)
    {
        super();
        this.configurationProvider = configurationProvider;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UsingSynchronousCallsCheck_title)
            .description(Messages.UsingSynchronousCallsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Invocation invocation = (Invocation)object;
        if (!(invocation.getMethodAccess() instanceof StaticFeatureAccess methodAccess))
        {
            return;
        }

        Module module = EcoreUtil2.getContainerOfType(invocation, Module.class);
        if (module == null || !isClientModule(module))
        {
            return;
        }

        Configuration configuration = configurationProvider.getConfiguration(invocation);
        if (configuration != null && configuration.getSynchronousPlatformExtensionAndAddInCallUseMode()
            == SynchronousPlatformExtensionAndAddInCallUseMode.USE)
        {
            // synchronous calls are allowed in the configuration, nothing to diagnose
            return;
        }

        Method method = EcoreUtil2.getContainerOfType(invocation, Method.class);
        if (method != null && hasServerCompilerDirective(method))
        {
            // the method is compiled on the server, synchronous calls are allowed
            return;
        }

        String name = methodAccess.getName().toLowerCase(Locale.ROOT);
        String pairMethod = PAIR_METHODS.get(name);
        if (pairMethod == null)
        {
            return;
        }

        String message =
            MessageFormat.format(Messages.UsingSynchronousCallsCheck_message, methodAccess.getName(), pairMethod);
        resultAceptor.addIssue(message, methodAccess);
    }

    private static boolean isClientModule(Module module)
    {
        ModuleType moduleType = module.getModuleType();
        return switch (moduleType)
        {
            case ORDINARY_APP_MODULE, COMMAND_MODULE, FORM_MODULE, MANAGED_APP_MODULE -> true;
            case COMMON_MODULE -> isClientCommonModule(module);
            default -> false; // all other modules are strictly server ones
        };
    }

    private static boolean isClientCommonModule(Module module)
    {
        if (module.getOwner() instanceof CommonModule commonModule)
        {
            return commonModule.isClientManagedApplication() || commonModule.isClientOrdinaryApplication();
        }
        return false;
    }

    private static boolean hasServerCompilerDirective(Method method)
    {
        for (Pragma pragma : method.getPragmas())
        {
            if (SERVER_COMPILER_DIRECTIVES.contains(new CaseInsensitiveString(pragma.getSymbol())))
            {
                return true;
            }
        }
        return false;
    }
}
