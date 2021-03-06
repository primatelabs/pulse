<#--
A table is used as there is no good way to have a div collapse to the
width of its contents.  Floating it works, but hurts other things.
-->
<table class="eform">
    <tr>
        <td id="${form.name}-status">
<#if actionErrors?exists && actionErrors?size &gt; 0>
            <ul id="${form.name}-action-errors" class="validation-error">
    <#list actionErrors as error>
                <li>${error?html}</li>
    </#list>
            </ul>
</#if>
        </td>
    </tr>
    <tr>
        <td class="form-cell" id="${form.id}"></td>
    </tr>
</table>

<#-- render form -->
<script type="text/javascript">
    Ext.form.Field.prototype.msgTarget = 'under';

    var ${form.name} = new Zutubi.form.FormPanel({
        method: 'POST',
<#if form.fileUpload>
        fileUpload: true,
</#if>
        ajax: ${form.ajax?string},
        readOnly: ${form.readOnly?string},
        defaultSubmitValue: '${form.defaultSubmit?js_string}',
        formName: '${form.name?js_string}',
<#if form.parameters.path?exists>
        path: '${form.parameters.path?js_string}',
</#if>
        waitMsgTarget: 'nested-layout',
        border: false,
        labelWidth: '-',
        items: [{
            xtype: 'hidden',
            id: '${form.name?js_string}.submitField',
            name: 'submitField',
            value: 'h'
        } <#if sessionToken?exists> , {
            xtype: 'hidden',
            id: '${form.name?js_string}.sessionToken',
            name: '${sessionTokenName}',
            value: '${sessionToken}'
        } </#if> ]
    });

    var bc;
    <#if !form.readOnly>
        <#list form.submitFields as submitField>
    bc = { text: <#if submitField.label?exists> '${submitField.label?i18n?js_string}' <#else> '${submitField.value?js_string}' </#if> };
            <#if form.displayMode?default(false)>
    bc.disabled = true;
            </#if>
    ${form.name}.addButton(bc, function() { ${form.name}.submitForm('${submitField.value?js_string}'); });
        </#list>
    </#if>
    
    <#include "/tove/xhtml/form-fields.ftl"/>
</script>
