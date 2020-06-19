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
package org.olat.course.nodes.appointments.manager;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.QueryBuilder;
import org.olat.core.util.StringHelper;
import org.olat.course.nodes.appointments.Appointment;
import org.olat.course.nodes.appointments.Appointment.Status;
import org.olat.course.nodes.appointments.AppointmentSearchParams;
import org.olat.course.nodes.appointments.Topic;
import org.olat.course.nodes.appointments.TopicRef;
import org.olat.course.nodes.appointments.model.AppointmentImpl;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;

/**
 * 
 * Initial date: 11 Apr 2020<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
@Component
class AppointmentDAO {
	
	@Autowired
	private DB dbInstance;
	
	Appointment createUnsavedAppointment(Topic topic) {
		AppointmentImpl appointment = new AppointmentImpl();
		appointment.setCreationDate(new Date());
		appointment.setLastModified(appointment.getCreationDate());
		appointment.setTopic(topic);
		appointment.setStatus(Appointment.Status.planned);
		appointment.setStatusModified(appointment.getCreationDate());
		
		return appointment;
	}
	
	Appointment createAppointment(Topic topic) {
		Appointment appointment = createUnsavedAppointment(topic);
		appointment = saveAppointment(appointment);
		return appointment;
	}
	
	Appointment saveAppointment(Appointment appointment) {
		if (appointment instanceof AppointmentImpl) {
			AppointmentImpl impl = (AppointmentImpl)appointment;
			impl.setLastModified(new Date());
		}
		if (appointment.getKey() == null) {
			dbInstance.getCurrentEntityManager().persist(appointment);
		} else {
			dbInstance.getCurrentEntityManager().merge(appointment);
		}
		return appointment;
	}

	Appointment updateStatus(Appointment appointment, Status status) {
		if (appointment instanceof AppointmentImpl) {
			AppointmentImpl impl = (AppointmentImpl)appointment;
			if (!Objects.equal(appointment.getStatus(), status)) {
				impl.setStatusModified(new Date());
			}
			impl.setStatus(status);
		}
		return saveAppointment(appointment);
	}

	void delete(Appointment appointment) {
		QueryBuilder sb = new QueryBuilder();
		sb.append("delete from appointment appointment");
		sb.and().append(" appointment.key = :appointmentKey");
		
		dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString())
				.setParameter("appointmentKey", appointment.getKey())
				.executeUpdate();
	}
	
	void delete(Collection<Long> keys) {
		QueryBuilder sb = new QueryBuilder();
		sb.append("delete from appointment appointment");
		sb.and().append(" appointment.key in (:appointmentKeys)");
		
		dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString())
				.setParameter("appointmentKeys", keys)
				.executeUpdate();	
	}
	
	void delete(TopicRef topic) {
		QueryBuilder sb = new QueryBuilder();
		sb.append("delete from appointment appointment");
		sb.and().append(" appointment.topic.key = :topicKey");
		
		dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString())
				.setParameter("topicKey", topic.getKey())
				.executeUpdate();
	}

	void delete(RepositoryEntry entry, String subIdent) {
		QueryBuilder sb = new QueryBuilder();
		sb.append("delete from appointment appointment");
		sb.append(" where exists (select 1");
		sb.append("                 from appointmenttopic topic");
		sb.append("                where topic.key = appointment.topic.key");
		sb.append("                  and topic.entry.key =  :entryKey");
		if (StringHelper.containsNonWhitespace(subIdent)) {
			sb.append("              and topic.subIdent =  :subIdent");
		}
		sb.append("               )");
		
		Query query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString())
				.setParameter("entryKey", entry.getKey());
		if (StringHelper.containsNonWhitespace(subIdent)) {
			query.setParameter("subIdent", subIdent);
		}
		query.executeUpdate();
	}

	Appointment loadByKey(Long key) {
		QueryBuilder sb = new QueryBuilder();
		sb.append("select appointment");
		sb.append("  from appointment appointment");
		sb.and().append(" appointment.key = :appointmentKey");
		
		List<Appointment> appointments = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Appointment.class)
				.setParameter("appointmentKey", key)
				.getResultList();
		return appointments.isEmpty() ? null : appointments.get(0);
	}
	
	Map<Long, Long> loadTopicKeyToAppointmentCount(AppointmentSearchParams params, boolean freeOnly) {
		QueryBuilder sb = new QueryBuilder();
		sb.append("select appointment.topic.key as topicKey");
		sb.append("     , appointment.key as key");
		sb.append("     , appointment.maxParticipations as maxParticipations");
		sb.append("     , ( select count(*)");
		sb.append("           from appointmentparticipation participation");
		sb.append("          where participation.appointment.key = appointment.key");
		sb.append("       ) as count");
		appendQuery(sb, params);
		
		TypedQuery<Tuple> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Tuple.class);
		addParameters(query, params);
		
		List<Tuple> results = query.getResultList();
		return results.stream()
				.filter(filterFreeOnly(freeOnly))
				.map(t -> (Long)t.get(0))
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}
	
	private Predicate<Tuple> filterFreeOnly(boolean freeOnly) {
		return t -> { 
			if (freeOnly) {
				// no max participants
				if (t.get(2) == null) {
					return true;
				}
				Integer maxParticipants = (Integer)t.get(2);
				Long count = (Long)t.get(3);
				return count.intValue() < maxParticipants.intValue();
			}
			return true; 
		};
	}
	
	Long loadAppointmentCount(AppointmentSearchParams params) {
		QueryBuilder sb = new QueryBuilder();
		sb.append("select count(appointment)");
		appendQuery(sb, params);
		
		TypedQuery<Long> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Long.class);
		addParameters(query, params);
		
		return query.getSingleResult();
	}

	List<Appointment> loadAppointments(AppointmentSearchParams params) {
		QueryBuilder sb = new QueryBuilder();
		sb.append("select appointment");
		appendQuery(sb, params);
		
		TypedQuery<Appointment> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Appointment.class);
		addParameters(query, params);
		
		return query.getResultList();
	}

	private void appendQuery(QueryBuilder sb, AppointmentSearchParams params) {
		sb.append("  from appointment appointment");
		sb.append("      join").append(" fetch", params.isFetchTopic()).append(" appointment.topic topic");
		if (params.getAppointmentKey() != null) {
			sb.and().append("appointment.key = :appointmentKey");
		}
		if (params.getEntry() != null) {
			sb.and().append("topic.entry.key = :entryKey");
		}
		if (StringHelper.containsNonWhitespace(params.getSubIdent())) {
			sb.and().append("topic.subIdent = :subIdent");
		}
		if (params.getTopicKeys() != null && !params.getTopicKeys().isEmpty()) {
			sb.and().append("appointment.topic.key in (:topicKeys)");
		}
		if (params.getStartAfter() != null) {
			sb.and().append("appointment.start >= :startAfter");
		}
		if (params.getStatus() != null) {
			sb.and().append("appointment.status = :status");
		}
	}

	private void addParameters(TypedQuery<?> query, AppointmentSearchParams params) {
		if (params.getAppointmentKey() != null) {
			query.setParameter("appointmentKey", params.getAppointmentKey());
		}
		if (params.getEntry() != null) {
			query.setParameter("entryKey", params.getEntry().getKey());
		}
		if (StringHelper.containsNonWhitespace(params.getSubIdent())) {
			query.setParameter("subIdent", params.getSubIdent());
		}
		if (params.getTopicKeys() != null && !params.getTopicKeys().isEmpty()) {
			query.setParameter("topicKeys", params.getTopicKeys());
		}
		if (params.getStartAfter() != null) {
			query.setParameter("startAfter", params.getStartAfter());
		}
		if (params.getStatus() != null) {
			query.setParameter("status", params.getStatus());
		}
	}

}
