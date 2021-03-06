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

package org.b5chat.crossfire.core.lockout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatches lockout events. The following events are supported:
 * <ul>
 * <li><b>accountLocked</b> --> An account has been disabled/locked out.</li>
 * <li><b>accountUnlocked</b> --> An account has been enabled/unlocked.</li>
 * <li><b>lockedAccountDenied</b> --> A locked out account has been denied login.</li>
 * </ul>
 * Use {@link #addListener(ILockOutEventListener)} and {@link #removeListener(ILockOutEventListener)}
 * to add or remove {@link ILockOutEventListener}.
 *
 * @author Daniel Henninger
 */
public class LockOutEventDispatcher {

    private static List<ILockOutEventListener> listeners =
            new CopyOnWriteArrayList<ILockOutEventListener>();

    /**
     * Registers a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void addListener(ILockOutEventListener listener) {
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
    public static void removeListener(ILockOutEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies the listeners that an account was just set to be disabled/locked out.
     *
     * @param flag The LockOutFlag that was set, which includes the username of the account and start/end times.
     */
    public static void accountLocked(LockOutFlag flag) {
        if (!listeners.isEmpty()) {
            for (ILockOutEventListener listener : listeners) {
                listener.accountLocked(flag);
            }
        }
    }

    /**
     * Notifies the listeners that an account was just enabled (lockout removed).
     *
     * @param username The username of the account that was enabled.
     */
    public static void accountUnlocked(String username) {
        if (!listeners.isEmpty()) {
            for (ILockOutEventListener listener : listeners) {
                listener.accountUnlocked(username);
            }
        }
    }

    /**
     * Notifies the listeners that a locked out account attempted to log in.
     *
     * @param username The username of the account that tried to log in.
     */
    public static void lockedAccountDenied(String username) {
        if (!listeners.isEmpty()) {
            for (ILockOutEventListener listener : listeners) {
                listener.lockedAccountDenied(username);
            }
        }
    }

}
