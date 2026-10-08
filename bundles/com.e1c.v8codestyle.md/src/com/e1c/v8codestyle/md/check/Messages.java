/*******************************************************************************
 * Copyright (C) 2021, 1C-Soft LLC and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     1C-Soft LLC - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import org.eclipse.osgi.util.NLS;

/**
 * @author Dmitriy Marmyshev
 *
 */
final class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
    public static String DbObjectRefNonRefTypesCheck_Description;
    public static String DbObjectRefNonRefTypesCheck_Ref_and_other;
    public static String DbObjectRefNonRefTypesCheck_Title;
    public static String MdObjectAttributeCommentCheck_Attribute_list;
    public static String MdObjectAttributeCommentCheck_Check_catalogs_param;
    public static String MdObjectAttributeCommentCheck_Check_documents_param;
    public static String MdObjectAttributeCommentCheck_Is_compound_type;
    public static String MdObjectAttributeCommentCheck_Multiline_edit_is_not_enabled;
    public static String MdObjectAttributeCommentCheck_Not_a_String;
    public static String MdObjectAttributeCommentCheck_String_is_not_unlimited;
    public static String MdObjectAttributeCommentCheck_description;
    public static String MdObjectAttributeCommentCheck_message;
    public static String MdObjectAttributeCommentCheck_title;
    public static String MdObjectAttributeCommentNotExist_description;
    public static String MdObjectAttributeCommentNotExist_Md_Object_attribute_Comment_does_not_exist;
    public static String MdObjectAttributeCommentNotExist_Param_Check_Catalogs;
    public static String MdObjectAttributeCommentNotExist_Param_Check_Documents;
    public static String MdObjectAttributeCommentNotExist_Param_Attribute_name_list;
    public static String MdObjectAttributeCommentNotExist_title;
    public static String MdObjectNameWithoutSuffix_Name_suffix_list_title;
    public static String DbObjectAnyRefCheck_AnyRef;
    public static String DbObjectAnyRefCheck_Description;
    public static String DbObjectAnyRefCheck_Title;
    public static String DbObjectMaxNumberLengthCheck_description;
    public static String DbObjectMaxNumberLengthCheck_message;
    public static String DbObjectMaxNumberLengthCheck_parameter;
    public static String DbObjectMaxNumberLengthCheck_title;
    public static String DbObjectCodeLengthCheck_description;
    public static String DbObjectCodeLengthCheck_message;
    public static String DbObjectCodeLengthCheck_title;
    public static String ExtensionMdObjectNamePrefixCheck_Description;
    public static String ExtensionMdObjectNamePrefixCheck_Object_0_should_have_1_prefix;
    public static String ExtensionMdObjectNamePrefixCheck_Title;
    public static String MdObjectNameLength_description;
    public static String MdObjectNameLength_Maximum_name_length_description;
    public static String MdObjectNameLength_message;
    public static String MdObjectNameLength_title;
    public static String MdObjectNameUnallowedLetterCheck_description;
    public static String MdObjectNameUnallowedLetterCheck_Ru_locale_unallowed_letter_used_for_name_synonym_or_comment;
    public static String MdObjectNameUnallowedLetterCheck_title;
    public static String MdListObjectPresentationCheck_decription;
    public static String MdListObjectPresentationCheck_Neither_Object_presentation_nor_List_presentation_is_not_filled;
    public static String MdListObjectPresentationCheck_title;
    public static String MdOwnerAttributeSynonymEmpty_Title;
    public static String MdOwnerAttributeSynonymEmpty_Description;
    public static String MdOwnerAttributeSynonymEmpty_owner_ErrorMessage;
    public static String MdOwnerAttributeSynonymEmpty_parent_ErrorMessage;
    public static String UnsafePasswordStorageCheck_Avoid_storing_password_in_infobase;
    public static String UnsafePasswordStorageCheck_Avoid_storing_password_in_infobase_description;
    public static String UnsafePasswordStorageCheck_Avoid_storing_password_in_infobase_error;
    public static String DbObjectStringAllowedLengthCheck_description;
    public static String DbObjectStringAllowedLengthCheck_message;
    public static String DbObjectStringAllowedLengthCheck_title;
    public static String DbObjectUnlimitedStringCheck_description;
    public static String DbObjectUnlimitedStringCheck_message;
    public static String DbObjectUnlimitedStringCheck_title;
    public static String MdObjectDeleteSynonymPrefixCheck_title;
    public static String MdObjectDeleteSynonymPrefixCheck_description;
    public static String MdObjectDeleteSynonymPrefixCheck_Name_prefix_without_synonym_prefix;
    public static String MdObjectDeleteSynonymPrefixCheck_Synonym_prefix_without_name_prefix;
    public static String MdObjectYoLetterCheck_title;
    public static String MdObjectYoLetterCheck_description;
    public static String MdObjectYoLetterCheck_Name_contains_yo;
    public static String MdObjectYoLetterCheck_Synonym_contains_yo;
    public static String MdObjectYoLetterCheck_Comment_contains_yo;
    public static String MdObjectFrenchQuotesCheck_title;
    public static String MdObjectFrenchQuotesCheck_description;
    public static String MdObjectFrenchQuotesCheck_message;
    public static String MdObjectFrenchQuotesCheck_Synonym;
    public static String MdObjectFrenchQuotesCheck_Comment;
    public static String MdObjectFrenchQuotesCheck_Tooltip;
    public static String MdAttributeHintMatchSynonymCheck_description;
    public static String MdAttributeHintMatchSynonymCheck_message;
    public static String MdAttributeHintMatchSynonymCheck_title;
    public static String MdTemplateLangPostfixCheck_title;
    public static String MdTemplateLangPostfixCheck_description;
    public static String MdTemplateLangPostfixCheck_Template_name_without_language_postfix;
    public static String MdTemplateEncodingCheck_title;
    public static String MdTemplateEncodingCheck_description;
    public static String MdTemplateEncodingCheck_Non_utf8_encoding;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
