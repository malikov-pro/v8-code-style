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
 *     malikov-pro - port of the АПК rule АПК_01146
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CONFIGURATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CONFIGURATION__COMMON_PICTURES;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.CommonPicture;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.ObjectBelonging;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.google.inject.Inject;

/**
 * Two or more common pictures of the configuration must not have the
 * identical content. The content of each common picture is the content of
 * its image file (or files) stored in the project next to the picture
 * descriptor; a hash sum (MD5) of the content is computed, and pictures with
 * the same hash sum are reported: every picture of a duplicate group is
 * reported and the message lists all pictures of the group. Pictures with
 * empty or not accessible content are not checked. The АПК rule 01146.
 * <p>
 * Pictures adopted in extension configurations are not checked.
 *
 * @author malikov-pro
 */
public class CommonPictureDuplicateCheck
    extends BasicCheck
{

    /** The check id of the АПК_01146 rule port. */
    public static final String CHECK_ID = "apk-01146-duplicate-pictures"; //$NON-NLS-1$

    private static final String SOURCE_FOLDER = "src"; //$NON-NLS-1$
    private static final String COMMON_PICTURES_FOLDER = "CommonPictures"; //$NON-NLS-1$
    private static final String MDO_FILE_SUFFIX = ".mdo"; //$NON-NLS-1$

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}
     */
    @Inject
    public CommonPictureDuplicateCheck(IV8ProjectManager v8ProjectManager)
    {
        super();
        this.v8ProjectManager = v8ProjectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.CommonPictureDuplicateCheck_title)
            .description(Messages.CommonPictureDuplicateCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .topObject(CONFIGURATION)
            .checkTop()
            .features(CONFIGURATION__COMMON_PICTURES);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof Configuration configuration))
        {
            return;
        }
        IV8Project project = v8ProjectManager.getProject(configuration);
        if (project instanceof IExtensionProject)
        {
            // The extension does not own the configuration picture assets.
            return;
        }

        Map<String, List<CommonPicture>> picturesByHash = new HashMap<>();
        for (CommonPicture picture : configuration.getCommonPictures())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (picture.getObjectBelonging() == ObjectBelonging.ADOPTED)
            {
                continue;
            }
            String hash = contentHash(picture, project);
            if (hash != null)
            {
                picturesByHash.computeIfAbsent(hash, key -> new ArrayList<>()).add(picture);
            }
        }

        for (List<CommonPicture> group : picturesByHash.values())
        {
            if (group.size() < 2)
            {
                continue;
            }
            for (CommonPicture picture : group)
            {
                if (monitor.isCanceled())
                {
                    return;
                }
                String duplicates = String.join(", ", namesOf(group, picture)); //$NON-NLS-1$
                resultAceptor.addIssue(MessageFormat.format(Messages.CommonPictureDuplicateCheck_Duplicate_pictures,
                    picture.getName(), duplicates), picture);
            }
        }
    }

    private static List<String> namesOf(List<CommonPicture> group, CommonPicture excluded)
    {
        List<String> names = new ArrayList<>(group.size());
        for (CommonPicture picture : group)
        {
            if (picture != excluded)
            {
                names.add(picture.getName());
            }
        }
        return names;
    }

    /**
     * Computes the hash sum of the picture content: the MD5 sum over the
     * content of every file of the picture folder (except the descriptor),
     * taken in the file name order. Returns {@code null} when the picture
     * has no name or its content is not accessible from the project.
     *
     * @param picture the common picture, cannot be {@code null}
     * @param project the project owning the configuration, cannot be {@code null}
     * @return the hash sum, or {@code null} when the content is not accessible
     */
    private String contentHash(CommonPicture picture, IV8Project project)
    {
        String name = picture.getName();
        if (name == null || name.isBlank())
        {
            return null;
        }
        IFolder folder = project.getProject().getFolder(SOURCE_FOLDER).getFolder(COMMON_PICTURES_FOLDER)
            .getFolder(name);
        if (!folder.isAccessible())
        {
            return null;
        }
        List<IFile> files;
        try
        {
            files = Arrays.stream(folder.members())
                .filter(IFile.class::isInstance)
                .map(IFile.class::cast)
                .filter(file -> !file.getName().equalsIgnoreCase(name + MDO_FILE_SUFFIX))
                .sorted(Comparator.comparing(IFile::getName))
                .toList();
        }
        catch (CoreException e)
        {
            // The picture folder is not readable - the picture is out of the scope.
            return null;
        }
        if (files.isEmpty())
        {
            return null;
        }
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
        }
        catch (NoSuchAlgorithmException e)
        {
            // Every Java platform must contain the MD5 algorithm - cannot happen.
            return null;
        }
        try (DigestOutputStream stream = new DigestOutputStream(OutputStream.nullOutputStream(), digest))
        {
            for (IFile file : files)
            {
                stream.write(file.getName().getBytes(StandardCharsets.UTF_8));
                try (InputStream content = file.getContents())
                {
                    content.transferTo(stream);
                }
            }
        }
        catch (CoreException | IOException e)
        {
            // The content is not readable - the picture is out of the scope.
            return null;
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
