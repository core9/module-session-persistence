package io.core9.session.persistence;

import io.core9.plugin.database.repository.Collection;
import io.core9.plugin.database.repository.CrudEntity;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.shiro.session.ExpiredSessionException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.StoppedSessionException;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.ValidatingSession;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Collection("core.sessions")
public class SessionEntity implements CrudEntity, ValidatingSession, Serializable {

	private static final long serialVersionUID = -7856484273493287783L;
	
    protected static final long MILLIS_PER_SECOND = 1000;
    protected static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    protected static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

	
	private Serializable id;
    private Date startTimestamp;
    private Date stopTimestamp;
    private Date lastAccessTime;
    private long timeout;
    private boolean expired;
    private String host;
    private Map<Object, Object> attributes;

	public void set_id(String id) {
		this.id = id;
	}
	
	public String get_id() {
		return (String) id;
	}

	@Override
	public String getId() {
		return (String) id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public Map<String,Object> retrieveDefaultQuery() {
		return null;
	}

	@Override
	public Date getStartTimestamp() {
        return startTimestamp;
	}
	
	public void setStartTimestamp(Date startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
	
	public Date getStopTimestamp() {
        return stopTimestamp;
    }

    public void setStopTimestamp(Date stopTimestamp) {
        this.stopTimestamp = stopTimestamp;
    }

	@Override
    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

	@Override
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

	@Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
    
    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

	@Override
	public void touch() throws InvalidSessionException {
        this.lastAccessTime = new Date();
	}

	@Override
	public void stop() throws InvalidSessionException {
        if (this.stopTimestamp == null) {
            this.stopTimestamp = new Date();
        }
	}

	@JsonIgnore
	@Override
	public java.util.Collection<Object> getAttributeKeys() throws InvalidSessionException {
        Map<Object, Object> attributes = getAttributes();
        if (attributes == null) {
            return Collections.emptySet();
        }
        return attributes.keySet();
	}

	@JsonIgnore
	@Override
	public Object getAttribute(Object key) throws InvalidSessionException {
		Map<Object, Object> attributes = getAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.get(key);
	}

	@JsonIgnore
	@Override
	public void setAttribute(Object key, Object value) throws InvalidSessionException {
		if (value == null) {
			 removeAttribute(key);
		} else {
			getAttributesLazy().put(key, value);
		}		
	}
	
	@JsonIgnore
    private Map<Object, Object> getAttributesLazy() {
        Map<Object, Object> attributes = getAttributes();
        if (attributes == null) {
            attributes = new HashMap<Object, Object>();
            setAttributes(attributes);
        }
        return attributes;
    }
	
    public Map<Object, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<Object, Object> attributes) {
        this.attributes = attributes;
    }


	@Override
	public Object removeAttribute(Object key) throws InvalidSessionException {
        Map<Object, Object> attributes = getAttributes();
        if (attributes == null) {
            return null;
        } else {
            return attributes.remove(key);
        }
	}

	@JsonIgnore
	@Override
	public boolean isValid() {
        return !isStopped() && !isExpired();
	}

	@JsonIgnore
    protected boolean isStopped() {
        return getStopTimestamp() != null;
    }

	@Override
	public void validate() throws InvalidSessionException {
        //check for stopped:
        if (isStopped()) {
            //timestamp is set, so the session is considered stopped:
            String msg = "Session with id [" + getId() + "] has been " +
                    "explicitly stopped.  No further interaction under this session is " +
                    "allowed.";
            throw new StoppedSessionException(msg);
        }

        //check for expiration
        if (isTimedOut()) {
            expire();

            //throw an exception explaining details of why it expired:
            Date lastAccessTime = getLastAccessTime();
            long timeout = getTimeout();

            Serializable sessionId = getId();

            DateFormat df = DateFormat.getInstance();
            String msg = "Session with id [" + sessionId + "] has expired. " +
                    "Last access time: " + df.format(lastAccessTime) +
                    ".  Current time: " + df.format(new Date()) +
                    ".  Session timeout is set to " + timeout / MILLIS_PER_SECOND + " seconds (" +
                    timeout / MILLIS_PER_MINUTE + " minutes)";
            System.out.println(msg);
            throw new ExpiredSessionException(msg);
        }		
	}
	
	@JsonIgnore
    protected boolean isTimedOut() {

        if (isExpired()) {
            return true;
        }

        long timeout = getTimeout();

        if (timeout >= 0l) {

            Date lastAccessTime = getLastAccessTime();

            if (lastAccessTime == null) {
                String msg = "session.lastAccessTime for session with id [" +
                        getId() + "] is null.  This value must be set at " +
                        "least once, preferably at least upon instantiation.  Please check the " +
                        getClass().getName() + " implementation and ensure " +
                        "this value will be set (perhaps in the constructor?)";
                throw new IllegalStateException(msg);
            }

            // Calculate at what time a session would have been last accessed
            // for it to be expired at this point.  In other words, subtract
            // from the current time the amount of time that a session can
            // be inactive before expiring.  If the session was last accessed
            // before this time, it is expired.
            long expireTimeMillis = System.currentTimeMillis() - timeout;
            Date expireTime = new Date(expireTimeMillis);
            return lastAccessTime.before(expireTime);
        } else {
        	System.out.println("No timeout for session with id [" + getId() +
                        "].  Session is not considered expired.");
        }
        return false;
    }
    
    protected void expire() {
        stop();
        this.expired = true;
    }

    public SessionEntity() {
    	this.timeout = DefaultSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT; //TODO - remove concrete reference to DefaultSessionManager
    	this.startTimestamp = new Date();
    	this.lastAccessTime = this.startTimestamp;
    }

}
