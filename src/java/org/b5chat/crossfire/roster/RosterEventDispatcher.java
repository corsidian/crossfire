/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2005-2008 B5Chat Community. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.b5chat.crossfire.roster;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Dispatches roster events. The following events are supported:
 * <ul>
 * <li><b>rosterLoaded</b> --> A roster has just been loaded.</li>
 * <li><b>contactAdded</b> --> A contact has been added to a roster.</li>
 * <li><b>contactUpdated</b> --> A contact has been updated of a roster.</li>
 * <li><b>contactDeleted</b> --> A contact has been deleted from a roster.</li>
 * </ul>
 * Use {@link #addListener(IRosterEventListener)} and {@link #removeListener(IRosterEventListener)}
 * to add or remove {@link IRosterEventListener}.
 *
 * @author Gaston Dombiak
 */
public class RosterEventDispatcher {

    private List<IRosterEventListener> listeners =
            new CopyOnWriteArrayList<IRosterEventListener>();

    /**
     * Registers a listener to receive events.
     *
     * @param listener the listener.
     */
    public void addListener(IRosterEventListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        listeners.add(listener);
    }

    /**
     * Unregisters a listener to receive events.
     *
     * @param listener the listener.
     */
    public void removeListener(IRosterEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies the listeners that a roster has just been loaded.
     *
     * @param rosterImpl the loaded roster.
     */
    public void rosterLoaded(IRoster rosterImpl) {
        if (!listeners.isEmpty()) {
            for (IRosterEventListener listener : listeners) {
                listener.rosterLoaded(rosterImpl);
            }
        }
    }

    /**
     * Notifies listeners that a contact is about to be added to a roster. New contacts
     * may be persisted to the database or not. Listeners may indicate that contact about
     * to be persisted should not be persisted. Only one listener is needed to return
     * <tt>false</tt> so that the contact is not persisted.
     *
     * @param rosterImpl the roster that was updated.
     * @param item the new roster item.
     * @param persistent true if the new contact is going to be saved to the database.
     * @return false if the contact should not be persisted to the database.
     */
    public boolean addingContact(IRoster rosterImpl, IRosterItem item, boolean persistent) {
        boolean answer = persistent;
        if (!listeners.isEmpty()) {
            for (IRosterEventListener listener : listeners) {
                if (!listener.addingContact(rosterImpl, item, persistent)) {
                    answer = false;
                }
            }
        }
        return answer;
    }

    /**
     * Notifies the listeners that a contact has been added to a roster.
     *
     * @param roster the roster that was updated.
     * @param item   the new roster item.
     */
    public void contactAdded(Roster roster, IRosterItem item) {
        if (!listeners.isEmpty()) {
            for (IRosterEventListener listener : listeners) {
                listener.contactAdded(roster, item);
            }
        }
    }

    /**
     * Notifies the listeners that a contact has been updated.
     *
     * @param roster the roster that was updated.
     * @param item   the updated roster item.
     */
    public void contactUpdated(Roster roster, IRosterItem item) {
        if (!listeners.isEmpty()) {
            for (IRosterEventListener listener : listeners) {
                listener.contactUpdated(roster, item);
            }
        }
    }

    /**
     * Notifies the listeners that a contact has been deleted from a roster.
     *
     * @param roster the roster that was updated.
     * @param item   the roster item that was deleted.
     */
    public void contactDeleted(Roster roster, IRosterItem item) {
        if (!listeners.isEmpty()) {
            for (IRosterEventListener listener : listeners) {
                listener.contactDeleted(roster, item);
            }
        }
    }
}
