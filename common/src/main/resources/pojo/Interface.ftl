${pojo.getPackageDeclaration()}
// Generated ${date} by Hibernate Tools ${version} for AlgoTrader

<#assign classbody>
<#include "InterfaceTypeDeclaration.ftl"/> {

<#include "InterfacePropertyAccessors.ftl"/>

<#include "InterfaceExtraClassCode.ftl"/>
}
</#assign>

${pojo.generateImports()}
${classbody}

