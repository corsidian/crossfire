package org.b5chat.crossfire;

import org.xmpp.packet.JID;

public interface JIDFactory {
	public JID createJID(String username, String resource);
	public JID createJID(String username, String resource, boolean skipStringprep);
	public boolean isRemote(JID jid);
	public boolean isLocal(JID jid);
}
