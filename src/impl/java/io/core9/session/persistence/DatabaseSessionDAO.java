package io.core9.session.persistence;

import io.core9.plugin.database.mongodb.MongoDatabase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * TODO: Heavily depends on MongoDB implementation, remove
 * @author mark
 *
 */
public class DatabaseSessionDAO extends AbstractSessionDAO implements SessionDAO {
	
	private final DBCollection coll;

	@Override
	public void update(Session session) throws UnknownSessionException {
		storeSession(session.getId(), session);
	}

	@Override
	public void delete(Session session) {
		if (session == null) {
            throw new NullPointerException("session argument cannot be null.");
        }
        Serializable id = session.getId();
        if (id != null) {
        	coll.remove(new BasicDBObject("_id", id));
        }
	}

	@Override
	public Collection<Session> getActiveSessions() {
		Collection<Session> result = new ArrayDeque<Session>();
		coll.find().forEach((dbsession) -> {
			try {
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream((byte[]) dbsession.get("session")));
				result.add((SimpleSession) ois.readObject());
				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		});
		return result;
	}

	@Override
	protected Serializable doCreate(Session session) {
		Serializable sessionId = generateSessionId(session);
		assignSessionId(session, sessionId);
        storeSession(sessionId, session);
        return sessionId;
	}

	private void storeSession(Serializable sessionId, Session session) {
		if (sessionId == null) {
            throw new NullPointerException("id argument cannot be null.");
        }
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream stream = new ObjectOutputStream(baos);
			stream.writeObject(session);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		coll.update(new BasicDBObject("_id", sessionId), new BasicDBObject("session", baos.toByteArray()), true, false);
	}

	@Override
	protected Session doReadSession(Serializable sessionId) {
		DBObject dbsess = coll.findOne(new BasicDBObject("_id", sessionId));
		if(dbsess == null) {
			return null;
		} else {
			Session session = null;
			try {
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream((byte[]) dbsess.get("session")));
				session = (SimpleSession) ois.readObject();
				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return session;
		}
	}
	
	public DatabaseSessionDAO(MongoDatabase db) {
		this.coll = db.getCollection(db.getMasterDBName(), "core.sessions");
	}

}
