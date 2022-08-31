package org.processmining.lpmsupportedwords.plugins;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "HTML export", returnLabels = {}, returnTypes = {}, parameterLabels = {
		"HTML", "File" }, userAccessible = true)
@UIExportPlugin(description = "HTML files", extension = "html")
public class HTMLExportPlugin {

	@PluginVariant(variantLabel = "HTML export", requiredParameterLabels = { 0, 1 })
	public void export(PluginContext context, String html, File file) throws IOException {
		try (Writer writer = new FileWriter(file)) {
			writer.write(html);
		}
	}
}
