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
package org.olat.core.commons.services.doceditor.office365;

import java.util.Locale;

import org.olat.core.commons.services.doceditor.DocEditor;
import org.olat.core.commons.services.doceditor.DocEditorConfigs;
import org.olat.core.commons.services.doceditor.DocEditorSecurityCallback;
import org.olat.core.commons.services.doceditor.office365.ui.Office365EditorController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.util.Util;
import org.olat.core.util.vfs.VFSLeaf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 26 Apr 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
@Service
public class Office365Editor implements DocEditor {
	
	@Autowired
	private Office365Module office365Module;
	@Autowired
	private Office365Service office365Service;

	@Override
	public boolean isEnable() {
		return office365Module.isEnabled();
	}

	@Override
	public String getType() {
		return "office365";
	}

	@Override
	public String getDisplayName(Locale locale) {
		Translator translator = Util.createPackageTranslator(Office365EditorController.class, locale);
		return translator.translate("editor.display.name");
	}

	@Override
	public boolean isSupportingFormat(String suffix, Mode mode, boolean hasMeta) {
		return hasMeta && office365Service.isSupportingFormat(suffix, mode);
	}

	@Override
	public boolean isLockedForMe(VFSLeaf vfsLeaf, Identity identity, Mode mode) {
		if (office365Service.isLockNeeded(mode)) {
			return office365Service.isLockedForMe(vfsLeaf, identity);
		}
		return false;
	}

	@Override
	public Controller getRunController(UserRequest ureq, WindowControl wControl, Identity identity, VFSLeaf vfsLeaf,
			DocEditorSecurityCallback securityCallback, DocEditorConfigs configs) {
		return new Office365EditorController(ureq, wControl, vfsLeaf, securityCallback);
	}

}
