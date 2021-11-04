/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.course.editor.importnodes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableComponent;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;
import org.olat.course.nodes.CourseNodeFactory;

/**
 * 
 * Initial date: 6 oct. 2021<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ConfigurationFileRowUseRenderer implements FlexiCellRenderer {

	@Override
	public void render(Renderer renderer, StringOutput target, Object cellValue, int row, FlexiTableComponent source,
			URLBuilder ubu, Translator translator) {
		if(cellValue instanceof ConfigurationFileRow) {
			ConfigurationFileRow fileRow = (ConfigurationFileRow)cellValue;
			Set<ImportCourseNode> usedByList = new HashSet<>();
			collectRecursively(fileRow, usedByList);
			if(!usedByList.isEmpty()) {
				render(target, usedByList);
			}
		}
	}
	
	private void collectRecursively(ConfigurationFileRow fileRow, Set<ImportCourseNode> usedByList) {
		if(fileRow.getUsedByList() != null && !fileRow.getUsedByList().isEmpty()) {
			usedByList.addAll(fileRow.getUsedByList());
		}
		for(ConfigurationFileRow child:fileRow.getChildren()) {
			collectRecursively(child, usedByList);
		}
	}
	
	
	private void render(StringOutput sb, Collection<ImportCourseNode> nodes) {
		sb.append("<ul class='list-unstyled'>");
		for(ImportCourseNode node:nodes) {
			render(sb, node);
		}
		sb.append("</ul>");
	}
	
	private void render(StringOutput sb, ImportCourseNode node) {
		String cssClass = CourseNodeFactory.getInstance()
				.getCourseNodeConfigurationEvenForDisabledBB(node.getCourseNode().getType())
				.getIconCSSClass();
		sb.append("<li><i class=\"o_icon ").append(cssClass).append("\"> </i> <span>")
		  .append(StringHelper.escapeHtml(node.getCourseNode().getShortTitle())).append("</span></li>");
	}
}