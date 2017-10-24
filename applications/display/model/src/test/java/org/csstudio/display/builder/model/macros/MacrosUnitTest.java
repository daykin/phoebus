/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/** JUnit test of macro handling
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacrosUnitTest
{
    /** Test check for unresolved macros
     *  @throws Exception on error
     */
    @Test
    public void testCheck() throws Exception
    {
        assertThat(MacroHandler.containsMacros("Plain Text"), equalTo(false));
        assertThat(MacroHandler.containsMacros("${S}"), equalTo(true));
        assertThat(MacroHandler.containsMacros("This is $(S)"), equalTo(true));
        assertThat(MacroHandler.containsMacros("$(MACRO)"), equalTo(true));
        assertThat(MacroHandler.containsMacros("$(${MACRO})"), equalTo(true));
        assertThat(MacroHandler.containsMacros("Escaped \\$(S)"), equalTo(true));
        assertThat(MacroHandler.containsMacros("Escaped \\$(S) Used $(S)"), equalTo(true));
    }

    /** Test basic macro=value
     *  @throws Exception on error
     */
    @Test
    public void testMacros() throws Exception
    {
        final Macros macros = new Macros();
        macros.add("S", "BL7");
        macros.add("NAME", "Flint, Eugene");
        macros.add("TAB", "    ");
        macros.add("MACRO", "S");
        macros.add("traces[0].y_pv", "TheValueWaveform");

        assertThat(MacroHandler.replace(macros, "Plain Text"), equalTo("Plain Text"));
        assertThat(MacroHandler.replace(macros, "${S}"), equalTo("BL7"));
        assertThat(MacroHandler.replace(macros, "This is $(S)"), equalTo("This is BL7"));
        // Plain macro
        assertThat(MacroHandler.replace(macros, "$(MACRO)"), equalTo("S"));
        // $($(MACRO)) ->  $(S) -> BL7
        assertThat(MacroHandler.replace(macros, "$(${MACRO})"), equalTo("BL7"));
        assertThat(MacroHandler.replace(macros, "$(TAB)$(NAME)$(TAB)"), equalTo("    Flint, Eugene    "));
        assertThat(MacroHandler.replace(macros, "$(traces[0].y_pv)"), equalTo("TheValueWaveform"));

        // Escaped macros leave the $(..), i.e. remove the escape char
        assertThat(MacroHandler.replace(macros, "Escaped \\$(S)"), equalTo("Escaped $(S)"));
        assertThat(MacroHandler.replace(macros, "Escaped \\$(S) Used $(S)"), equalTo("Escaped $(S) Used BL7"));

        // If you want to keep a '\$' in the output, escape the escape
        assertThat(MacroHandler.replace(macros, "Escaped \\\\$(S)"), equalTo("Escaped \\$(S)"));
    }

    /** Test special cases
     *  @throws Exception on error
     */
    @Test
    public void testSpecials() throws Exception
    {
        MacroValueProvider macros = new Macros();
        System.out.println(macros);
        assertThat(macros.toString(), equalTo("[]"));

        assertThat(MacroHandler.replace(macros, "Plain Text"), equalTo("Plain Text"));
        assertThat(MacroHandler.replace(macros, "Nothing for ${S} <-- this one"), equalTo("Nothing for ${S} <-- this one"));
        assertThat(MacroHandler.replace(macros, "${NOT_CLOSED"), equalTo("${NOT_CLOSED"));
    }

    /**
     * Test macros with default values
     *
     * @throws Exception on error
     */
    @Test
    public void testDefaults() throws Exception
    {
        Macros macros = new Macros();
        macros.add("A", "a");
        System.out.println(macros);

        assertThat(MacroHandler.replace(macros, "Empty default ${S=} value"), equalTo("Empty default  value"));
        assertThat(MacroHandler.replace(macros, "Invalid-name default ${ S=X}"), equalTo("Invalid-name default ${ S=X}"));
        assertThat(MacroHandler.replace(macros, "Default ${S=X} value"), equalTo("Default X value"));
        assertThat(MacroHandler.replace(macros, "Default ${S = X + Y = Z} value"), equalTo("Default X + Y = Z value"));
        assertThat(MacroHandler.replace(macros, "Doesn't use default: ${S = $(A=z)}"), equalTo("Doesn't use default: a"));
    }


    /** Test recursive macro error
     *  @throws Exception on error
     */
    @Test
    public void testRecursion() throws Exception
    {
        final Macros macros = new Macros();
        macros.add("S", "$(S)");
        try
        {
            MacroHandler.replace(macros, "Never ending $(S)");
            fail("Didn't detect recursive macro");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(), containsString(/* [Rr] */ "ecursive"));
        }

        try
        {
            MacroHandler.replace(macros, "Recursive $(S=a) default");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(), containsString(/* [Rr] */ "ecursive"));
        }
    }

    @Test
    public void testXML() throws Exception
    {
        final Macros macros = new Macros();
        macros.add("S", "System");
        macros.add("N", "42");

        final String xml = MacroXMLUtil.toString(macros);
        System.out.println(xml);
        assertThat(xml, equalTo("<N>42</N><S>System</S>"));

        final Macros readback = MacroXMLUtil.readMacros(xml);
        assertThat(readback.getValue("S"), equalTo("System"));
        assertThat(readback.getNames(), hasItems("S", "N"));
    }
}
