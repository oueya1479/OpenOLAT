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
package org.olat.modules.contacttracing.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.olat.core.commons.persistence.SortKey;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableFilter;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.elements.FormLinkImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FilterableFlexiTableModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiSortableColumnDef;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SortableFlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SortableFlexiTableModelDelegate;
import org.olat.core.gui.components.link.Link;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.modules.contacttracing.ContactTracingLocation;

/**
 * Initial date: 12.10.20<br>
 *
 * @author aboeckle, alexander.boeckle@frentix.com, http://www.frentix.com
 */
public class ContactTracingLocationTableModel extends DefaultFlexiTableDataModel<ContactTracingLocation>
implements SortableFlexiTableDataModel<ContactTracingLocation>, FilterableFlexiTableModel {

    public static final String ACTIONS_CMD = "actions_cmd";
    private static final Logger log = Tracing.createLoggerFor(ContactTracingLocationTableModel.class);

    private final Locale locale;

    private Map<ContactTracingLocation, Long> locationRegistrationMap;
    private Map<ContactTracingLocation, FormLink> toolLinks;
    private List<ContactTracingLocation> backup;

    public ContactTracingLocationTableModel(FlexiTableColumnModel columnModel, Map<ContactTracingLocation, Long> locationRegistrationMap, Locale locale) {
        super(columnModel);

        // Save locale for sorting
        this.locale = locale;

        // Set objects
        setObjects(locationRegistrationMap);
    }

    @Override
    public void sort(SortKey sortKey) {
        if(sortKey != null) {
            List<ContactTracingLocation> views = new SortableFlexiTableModelDelegate<>(sortKey, this, locale).sort();
            super.setObjects(views);
        }
    }

    @Override
    public void filter(String searchString, List<FlexiTableFilter> filters) {
        if (StringHelper.containsNonWhitespace(searchString)) {
            search(searchString);
        } else {
            super.setObjects(backup);
        }
    }

    private void search(String searchString) {
        try {
            List<ContactTracingLocation> filteredList;
            String search = searchString.toLowerCase();

            filteredList = backup.stream().filter(location -> {
                if (location.getReference().toLowerCase().contains(search) ||
                        location.getTitle().toLowerCase().contains(search) ||
                        location.getRoom().toLowerCase().contains(search) ||
                        location.getBuilding().toLowerCase().contains(search) ||
                        location.getQrId().toLowerCase().contains(search)) {
                    return true;
                }

                return false;
            }).collect(Collectors.toList());

            super.setObjects(filteredList);
        } catch (Exception e) {
            log.error("", e);
            super.setObjects(backup);
        }
    }

    public void setObjects(Map<ContactTracingLocation, Long> locationRegistrationMap) {
        // Extract locations out of map
        this.backup = new ArrayList<>(locationRegistrationMap.keySet());

        // Save locationRegistrationMap
        this.locationRegistrationMap = locationRegistrationMap;

        // Call setObjects
        setObjects(backup);
    }

    @Override
    public void setObjects(List<ContactTracingLocation> objects) {
        // Set objects
        super.setObjects(objects);
        // Create tool links
        toolLinks = new HashMap<>();
        for (ContactTracingLocation location : objects) {
            FormLink toolsLink = new FormLinkImpl("tools-" + objects.indexOf(location), ACTIONS_CMD, "", Link.NONTRANSLATED);
            toolsLink.setIconLeftCSS("o_icon o_icon_actions o_icon-lg");
            toolsLink.setUserObject(location);

            toolLinks.put(location, toolsLink);
        }
    }

    @Override
    public Object getValueAt(ContactTracingLocation row, int col) {
        switch (ContactTracingLocationCols.values()[col]) {
            case reference:
                return row.getReference();
            case title:
                return row.getTitle();
            case building:
                return row.getBuilding();
            case room:
                return row.getRoom();
            case sector:
                return row.getSector();
            case table:
                return row.getTable();
            case qrId:
                return row.getQrId();
            case qrText:
                return row.getQrText();
            case guest:
                return row.isAccessibleByGuests();
            case registrations:
                return locationRegistrationMap.get(row);
            case settings:
                return toolLinks.get(row);
            default:
                return "ERROR";
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        ContactTracingLocation location = getObject(row);
        return getValueAt(location, col);
    }

    @Override
    public DefaultFlexiTableDataModel<ContactTracingLocation> createCopyWithEmptyList() {
        return null;
    }

    public enum ContactTracingLocationCols implements FlexiSortableColumnDef {
        reference("contact.tracing.cols.reference"),
        title("contact.tracing.cols.title"),
        building("contact.tracing.cols.building"),
        room("contact.tracing.cols.room"),
        sector("contact.tracing.cols.sector"),
        table("contact.tracing.cols.table"),
        qrId("contact.tracing.cols.qr.id"),
        qrText("contact.tracing.cols.qr.text"),
        guest("contact.tracing.cols.guest"),
        registrations("contact.tracing.cols.registrations"),
        settings("contact.tracing.cols.settings", "o_icon o_icon-lg o_icon_actions");

        private final String i18nHeaderKey;
        private final String iconHeader;

        ContactTracingLocationCols(String i18nHeaderKey) {
            this(i18nHeaderKey, null);
        }

        ContactTracingLocationCols(String i18nHeaderKey, String iconHeader) {
            this.i18nHeaderKey = i18nHeaderKey;
            this.iconHeader = iconHeader;
        }

        @Override
        public boolean sortable() {
            return true;
        }

        @Override
        public String sortKey() {
            return name();
        }

        @Override
        public String i18nHeaderKey() {
            return i18nHeaderKey;
        }

        @Override
        public String iconHeader() {
            return iconHeader;
        }
    }
}
