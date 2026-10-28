/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.OperatorStyleCreator;
import com._1c.g5.v8.dt.bsl.model.FunctionStyleCreator;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Opt-in file-system audit. TempFilesDir remains an independent opt-in check. */
public class FileSystemAccessCheck extends BasicCheck
{
    public static final String CHECK_ID = "file-system-access"; //$NON-NLS-1$
    public static final String GLOBAL_METHODS = "globalMethods"; //$NON-NLS-1$
    public static final String NEW_EXPRESSION = "newExpression"; //$NON-NLS-1$

    public static final String DEFAULT_NEW_EXPRESSION =
        "File|Файл|xBase|HTMLWriter|ЗаписьHTML|HTMLReader|ЧтениеHTML" //$NON-NLS-1$
            + "|FastInfosetReader|ЧтениеFastInfoset|FastInfosetWriter|ЗаписьFastInfoset|XSLTransform|ПреобразованиеXSL" //$NON-NLS-1$
            + "|ZipFileWriter|ЗаписьZipФайла|ZipFileReader|ЧтениеZipФайла|TextReader|ЧтениеТекста|TextWriter|ЗаписьТекста" //$NON-NLS-1$
            + "|TextExtraction|ИзвлечениеТекста|BinaryData|ДвоичныеДанные|FileStream|ФайловыйПоток" //$NON-NLS-1$
            + "|FileStreamsManager|МенеджерФайловыхПотоков|DataWriter|ЗаписьДанных|DataReader|ЧтениеДанных"; //$NON-NLS-1$

    public static final String DEFAULT_GLOBAL_METHODS = "ЗначениеВФайл|ValueToFile|КопироватьФайл|FileCopy" //$NON-NLS-1$
        + "|ОбъединитьФайлы|MergeFiles|ПереместитьФайл|MoveFile|РазделитьФайл|SplitFile|СоздатьКаталог|CreateDirectory|" //$NON-NLS-1$
        + "УдалитьФайлы|DeleteFiles|КаталогПрограммы|BinDir|КаталогВременныхФайлов|TempFilesDir" //$NON-NLS-1$
        + "|КаталогДокументов|DocumentsDir|РабочийКаталогДанныхПользователя|UserDataWorkDir" //$NON-NLS-1$
        + "|НачатьПодключениеРасширенияРаботыСФайлами|BeginAttachingFileSystemExtension" //$NON-NLS-1$
        + "|НачатьУстановкуРасширенияРаботыСФайлами|BeginInstallFileSystemExtension" //$NON-NLS-1$
        + "|УстановитьРасширениеРаботыСФайлами|InstallFileSystemExtension" //$NON-NLS-1$
        + "|УстановитьРасширениеРаботыСФайламиАсинх|InstallFileSystemExtensionAsync" //$NON-NLS-1$
        + "|ПодключитьРасширениеРаботыСФайламиАсинх|AttachFileSystemExtensionAsync|" //$NON-NLS-1$
        + "КаталогВременныхФайловАсинх|TempFilesDirAsync|КаталогДокументовАсинх|DocumentsDirAsync" //$NON-NLS-1$
        + "|НачатьПолучениеКаталогаВременныхФайлов|BeginGettingTempFilesDir" //$NON-NLS-1$
        + "|НачатьПолучениеКаталогаДокументов|BeginGettingDocumentsDir" //$NON-NLS-1$
        + "|НачатьПолучениеРабочегоКаталогаДанныхПользователя|BeginGettingUserDataWorkDir" //$NON-NLS-1$
        + "|РабочийКаталогДанныхПользователяАсинх|UserDataWorkDirAsync" //$NON-NLS-1$
        + "|КопироватьФайлАсинх|CopyFileAsync|НайтиФайлыАсинх|FindFilesAsync|НачатьКопированиеФайла|BeginCopyingFile" //$NON-NLS-1$
        + "|НачатьПеремещениеФайла|BeginMovingFile|НачатьПоискФайлов|BeginFindingFiles" //$NON-NLS-1$
        + "|НачатьСозданиеДвоичныхДанныхИзФайла|BeginCreateBinaryDataFromFile" //$NON-NLS-1$
        + "|НачатьСозданиеКаталога|BeginCreatingDirectory" //$NON-NLS-1$
        + "|НачатьУдалениеФайлов|BeginDeletingFiles|ПереместитьФайлАсинх|MoveFileAsync" //$NON-NLS-1$
        + "|СоздатьДвоичныеДанныеИзФайлаАсинх|CreateBinaryDataFromFileAsync|СоздатьКаталогАсинх|CreateDirectoryAsync" //$NON-NLS-1$
        + "|УдалитьФайлыАсинх|DeleteFilesAsync"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.FileSystemAccessCheck_title)
            .description(Messages.FileSystemAccessCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.SECURITY)
            .extension(new OptInCheckExtension())
            .module().checkedObjectType(MODULE)
            .parameter(GLOBAL_METHODS, String.class, DEFAULT_GLOBAL_METHODS, Messages.FileSystemAccessCheck_globalMethods)
            .parameter(NEW_EXPRESSION, String.class, DEFAULT_NEW_EXPRESSION, Messages.FileSystemAccessCheck_newExpression);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        Pattern methods = pattern(parameters, GLOBAL_METHODS, DEFAULT_GLOBAL_METHODS);
        Pattern constructors = pattern(parameters, NEW_EXPRESSION, DEFAULT_NEW_EXPRESSION);
        // One module traversal: regexes compile once per branch, not once per call.
        var contents = module.eAllContents();
        while (contents.hasNext() && !monitor.isCanceled())
        {
            var item = contents.next();
            if (methods != null && item instanceof Invocation call
                && call.getMethodAccess() instanceof StaticFeatureAccess access
                && access.getName() != null && methods.matcher(access.getName()).matches())
            {
                acceptor.addIssue(Messages.FileSystemAccessCheck_message, access, FEATURE_ACCESS__NAME);
            }
            else if (constructors != null
                && (item instanceof OperatorStyleCreator || item instanceof FunctionStyleCreator))
            {
                String name = AuditConstructorName.of(item);
                if (name != null && constructors.matcher(name).matches())
                {
                    acceptor.addIssue(Messages.FileSystemAccessCheck_message, item);
                }
            }
        }
    }

    private static Pattern pattern(ICheckParameters parameters, String name, String defaultValue)
    {
        String value;
        try
        {
            value = parameters.getString(name);
        }
        catch (WrongParameterException exception)
        {
            value = defaultValue;
        }
        if (value == null)
        {
            return null;
        }
        try
        {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        catch (PatternSyntaxException exception)
        {
            BslPlugin.log(BslPlugin.createWarningStatus(String.format(
                "Check %s: invalid %s regex: %s", CHECK_ID, name, exception.getMessage()))); //$NON-NLS-1$
            return null;
        }
    }
}
